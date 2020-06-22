package org.infinispan.server.core.backup;

import static org.infinispan.server.core.backup.BackupUtil.CACHES_CONFIG_PROPERTY;
import static org.infinispan.server.core.backup.BackupUtil.CACHES_DIR;
import static org.infinispan.server.core.backup.BackupUtil.CACHES_PROPERTY;
import static org.infinispan.server.core.backup.BackupUtil.CACHE_CONFIG_DIR;
import static org.infinispan.server.core.backup.BackupUtil.CONTAINERS_PROPERTIES_FILE;
import static org.infinispan.server.core.backup.BackupUtil.CONTAINERS_PROPERTY;
import static org.infinispan.server.core.backup.BackupUtil.CONTAINER_DIR;
import static org.infinispan.server.core.backup.BackupUtil.COUNTERS_DIR;
import static org.infinispan.server.core.backup.BackupUtil.COUNTERS_FILE;
import static org.infinispan.server.core.backup.BackupUtil.COUNTERS_PROPERTY;
import static org.infinispan.server.core.backup.BackupUtil.GLOBAL_CONFIG_FILE;
import static org.infinispan.server.core.backup.BackupUtil.MANIFEST_PROPERTIES_FILE;
import static org.infinispan.server.core.backup.BackupUtil.PROTO_CACHE_NAME;
import static org.infinispan.server.core.backup.BackupUtil.PROTO_SCHEMA_DIR;
import static org.infinispan.server.core.backup.BackupUtil.PROTO_SCHEMA_PROPERTY;
import static org.infinispan.server.core.backup.BackupUtil.SCRIPT_CACHE_NAME;
import static org.infinispan.server.core.backup.BackupUtil.SCRIPT_DIR;
import static org.infinispan.server.core.backup.BackupUtil.SCRIPT_PROPERTY;
import static org.infinispan.server.core.backup.BackupUtil.VERSION_PROPERTY;
import static org.infinispan.server.core.backup.BackupUtil.cacheDataFile;
import static org.infinispan.server.core.backup.BackupUtil.writeMessageStream;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.stream.XMLStreamException;

import org.infinispan.AdvancedCache;
import org.infinispan.commons.CacheException;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.commons.marshall.MarshallingException;
import org.infinispan.commons.util.Version;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.parsing.ParserRegistry;
import org.infinispan.container.entries.CacheEntry;
import org.infinispan.counter.api.CounterConfiguration;
import org.infinispan.counter.api.CounterManager;
import org.infinispan.counter.api.CounterType;
import org.infinispan.encoding.impl.StorageConfigurationManager;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.marshall.persistence.PersistenceMarshaller;
import org.infinispan.marshall.protostream.impl.SerializationContextRegistry;
import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.reactive.publisher.PublisherTransformers;
import org.infinispan.reactive.publisher.impl.ClusterPublisherManager;
import org.infinispan.reactive.publisher.impl.DeliveryGuarantee;
import org.infinispan.registry.InternalCacheRegistry;
import org.infinispan.util.concurrent.BlockingManager;
import org.infinispan.util.concurrent.CompletionStages;
import org.reactivestreams.Publisher;

import io.reactivex.rxjava3.core.Flowable;

/**
 * Responsible for creating backup files that can be used to restore a container/cache on a new cluster.
 *
 * @author Ryan Emerson
 * @since 11.0
 */
class BackupWriter {
   // TODO what size?
   private static final int BUFFER_SIZE = 100;

   private final BlockingManager blockingManager;
   private final Map<String, DefaultCacheManager> cacheManagers;

   private final Path rootDir;
   private final ParserRegistry parserRegistry;

   BackupWriter(BlockingManager blockingManager, Map<String, DefaultCacheManager> cacheManagers, Path rootDir) {
      this.blockingManager = blockingManager;
      this.cacheManagers = cacheManagers;
      this.rootDir = rootDir;
      this.parserRegistry = new ParserRegistry();
   }

   CompletionStage<Path> create() {
      List<CompletionStage<?>> stages = cacheManagers.entrySet().stream()
            .map(e -> createBackup(e.getKey(), e.getValue(), null))
            .collect(Collectors.toList());

      stages.add(writeManifest(cacheManagers.keySet()));
      return blockingManager.thenApplyBlocking(CompletionStages.allOf(stages), Void -> createZip(), "create");
   }

