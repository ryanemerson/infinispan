package org.infinispan.server.core.backup;

import static org.infinispan.server.core.BackupManager.ResourceType.COUNTERS;
import static org.infinispan.server.core.BackupManager.ResourceType.PROTO_SCHEMAS;
import static org.infinispan.server.core.BackupManager.ResourceType.SCRIPTS;
import static org.infinispan.server.core.backup.BackupUtil.CONTAINERS_PROPERTIES_FILE;
import static org.infinispan.server.core.backup.BackupUtil.CONTAINER_KEY;
import static org.infinispan.server.core.backup.BackupUtil.COUNTERS_FILE;
import static org.infinispan.server.core.backup.BackupUtil.IOConsumer;
import static org.infinispan.server.core.backup.BackupUtil.MANIFEST_PROPERTIES_FILE;
import static org.infinispan.server.core.backup.BackupUtil.PROTO_CACHE_NAME;
import static org.infinispan.server.core.backup.BackupUtil.SCRIPT_CACHE_NAME;
import static org.infinispan.server.core.backup.BackupUtil.STAGING_ZIP;
import static org.infinispan.server.core.backup.BackupUtil.VERSION_KEY;
import static org.infinispan.server.core.backup.BackupUtil.asSet;
import static org.infinispan.server.core.backup.BackupUtil.readMessageStream;
import static org.infinispan.server.core.backup.BackupUtil.resolve;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.infinispan.Cache;
import org.infinispan.commons.CacheException;
import org.infinispan.commons.logging.LogFactory;
import org.infinispan.commons.util.Version;
import org.infinispan.counter.api.CounterConfiguration;
import org.infinispan.counter.api.CounterManager;
import org.infinispan.counter.api.CounterType;
import org.infinispan.counter.api.StrongCounter;
import org.infinispan.counter.api.WeakCounter;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.marshall.protostream.impl.SerializationContextRegistry;
import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.server.core.BackupManager;
import org.infinispan.server.core.logging.Log;
import org.infinispan.util.concurrent.BlockingManager;
import org.infinispan.util.concurrent.CompletionStages;

/**
 * Responsible for reading backup bytes and restoring the contents to the appropriate cache manager.
 *
 * @author Ryan Emerson
 * @since 11.0
 */
class BackupReader {

   private static final Log log = LogFactory.getLog(BackupReader.class, Log.class);

   private final BlockingManager blockingManager;
   private final Map<String, DefaultCacheManager> cacheManagers;
   private final Path rootDir;

   public BackupReader(BlockingManager blockingManager, Map<String, DefaultCacheManager> cacheManagers, Path rootDir) {
      this.blockingManager = blockingManager;
      this.cacheManagers = cacheManagers;
      this.rootDir = rootDir;
   }

   CompletionStage<Void> restore(byte[] backup, Map<String, BackupManager.Parameters> params) {
      // TODO split into more fine-grained blocking manager executions?
      return blockingManager.runBlocking(() -> {
         Path stagingFile = rootDir.resolve(STAGING_ZIP);
         try {
            Files.write(stagingFile, backup);
            try (ZipFile zip = new ZipFile(stagingFile.toFile())) {
               Properties manifest = readManifestAndValidate(zip);
               // Restore all containers specified in the manifest
               String[] containers = manifest.getProperty(CONTAINER_KEY).split(",");
               for (String container : containers) {
                  restoreContainer(container, params.get(container), zip);
               }
            }
         } catch (IOException e) {
            throw new CacheException("Unable to restore container", e);
         }

         try {
            Files.delete(stagingFile);
         } catch (IOException e) {
            log.errorf("Unable to remove '%s'", stagingFile, e);
         }
      }, "restore-backup");
   }

   private void restoreContainer(String containerName, BackupManager.Parameters params, ZipFile zip) throws IOException {
      // TODO validate container config
      EmbeddedCacheManager cm = cacheManagers.get(containerName);
      Path containerRoot = Paths.get(CONTAINER_KEY, containerName);

      Properties properties = readProperties(containerRoot.resolve(CONTAINERS_PROPERTIES_FILE), zip);

      ContainerResourceFactory factory = new ContainerResourceFactory(blockingManager, cm, containerRoot);
      Collection<ContainerResource> resources = factory.getResources(params);
      List<CompletionStage<?>> stages = resources.stream()
            .map(r -> r.restore(properties, zip))
            .collect(Collectors.toList());

      // TODO update to return CompletionStage
      try {
         CompletionStages.allOf(stages).toCompletableFuture().get();
      } catch (InterruptedException | ExecutionException e) {
         throw new RuntimeException(e);
      }

      processAllResources(COUNTERS, params, properties, counters -> processCounters(containerRoot, counters, cm, zip));

      processAllResources(PROTO_SCHEMAS, params, properties, schemas -> processProtoSchemas(containerRoot, schemas, cm, zip));

      processAllResources(SCRIPTS, params, properties, scripts -> processScripts(containerRoot, scripts, cm, zip));
   }

