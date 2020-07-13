package org.infinispan.server.core.backup;

import static org.infinispan.server.core.BackupManager.Resource.CACHES;
import static org.infinispan.server.core.BackupManager.Resource.CACHE_CONFIGURATIONS;
import static org.infinispan.server.core.BackupManager.Resource.COUNTERS;
import static org.infinispan.server.core.BackupManager.Resource.PROTO_SCHEMAS;
import static org.infinispan.server.core.BackupManager.Resource.SCRIPTS;
import static org.infinispan.server.core.backup.BackupUtil.CONTAINERS_PROPERTIES_FILE;
import static org.infinispan.server.core.backup.BackupUtil.CONTAINER_KEY;
import static org.infinispan.server.core.backup.BackupUtil.COUNTERS_FILE;
import static org.infinispan.server.core.backup.BackupUtil.GLOBAL_CONFIG_FILE;
import static org.infinispan.server.core.backup.BackupUtil.MANIFEST_PROPERTIES_FILE;
import static org.infinispan.server.core.backup.BackupUtil.PROTO_CACHE_NAME;
import static org.infinispan.server.core.backup.BackupUtil.SCRIPT_CACHE_NAME;
import static org.infinispan.server.core.backup.BackupUtil.VERSION_KEY;
import static org.infinispan.server.core.backup.BackupUtil.cacheDataFile;
import static org.infinispan.server.core.backup.BackupUtil.resolve;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletionStage;
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
import org.infinispan.server.core.BackupManager;
import org.infinispan.server.core.logging.Log;
import org.infinispan.util.concurrent.BlockingManager;
import org.infinispan.util.concurrent.CompletionStages;
import org.infinispan.util.logging.LogFactory;
import org.reactivestreams.Publisher;

import io.reactivex.rxjava3.core.Flowable;

/**
 * Responsible for creating backup files that can be used to restore a container/cache on a new cluster.
 *
 * @author Ryan Emerson
 * @since 11.0
 */
class BackupWriter {

   private static final Log log = LogFactory.getLog(BackupWriter.class, Log.class);

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

   CompletionStage<Path> create(Map<String, BackupManager.BackupParameters> params) {
      List<CompletionStage<?>> stages = new ArrayList<>(params.size() + 1);
      for (Map.Entry<String, BackupManager.BackupParameters> e : params.entrySet()) {
         String container = e.getKey();
         EmbeddedCacheManager cm = cacheManagers.get(container);
         stages.add(createBackup(container, cm, e.getValue()));
      }

      stages.add(writeManifest(cacheManagers.keySet()));
      return blockingManager.thenApplyBlocking(CompletionStages.allOf(stages), Void -> createZip(), "create");
   }

   private CompletionStage<Void> writeManifest(Set<String> containers) {
      return blockingManager.runBlocking(() -> {
         Properties manifest = new Properties();
         manifest.put(CONTAINER_KEY, String.join(",", containers));
         manifest.put(VERSION_KEY, Version.getVersion());
         storeProperties(manifest, "Backup Manifest", rootDir.resolve(MANIFEST_PROPERTIES_FILE));
      }, "write-manifest");
   }