   private CompletionStage<Void> writeManifest(Set<String> containers) {
      return blockingManager.runBlocking(() -> {
         Properties manifest = new Properties();
         manifest.put(CONTAINERS_PROPERTY, String.join(",", containers));
         manifest.put(VERSION_PROPERTY, Version.getVersion());
         storeProperties(manifest, "Backup Manifest", rootDir.resolve(MANIFEST_PROPERTIES_FILE));
      }, "write-manifest");
   }

   /**
    * Create a backup of the specified container.
    *
    * @param containerName the name of container to backup.
    * @param cm            the container to backup.
    * @param cacheList     the name of the caches to include in the backup, or null if all container content should be
    *                      included.
    * @return a {@link CompletionStage} that completes once the backup has finished.
    */
   private CompletionStage<Void> createBackup(String containerName, EmbeddedCacheManager cm, Set<String> cacheList) {
      Path containerRoot = rootDir.resolve(CONTAINER_DIR).resolve(containerName);
      containerRoot.toFile().mkdirs();
      GlobalComponentRegistry gcr = cm.getGlobalComponentRegistry();
      BlockingManager blockingManager = gcr.getComponent(BlockingManager.class);
      InternalCacheRegistry internalCacheRegistry = gcr.getComponent(InternalCacheRegistry.class);
      boolean containerBackup = cacheList == null;

      // Templates and cache configurations are the same except isTemplate == true
      // Parser simply resolves configuration="example" config and uses that as the basis for the cache.
      // When backing up individual caches, it should be ok just to write it's configuration,
      // however, the base templates that it was originally created from will not be included in the backup
      Set<String> cacheNames = ConcurrentHashMap.newKeySet();
      Set<String> configNames = ConcurrentHashMap.newKeySet();
      Set<String> counterNames = ConcurrentHashMap.newKeySet();
      Set<String> protoFiles = ConcurrentHashMap.newKeySet();
      Set<String> scriptFiles = ConcurrentHashMap.newKeySet();
      Path configRoot = containerRoot.resolve(CACHE_CONFIG_DIR);
      configRoot.toFile().mkdir();
      List<CompletionStage<?>> stages = cm.getCacheConfigurationNames().stream()
            .filter(cache -> cacheList == null || cacheList.contains(cache))
            .filter(cache -> !internalCacheRegistry.isInternalCache(cache))
            .map(name -> blockingManager.runBlocking(() -> {
               Configuration config = cm.getCacheConfiguration(name);
               if (config.isTemplate()) {
                  configNames.add(name);
                  writeCacheConfig(name, config, configRoot);
               } else {
                  cacheNames.add(name);
                  createCacheBackup(name, cm, containerRoot);
               }
            }, "backup-caches"))
            .collect(Collectors.toList());

      stages.add(
            // Write the global configuration xml
            blockingManager.runBlocking(() ->
                  writeGlobalConfig(cm.getCacheManagerConfiguration(), containerRoot), "global-config")
      );

      if (containerBackup) {
         stages.add(
               // Write the counters.dat file
               blockingManager.runBlocking(() -> writeCounters(counterNames, cm, containerRoot), "write-counters")
         );

         stages.add(
               blockingManager.runBlocking(() -> writeProtoFiles(protoFiles, cm, containerRoot), "write-proto")
         );

         stages.add(
               blockingManager.runBlocking(() -> writeScriptFiles(scriptFiles, cm, containerRoot), "write-scripts")
         );
      }

      return blockingManager
            .thenRun(CompletionStages.allOf(stages),
                  () -> {
                     Properties manifest = new Properties();
                     manifest.put(CACHES_CONFIG_PROPERTY, String.join(",", configNames));
                     manifest.put(CACHES_PROPERTY, String.join(",", cacheNames));
                     manifest.put(COUNTERS_PROPERTY, String.join(",", counterNames));
                     manifest.put(PROTO_SCHEMA_PROPERTY, String.join(",", protoFiles));
                     manifest.put(SCRIPT_PROPERTY, String.join(",", scriptFiles));
                     storeProperties(manifest, "Container Properties", containerRoot.resolve(CONTAINERS_PROPERTIES_FILE));
                  },
                  "create-manifest");
   }

