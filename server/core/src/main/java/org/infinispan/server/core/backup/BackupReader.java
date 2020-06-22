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
import static org.infinispan.server.core.backup.BackupUtil.MANIFEST_PROPERTIES_FILE;
import static org.infinispan.server.core.backup.BackupUtil.RESTORE_LOCAL_ZIP;
import static org.infinispan.server.core.backup.BackupUtil.cacheConfigFile;
import static org.infinispan.server.core.backup.BackupUtil.cacheDataFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletionStage;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.infinispan.AdvancedCache;
import org.infinispan.commons.CacheException;
import org.infinispan.commons.util.Util;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ParserRegistry;
import org.infinispan.counter.api.CounterConfiguration;
import org.infinispan.counter.api.CounterManager;
import org.infinispan.counter.api.CounterType;
import org.infinispan.counter.api.StrongCounter;
import org.infinispan.counter.api.WeakCounter;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.marshall.protostream.impl.SerializationContextRegistry;
import org.infinispan.metadata.Metadata;
import org.infinispan.metadata.impl.InternalMetadataImpl;
import org.infinispan.metadata.impl.PrivateMetadata;
import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.util.concurrent.BlockingManager;

/**
 * // TODO: Document this
 *
 * @author Ryan Emerson
 * @since 11.0
 */
class BackupReader {

   private final BlockingManager blockingManager;
   private final Map<String, DefaultCacheManager> cacheManagers;
   private final Path rootDir;
   private final ParserRegistry parserRegistry;

   public BackupReader(BlockingManager blockingManager, Map<String, DefaultCacheManager> cacheManagers, Path rootDir) {
      this.blockingManager = blockingManager;
      this.cacheManagers = cacheManagers;
      this.rootDir = rootDir;
      this.parserRegistry = new ParserRegistry();
   }

   CompletionStage<Void> restore(byte[] backup) {
      // TODO make split into more fine-grained blocking manager executions?
      return blockingManager.runBlocking(() -> {
         Path localZip = createLocalZip(backup);
         File zipFile = localZip.toFile();
         Properties manifest = readManifestAndValidate(zipFile);

         // Restore all containers specified in the manifest
         String[] containers = manifest.getProperty(CONTAINERS_PROPERTY).split(",");
         for (String container : containers) {
            restoreContainer(container, zipFile);
         }
      }, "restore-backup");
   }

   private Path createLocalZip(byte[] bytes) {
      try {
         Path localFile = rootDir.resolve(RESTORE_LOCAL_ZIP);
         Files.write(localFile, bytes);
         return localFile;
      } catch (IOException e) {
         throw new CacheException("Unable to copy backup bytes to local filesystem");
      }
   }

   private void restoreContainer(String containerName, File zipFile) {
      // TODO validate container config
      EmbeddedCacheManager cm = cacheManagers.get(containerName);
      Path containerPath = Paths.get(CONTAINER_DIR, containerName);
      Properties containerProperties = readProperties(containerPath.resolve(CONTAINERS_PROPERTIES_FILE), zipFile);

      try (ZipFile zip = new ZipFile(zipFile)) {
         // TODO delegate sub-tasks to BlockingManager
         for (String config : csvProperty(containerProperties, CACHES_CONFIG_PROPERTY)) {
            processCacheXml(config, containerPath.resolve(CACHE_CONFIG_DIR), cm, zip);
         }

         for (String cache : csvProperty(containerProperties, CACHES_PROPERTY)) {
            Path cacheRoot = containerPath.resolve(CACHES_DIR).resolve(cache);
            processCache(cache, cacheRoot, cm, zip);
         }
         processCounters(containerPath.resolve(COUNTERS_DIR), cm, zip);
      } catch (IOException e) {
         throw new CacheException("Unable to restore container", e);
      }
   }