   /**
    * Create a backup of the specified container.
    *
    * @param containerName the name of container to backup.
    * @param cm            the container to backup.
    * @param params        the {@link BackupManager.BackupParameters} object that determines what resources are included
    *                      in the backup for this container.
    * @return a {@link CompletionStage} that completes once the backup has finished.
    */
   private CompletionStage<Void> createBackup(String containerName, EmbeddedCacheManager cm, BackupManager.BackupParameters params) {
      Path containerRoot = rootDir.resolve(CONTAINER_KEY).resolve(containerName);
      containerRoot.toFile().mkdirs();
      GlobalComponentRegistry gcr = cm.getGlobalComponentRegistry();
      BlockingManager blockingManager = gcr.getComponent(BlockingManager.class);
      InternalCacheRegistry icr = gcr.getComponent(InternalCacheRegistry.class);

      List<CompletionStage<?>> stages = new ArrayList<>();

      stages.add(
            // Write the global configuration xml
            blockingManager.runBlocking(() ->
                  writeGlobalConfig(cm.getCacheManagerConfiguration(), containerRoot), "global-config")
      );

      if (params.include(CACHES)) {
         Set<String> cacheNames = params.computeIfEmpty(
               CACHES,
               () -> cm.getCacheConfigurationNames().stream()
                     .filter(name -> !cm.getCacheConfiguration(name).isTemplate())
                     .filter(name -> !icr.isInternalCache(name))
                     .collect(Collectors.toSet())
         );

         for (String cache : cacheNames) {
            stages.add(
                  blockingManager.runBlocking(() -> createCacheBackup(cache, cm, containerRoot), "backup-cache-" + cache)
            );
         }
      }

      if (params.include(CACHE_CONFIGURATIONS)) {
         Set<String> configNames = params.computeIfEmpty(
               CACHE_CONFIGURATIONS,
               () -> cm.getCacheConfigurationNames().stream()
                     .filter(name -> cm.getCacheConfiguration(name).isTemplate())
                     .collect(Collectors.toSet())
         );

         Path configRoot = resolve(containerRoot, CACHE_CONFIGURATIONS);
         configRoot.toFile().mkdir();
         for (String configName : configNames) {
            stages.add(
                  blockingManager.runBlocking(() -> {
                     Configuration config = cm.getCacheConfiguration(configName);
                     writeCacheConfig(configName, config, configRoot);
                  }, "write-config-" + configName)
            );
         }
      }

      if (params.include(COUNTERS)) {
         CounterManager counterManager = gcr.getComponent(CounterManager.class);
         if (counterManager != null) {
            stages.add(
                  blockingManager.runBlocking(() -> {
                     Set<String> counterNames = params.computeIfEmpty(COUNTERS, () -> new HashSet<>(counterManager.getCounterNames()));
                     writeCounters(counterNames, cm, containerRoot);
                  }, "write-counters")
            );
         } else {
            throw log.missingBackupResourceModule(COUNTERS);
         }
      }

      if (params.include(PROTO_SCHEMAS)) {
         if (cm.getCacheConfiguration(PROTO_CACHE_NAME) != null) {
            stages.add(
                  blockingManager.runBlocking(() -> {
                     Set<String> schemaNames = params.computeIfEmpty(PROTO_SCHEMAS, () -> cm.<String, String>getCache(PROTO_CACHE_NAME).keySet());
                     writeInternalCacheAsFiles(schemaNames, cm.getCache(PROTO_CACHE_NAME), resolve(containerRoot, PROTO_SCHEMAS));
                  }, "write-proto-schema")
            );
         } else {
            throw log.missingBackupResourceModule(PROTO_SCHEMAS);
         }
      }

      if (params.include(SCRIPTS)) {
         if (cm.getCacheConfiguration(SCRIPT_CACHE_NAME) != null) {
            stages.add(
                  blockingManager.runBlocking(() -> {
                     Set<String> scriptNames = params.computeIfEmpty(SCRIPTS, () -> cm.<String, String>getCache(SCRIPT_CACHE_NAME).keySet());
                     writeInternalCacheAsFiles(scriptNames, cm.getCache(SCRIPT_CACHE_NAME), resolve(containerRoot, SCRIPTS));
                  }, "write-scripts")
            );
         } else {
            throw log.missingBackupResourceModule(SCRIPTS);
         }
      }

      return blockingManager
            .thenRun(CompletionStages.allOf(stages),
                  () -> {
                     Properties manifest = new Properties();
                     addResourceProperty(params, CACHE_CONFIGURATIONS, manifest);
                     addResourceProperty(params, CACHES, manifest);
                     addResourceProperty(params, COUNTERS, manifest);
                     addResourceProperty(params, PROTO_SCHEMAS, manifest);
                     addResourceProperty(params, SCRIPTS, manifest);
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

   private void writeInternalCacheAsFiles(Set<String> allowList, Map<String, String> cache, Path root) {
      root.toFile().mkdir();
      for (Map.Entry<String, String> entry : cache.entrySet()) {
         String fileName = entry.getKey();
         if (allowList.contains(fileName)) {
            Path file = root.resolve(fileName);
            try {
               Files.write(file, entry.getValue().getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
               throw new CacheException(String.format("Unable to create %s", file), e);
            }
         }
      }
   }

   private void writeCounters(Set<String> counterNames, EmbeddedCacheManager cm, Path containerRoot) {
      GlobalComponentRegistry gcr = cm.getGlobalComponentRegistry();
      CounterManager counterManager = gcr.getComponent(CounterManager.class);
      if (counterManager == null)
         return;

      SerializationContextRegistry ctxRegistry = gcr.getComponent(SerializationContextRegistry.class);
      ImmutableSerializationContext serCtx = ctxRegistry.getPersistenceCtx();

      Path countersRoot = resolve(containerRoot, COUNTERS);
      countersRoot.toFile().mkdir();
      Flowable.using(
            () -> Files.newOutputStream(countersRoot.resolve(COUNTERS_FILE)),
            output ->
                  Flowable.fromIterable(counterNames)
                        .map(counter -> {
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
      Path cacheRoot = resolve(containerRoot, CACHES, cacheName);
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

   private void addResourceProperty(BackupManager.BackupParameters params, BackupManager.Resource resource, Properties properties) {
      String key = resource.toString();
      Set<String> set = params.get(resource);
      String value = set == null ? "" : String.join(",", set);
      properties.put(key, value);
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