   private void writeGlobalConfig(GlobalConfiguration configuration, Path root) {
      Path xmlPath = root.resolve(GLOBAL_CONFIG_FILE);
      try (OutputStream os = Files.newOutputStream(xmlPath)) {
         parserRegistry.serialize(os, configuration, Collections.emptyMap());
      } catch (XMLStreamException | IOException e) {
         throw new CacheException(String.format("Unable to create global configuration file '%s'", xmlPath), e);
      }
   }

   private void writeProtoFiles(Set<String> schemaNames, EmbeddedCacheManager cm, Path containerRoot) {
      writeInternalCacheAsFiles(schemaNames, cm.getCache(PROTO_CACHE_NAME), containerRoot.resolve(PROTO_SCHEMA_DIR));
   }

   private void writeScriptFiles(Set<String> scriptNames, EmbeddedCacheManager cm, Path containerRoot) {
      writeInternalCacheAsFiles(scriptNames, cm.getCache(SCRIPT_CACHE_NAME), containerRoot.resolve(SCRIPT_DIR));
   }

   private void writeInternalCacheAsFiles(Set<String> fileNames, Map<String, String> cache, Path root) {
      root.toFile().mkdir();
      for (Map.Entry<String, String> entry : cache.entrySet()) {
         String fileName = entry.getKey();
         fileNames.add(fileName);
         Path file = root.resolve(fileName);
         try {
            Files.write(file, entry.getValue().getBytes(StandardCharsets.UTF_8));
         } catch (IOException e) {
            throw new CacheException(String.format("Unable to create %s", file), e);
         }
      }
   }

   private void writeCounters(Set<String> counterNames, EmbeddedCacheManager cm, Path containerRoot) {
      GlobalComponentRegistry gcr = cm.getGlobalComponentRegistry();
      CounterManager counterManager = gcr.getComponent(CounterManager.class);
      if (counterManager == null)
         return;

      Collection<String> counters = counterManager.getCounterNames();
      if (counters.isEmpty())
         return;

      SerializationContextRegistry ctxRegistry = gcr.getComponent(SerializationContextRegistry.class);
      ImmutableSerializationContext serCtx = ctxRegistry.getPersistenceCtx();

      Path countersRoot = containerRoot.resolve(COUNTERS_DIR);
      countersRoot.toFile().mkdir();
      Flowable.using(
            () -> Files.newOutputStream(countersRoot.resolve(COUNTERS_FILE)),
            output ->
                  Flowable.fromIterable(counters)
                        .map(counter -> {
                           counterNames.add(counter);
                           CounterConfiguration config = counterManager.getConfiguration(counter);
                           CounterBackupEntry e = new CounterBackupEntry();
                           e.name = counter;
                           e.configuration = config;
                           e.value = config.type() == CounterType.WEAK ?
                                 counterManager.getWeakCounter(counter).getValue() :
                                 CompletionStages.join(counterManager.getStrongCounter(counter).getValue());
                           return e;
                        })
                        .doOnNext(e -> writeMessageStream(e, serCtx, output))
                        .doOnError(t -> {
                           throw new CacheException("Unable to create counter backup", t);
                        }),
            OutputStream::close
      ).subscribe();
   }

   private void createCacheBackup(String cacheName, EmbeddedCacheManager cm, Path containerRoot) {
      AdvancedCache<?, ?> cache = cm.getCache(cacheName).getAdvancedCache();
      ComponentRegistry cr = cache.getComponentRegistry();

      // Create the cache backup dir and parents
      Path cacheRoot = containerRoot.resolve(CACHES_DIR).resolve(cacheName);
      cacheRoot.toFile().mkdirs();

      // Write configuration file
      writeCacheConfig(cacheName, cr.getConfiguration(), cacheRoot);

      // Write in-memory cache contents to .dat file if the cache is not empty
      if (!cache.isEmpty())
         writeCacheDataFile(cacheName, cr, cacheRoot);
   }

   private void writeCacheConfig(String cacheName, Configuration configuration, Path root) {
      String fileName = BackupUtil.cacheConfigFile(cacheName);
      Path xmlPath = root.resolve(BackupUtil.cacheConfigFile(cacheName));
      try (OutputStream os = Files.newOutputStream(xmlPath)) {
         parserRegistry.serialize(os, cacheName, configuration);
      } catch (XMLStreamException | IOException e) {
         throw new CacheException(String.format("Unable to create backup file '%s'", fileName), e);
      }
   }