   private void processCounters(Path containerPath, Set<String> countersToRestore, EmbeddedCacheManager cm, ZipFile zip) throws IOException {
      GlobalComponentRegistry gcr = cm.getGlobalComponentRegistry();
      CounterManager counterManager = gcr.getComponent(CounterManager.class);
      if (counterManager == null) {
         if (!countersToRestore.isEmpty())
            throw log.missingBackupResourceModule(COUNTERS);
         return;
      }

      String countersFile = resolve(containerPath, COUNTERS, COUNTERS_FILE).toString();
      ZipEntry zipEntry = zip.getEntry(countersFile);
      if (zipEntry == null) {
         if (!countersToRestore.isEmpty())
            throw log.unableToFindBackupResource(COUNTERS, countersToRestore);
         return;
      }

      SerializationContextRegistry ctxRegistry = gcr.getComponent(SerializationContextRegistry.class);
      ImmutableSerializationContext serCtx = ctxRegistry.getPersistenceCtx();
      try (InputStream is = zip.getInputStream(zipEntry)) {
         while (is.available() > 0) {
            CounterBackupEntry entry = readMessageStream(serCtx, CounterBackupEntry.class, is);
            if (!countersToRestore.isEmpty() && !countersToRestore.contains(entry.name)) {
               log.debugf("Ignoring '%s' counter", entry.name);
               continue;
            }
            CounterConfiguration config = entry.configuration;
            counterManager.defineCounter(entry.name, config);
            if (config.type() == CounterType.WEAK) {
               WeakCounter counter = counterManager.getWeakCounter(entry.name);
               counter.add(entry.value - config.initialValue());
            } else {
               StrongCounter counter = counterManager.getStrongCounter(entry.name);
               counter.compareAndSet(config.initialValue(), entry.value);
            }
         }
      }
   }

   private void processProtoSchemas(Path containerPath, Set<String> schemasToRestore, EmbeddedCacheManager cm, ZipFile zip) throws IOException {
      Path scriptPath = resolve(containerPath, PROTO_SCHEMAS);
      processInternalCache(scriptPath, schemasToRestore, PROTO_CACHE_NAME, cm, zip);
   }

   private void processScripts(Path containerPath, Set<String> scriptsToRestore, EmbeddedCacheManager cm, ZipFile zip) throws IOException {
      Path scriptPath = resolve(containerPath, SCRIPTS);
      processInternalCache(scriptPath, scriptsToRestore, SCRIPT_CACHE_NAME, cm, zip);
   }

   private void processInternalCache(Path root, Set<String> files, String cacheName, EmbeddedCacheManager cm, ZipFile zip) throws IOException {
      if (cm.getCacheConfiguration(cacheName) != null) {
         Cache<String, String> cache = cm.getCache(cacheName);
         for (String file : files) {
            String zipPath = root.resolve(file).toString();
            try (InputStream is = zip.getInputStream(zip.getEntry(zipPath));
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
               String content = reader.lines().collect(Collectors.joining("\n"));
               cache.put(file, content);
            }
         }
      }
   }

   private void processAllResources(BackupManager.ResourceType resource, BackupManager.Parameters parameters, Properties properties, IOConsumer<Set<String>> consumer) {
      Set<String> resourcesToProcess = getResourceNames(resource, parameters, properties);
      if (resourcesToProcess != null) {
         try {
            consumer.accept(resourcesToProcess);
         } catch (IOException e) {
            throw new CacheException(e);
         }
      }
   }

   private Set<String> getResourceNames(BackupManager.ResourceType resource, BackupManager.Parameters parameters, Properties properties) {
//      Set<String> requestedResources = parameters.get(resource);
      Set<String> requestedResources = null;
      if (requestedResources == null) {
         log.debugf("Ignoring %s resources", resource);
         return null;
      }

      // Only process specific resources if specified
      Set<String> backupContents = asSet(properties, resource);
      Set<String> resourcesToProcess = new HashSet<>(backupContents);
      if (!requestedResources.isEmpty())
         resourcesToProcess.retainAll(requestedResources);

      // The requested resource(s) cannot be found in the backup properties file, so abort the restore
      if (resourcesToProcess.isEmpty()) {
         requestedResources.removeAll(backupContents);
         throw log.unableToFindBackupResource(resource, requestedResources);
      }
      return resourcesToProcess;
   }

   private Properties readManifestAndValidate(ZipFile zip) {
      Path manifestPath = Paths.get(MANIFEST_PROPERTIES_FILE);
      Properties properties = readProperties(manifestPath, zip);
      String version = properties.getProperty(VERSION_KEY);
      if (version == null)
         throw new IllegalStateException("Missing manifest version");

      int majorVersion = Integer.parseInt(version.split("[\\.\\-]")[0]);
      // TODO replace with check that version difference is in the supported range, i.e. across 3 majors etc
      if (majorVersion < 12)
         throw new CacheException(String.format("Unable to restore from a backup as '%s' is no longer supported in '%s %s'",
               version, Version.getBrandName(), Version.getVersion()));
      return properties;
   }

   private Properties readProperties(Path file, ZipFile zip) {
      try (InputStream is = zip.getInputStream(zip.getEntry(file.toString()))) {
         Properties props = new Properties();
         props.load(is);
         return props;
      } catch (IOException e) {
         throw new CacheException("Unable to read properties file", e);
      }
   }
}