   private void processCacheXml(String configName, Path configPath, EmbeddedCacheManager cm, ZipFile zip) throws
         IOException {
      String configFile = cacheConfigFile(configName);
      String zipPath = configPath.resolve(configFile).toString();
      try (InputStream is = zip.getInputStream(zip.getEntry(zipPath))) {
         ConfigurationBuilderHolder builderHolder = parserRegistry.parse(is, null);
         Configuration cfg = builderHolder.getNamedConfigurationBuilders().get(configName).build();

         // Only define configurations that don't already exist so that we don't overwrite newer versions of the default
         // templates e.g. org.infinispan.DIST_SYNC when upgrading a cluster
         if (cm.getCacheConfiguration(configName) == null)
            cm.defineConfiguration(configName, cfg);
      }
   }

   private void processCache(String cacheName, Path cacheRoot, EmbeddedCacheManager cm, ZipFile zip) throws
         IOException {
      // Recreate cache from xml
      processCacheXml(cacheName, cacheRoot, cm, zip);

      // Process .dat
      String xml = cacheDataFile(cacheName);
      String data = cacheRoot.resolve(xml).toString();
      AdvancedCache<Object, Object> cache = cm.getCache(cacheName).getAdvancedCache();
      ZipEntry zipEntry = zip.getEntry(data);
      if (zipEntry == null)
         return;

      SerializationContextRegistry ctxRegistry = cm.getGlobalComponentRegistry().getComponent(SerializationContextRegistry.class);
      ImmutableSerializationContext serCtx = ctxRegistry.getPersistenceCtx();
      try (InputStream is = zip.getInputStream(zipEntry)) {
         CacheBackupEntry entry = ProtobufUtil.readFrom(serCtx, is, CacheBackupEntry.class);
         Object key = entry.key;
         Object value = entry.value;
//         Metadata metadata = entry.metadata;
         // TODO How to restore PrivateMetadata?
         // Send put raw put command: https://github.com/infinispan/infinispan/blob/master/core/src/main/java/org/infinispan/xsite/ClusteredCacheBackupReceiver.java#L226
         PrivateMetadata internalMetadata = entry.internalMetadata;
         Metadata internalMetadataImpl = new InternalMetadataImpl(null, entry.created, entry.lastUsed);
         cache.put(key, value, internalMetadataImpl);
      }
   }

   private void processCounters(Path containerPath, EmbeddedCacheManager cm, ZipFile zip) throws IOException {
      String countersFile = containerPath.resolve(COUNTERS_FILE).toString();
      GlobalComponentRegistry gcr = cm.getGlobalComponentRegistry();
      CounterManager counterManager = gcr.getComponent(CounterManager.class);
      ZipEntry zipEntry = zip.getEntry(countersFile);
      if (counterManager == null || zipEntry == null)
         return;

      SerializationContextRegistry ctxRegistry = cm.getGlobalComponentRegistry().getComponent(SerializationContextRegistry.class);
      ImmutableSerializationContext serCtx = ctxRegistry.getPersistenceCtx();
      try (InputStream is = zip.getInputStream(zipEntry)) {
         CounterBackupEntry entry = ProtobufUtil.readFrom(serCtx, is, CounterBackupEntry.class);
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

   private Properties readManifestAndValidate(File zipFile) {
      Path manifestPath = Paths.get(MANIFEST_PROPERTIES_FILE);
      Properties properties = readProperties(manifestPath, zipFile);
      // TODO validate version, e.g. major version is within supported range
      return properties;
   }

   private Properties readProperties(Path file, File zipFile) {
      try (ZipFile zip = new ZipFile(zipFile);
           InputStream is = zip.getInputStream(zip.getEntry(file.toString()))) {
         Properties props = new Properties();
         props.load(is);
         return props;
      } catch (IOException e) {
         throw new CacheException("Unable to read properties file", e);
      }
   }

   private String[] csvProperty(Properties properties, String key) {
      String prop = properties.getProperty(key);
      if (prop == null || prop.isEmpty())
         return Util.EMPTY_STRING_ARRAY;
      return prop.split(",");
   }
}