   private void writeCacheDataFile(String cacheName, ComponentRegistry cr, Path cacheRoot) {
      ClusterPublisherManager<?, ?> clusterPublisherManager = cr.getClusterPublisherManager().running();
      SerializationContextRegistry ctxRegistry = cr.getGlobalComponentRegistry().getComponent(SerializationContextRegistry.class);
      ImmutableSerializationContext serCtx = ctxRegistry.getPersistenceCtx();

      Path datFile = cacheRoot.resolve(cacheDataFile(cacheName));

      Publisher<CacheEntry<?, ?>> p = s -> clusterPublisherManager.entryPublisher(null, null, null, false,
            DeliveryGuarantee.EXACTLY_ONCE, BUFFER_SIZE, PublisherTransformers.identity())
            .subscribe(s);

      StorageConfigurationManager scm = cr.getComponent(StorageConfigurationManager.class);
      boolean keyMarshalling = MediaType.APPLICATION_OBJECT.equals(scm.getKeyStorageMediaType());
      boolean valueMarshalling = MediaType.APPLICATION_OBJECT.equals(scm.getValueStorageMediaType());
      PersistenceMarshaller persistenceMarshaller = cr.getPersistenceMarshaller();
      Marshaller userMarshaller = persistenceMarshaller.getUserMarshaller();
      Flowable.using(
            () -> Files.newOutputStream(datFile),
            output ->
                  Flowable.fromPublisher(p)
                        .buffer(BUFFER_SIZE)
                        .flatMap(Flowable::fromIterable)
                        .map(e -> {
                           CacheBackupEntry be = new CacheBackupEntry();
                           be.key = keyMarshalling ? marshall(e.getKey(), userMarshaller) : (byte[]) scm.getKeyWrapper().unwrap(e.getKey());
                           be.value = valueMarshalling ? marshall(e.getValue(), userMarshaller) : (byte[]) scm.getValueWrapper().unwrap(e.getKey());
                           be.metadata = marshall(e.getMetadata(), persistenceMarshaller);
                           be.internalMetadata = e.getInternalMetadata();
                           be.created = e.getCreated();
                           be.lastUsed = e.getLastUsed();
                           return be;
                        })
                        .doOnNext(e -> writeMessageStream(e, serCtx, output))
                        .doOnError(t -> {
                           throw new CacheException("Unable to create cache backup", t);
                        }),
            OutputStream::close
      ).subscribe();
   }

   private Path createZip() {
      LocalDateTime now = LocalDateTime.now();
      // Name the backup in the format 'infinispan-[year][month][day][hour][minute].zip'
      String backupName = String.format("%s-%d%02d%02d%02d%02d.zip", Version.getBrandName(), now.getYear(), now.getMonthValue(), now.getDayOfMonth(), now.getHour(), now.getMinute());
      Path zipFile = rootDir.resolve(backupName);
      try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(Files.createFile(zipFile)))) {
         Files.walkFileTree(rootDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
               if (!path.equals(zipFile)) {
                  String name = rootDir.relativize(path).toString();
                  zs.putNextEntry(new ZipEntry(name));
                  Files.copy(path, zs);
                  zs.closeEntry();
                  Files.delete(path);
               }
               return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
               if (exc != null)
                  throw new IllegalStateException(exc);

               if (!dir.equals(rootDir))
                  Files.delete(dir);
               return FileVisitResult.CONTINUE;
            }
         });
      } catch (IOException e) {
         throw new CacheException(e);
      }
      return zipFile;
   }

   private void storeProperties(Properties properties, String description, Path dest) {
      try (OutputStream os = Files.newOutputStream(dest)) {
         properties.store(os, description);
      } catch (IOException e) {
         throw new CacheException(String.format("Unable to create %s file", description), e);
      }
   }

   private static byte[] marshall(Object key, Marshaller marshaller) {
      try {
         return marshaller.objectToByteBuffer(key);
      } catch (IOException | InterruptedException e) {
         throw new MarshallingException(e);
      }
   }
}
