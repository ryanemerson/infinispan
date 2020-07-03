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
import static org.infinispan.server.core.backup.BackupUtil.PROTO_CACHE_NAME;
import static org.infinispan.server.core.backup.BackupUtil.PROTO_SCHEMA_PROPERTY;
import static org.infinispan.server.core.backup.BackupUtil.RESTORE_LOCAL_ZIP;
import static org.infinispan.server.core.backup.BackupUtil.cacheConfigFile;
import static org.infinispan.server.core.backup.BackupUtil.cacheDataFile;
import static org.infinispan.server.core.backup.BackupUtil.readMessageStream;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.cache.impl.InvocationHelper;
import org.infinispan.commands.CommandsFactory;
import org.infinispan.commands.write.PutKeyValueCommand;
import org.infinispan.commons.CacheException;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.commons.marshall.MarshallingException;
import org.infinispan.commons.util.Util;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ParserRegistry;
import org.infinispan.context.impl.FlagBitSets;
import org.infinispan.counter.api.CounterConfiguration;
import org.infinispan.counter.api.CounterManager;
import org.infinispan.counter.api.CounterType;
import org.infinispan.counter.api.StrongCounter;
import org.infinispan.counter.api.WeakCounter;
import org.infinispan.distribution.ch.KeyPartitioner;
import org.infinispan.encoding.impl.StorageConfigurationManager;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.marshall.persistence.PersistenceMarshaller;
import org.infinispan.marshall.protostream.impl.SerializationContextRegistry;
import org.infinispan.metadata.Metadata;
import org.infinispan.metadata.impl.InternalMetadataImpl;
import org.infinispan.protostream.ImmutableSerializationContext;
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

         Cache<String, String> protoCache = cm.getCache(PROTO_CACHE_NAME);
         for (String schema : csvProperty(containerProperties, PROTO_SCHEMA_PROPERTY)) {
            processProtoSchema(schema, containerPath, protoCache, zip);
         }

         processCounters(containerPath.resolve(COUNTERS_DIR), cm, zip);
      } catch (IOException e) {
         throw new CacheException("Unable to restore container", e);
      }
   }

   private Configuration processCacheXml(String configName, Path configPath, EmbeddedCacheManager cm, ZipFile zip)
         throws IOException {
      String configFile = cacheConfigFile(configName);
      String zipPath = configPath.resolve(configFile).toString();
      try (InputStream is = zip.getInputStream(zip.getEntry(zipPath))) {
         ConfigurationBuilderHolder builderHolder = parserRegistry.parse(is, null);
         Configuration cfg = builderHolder.getNamedConfigurationBuilders().get(configName).build();

         // Only define configurations that don't already exist so that we don't overwrite newer versions of the default
         // templates e.g. org.infinispan.DIST_SYNC when upgrading a cluster
         if (cm.getCacheConfiguration(configName) == null)
            cm.defineConfiguration(configName, cfg);

         return cfg;
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


      ComponentRegistry cr = cache.getComponentRegistry();
      CommandsFactory commandsFactory = cr.getCommandsFactory();
      KeyPartitioner keyPartitioner = cr.getComponent(KeyPartitioner.class);
      InvocationHelper invocationHelper = cr.getComponent(InvocationHelper.class);
      StorageConfigurationManager scm = cr.getComponent(StorageConfigurationManager.class);
      PersistenceMarshaller persistenceMarshaller = cr.getPersistenceMarshaller();
      Marshaller userMarshaller = persistenceMarshaller.getUserMarshaller();

      boolean keyMarshalling = MediaType.APPLICATION_OBJECT.equals(scm.getValueStorageMediaType());
      boolean valueMarshalling = MediaType.APPLICATION_OBJECT.equals(scm.getValueStorageMediaType());

      SerializationContextRegistry ctxRegistry = cm.getGlobalComponentRegistry().getComponent(SerializationContextRegistry.class);
      ImmutableSerializationContext serCtx = ctxRegistry.getPersistenceCtx();
      try (InputStream is = zip.getInputStream(zipEntry)) {
         while (is.available() > 0) {
            CacheBackupEntry entry = readMessageStream(serCtx, CacheBackupEntry.class, is);
            Object key = keyMarshalling ? unmarshall(entry.key, userMarshaller) : scm.getKeyWrapper().wrap(entry.key);
            Object value = valueMarshalling ? unmarshall(entry.value, userMarshaller) : scm.getKeyWrapper().wrap(entry.value);
            Metadata metadata = unmarshall(entry.metadata, persistenceMarshaller);
            Metadata internalMetadataImpl = new InternalMetadataImpl(metadata, entry.created, entry.lastUsed);

            PutKeyValueCommand cmd = commandsFactory.buildPutKeyValueCommand(key, value, keyPartitioner.getSegment(key),
                  internalMetadataImpl, FlagBitSets.IGNORE_RETURN_VALUES);
            cmd.setInternalMetadata(entry.internalMetadata);
            invocationHelper.invoke(cmd, 1);
         }
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
         CounterBackupEntry entry = readMessageStream(serCtx, CounterBackupEntry.class, is);
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

   private void processProtoSchema(String schema, Path containerRoot, Cache<String, String> cache, ZipFile zip) throws IOException {
      String zipPath = containerRoot.resolve(schema).toString();
      try (InputStream is = zip.getInputStream(zip.getEntry(zipPath));
           BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
         String content = reader.lines().collect(Collectors.joining("\n"));
         cache.put(schema, content);
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

   @SuppressWarnings("unchecked")
   private static <T> T unmarshall(byte[] bytes, Marshaller marshaller) {
      try {
         return (T) marshaller.objectFromByteBuffer(bytes);
      } catch (ClassNotFoundException | IOException e) {
         throw new MarshallingException(e);
      }
   }
}
