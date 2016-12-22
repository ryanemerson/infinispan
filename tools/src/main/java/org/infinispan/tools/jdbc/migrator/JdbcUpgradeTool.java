//package org.infinispan.persistence.migrator.jdbc;
//
//import static org.infinispan.factories.KnownComponentNames.CACHE_MARSHALLER;
//
//import java.io.FileReader;
//import java.lang.reflect.Method;
//import java.util.Properties;
//import java.util.Set;
//
//import org.infinispan.Cache;
//import org.infinispan.commons.marshall.AdvancedExternalizer;
//import org.infinispan.commons.marshall.StreamingMarshaller;
//import org.infinispan.commons.util.ReflectionUtil;
//import org.infinispan.configuration.cache.CacheMode;
//import org.infinispan.configuration.cache.ConfigurationBuilder;
//import org.infinispan.configuration.global.GlobalConfigurationBuilder;
//import org.infinispan.container.entries.InternalCacheEntry;
//import org.infinispan.loaders.AbstractCacheStore;
//import org.infinispan.loaders.CacheLoaderException;
//import org.infinispan.loaders.CacheStore;
//import org.infinispan.loaders.bucket.Bucket;
//import org.infinispan.loaders.jdbc.AbstractJdbcCacheStoreConfig;
//import org.infinispan.loaders.jdbc.AbstractNonDelegatingJdbcCacheStoreConfig;
//import org.infinispan.loaders.jdbc.binary.JdbcBinaryCacheStore;
//import org.infinispan.loaders.jdbc.binary.JdbcBinaryCacheStoreConfig;
//import org.infinispan.loaders.jdbc.connectionfactory.SimpleConnectionFactory;
//import org.infinispan.loaders.jdbc.mixed.JdbcMixedCacheStore;
//import org.infinispan.loaders.jdbc.mixed.JdbcMixedCacheStoreConfig;
//import org.infinispan.loaders.jdbc.stringbased.JdbcStringBasedCacheStore;
//import org.infinispan.loaders.jdbc.stringbased.JdbcStringBasedCacheStoreConfig;
//import org.infinispan.manager.DefaultCacheManager;
//import org.infinispan.marshall.core.ExternalizerTable;
//import org.infinispan.persistence.jdbc.configuration.AbstractJdbcStoreConfigurationBuilder;
//import org.infinispan.persistence.jdbc.configuration.JdbcBinaryStoreConfigurationBuilder;
//import org.infinispan.persistence.jdbc.configuration.JdbcMixedStoreConfigurationBuilder;
//import org.infinispan.persistence.jdbc.configuration.JdbcStringBasedStoreConfigurationBuilder;
//
//public class JdbcUpgradeTool {
//   private final CacheStore sourceStore;
//   private final JDBCStoreType storeType;
//   private StreamingMarshaller marshaller;
//   private Cache<Object, Object> targetCache;
//   private DefaultCacheManager cacheManager;
//
//   public JdbcUpgradeTool(Properties properties) throws Exception {
//      String type = properties.getProperty("type");
//      storeType = JDBCStoreType.valueOf(type.toUpperCase());
//      initializeTargetStore(filterProperties(properties, "target."));
//      sourceStore = initializeSourceStore(filterProperties(properties, "source."));
//   }
//
//   private void initializeTargetStore(Properties properties) throws Exception {
//      GlobalConfigurationBuilder global = new GlobalConfigurationBuilder();
//      ConfigurationBuilder builder = new ConfigurationBuilder();
//      AbstractJdbcStoreConfigurationBuilder<?, ?> storeBuilder;
//      switch (storeType) {
//         case STRING:
//            storeBuilder = builder.persistence().addStore(JdbcStringBasedStoreConfigurationBuilder.class);
//            break;
//         case BINARY:
//            storeBuilder = builder.persistence().addStore(JdbcBinaryStoreConfigurationBuilder.class);
//            break;
//         case MIXED:
//            storeBuilder = builder.persistence().addStore(JdbcMixedStoreConfigurationBuilder.class);
//            break;
//         default:
//            throw new RuntimeException("Unknown store type : " + storeType);
//      }
//      storeBuilder.simpleConnection().withProperties(properties);
//      if (storeBuilder instanceof JdbcMixedStoreConfigurationBuilder) {
//         JdbcMixedStoreConfigurationBuilder mixed = (JdbcMixedStoreConfigurationBuilder) storeBuilder;
//         mixed.stringTable().tableNamePrefix(properties.getProperty("tableNamePrefixForStrings"));
//         mixed.binaryTable().tableNamePrefix(properties.getProperty("tableNamePrefixForBinary"));
//      }
//      cacheManager = new DefaultCacheManager(global.build());
//      cacheManager.defineConfiguration(properties.getProperty("cacheName"), builder.build());
//      targetCache = cacheManager.getCache(properties.getProperty("cacheName"));
//      ExternalizerTable externalizerTable = cacheManager.getGlobalComponentRegistry().getComponent(ExternalizerTable.class);
//      Method method = externalizerTable.getClass().getDeclaredMethod("addInternalExternalizer", AdvancedExternalizer.class);
//      ReflectionUtil.invokeAccessibly(externalizerTable, method, new Object[] {new Bucket.Externalizer()});
//      marshaller = targetCache.getAdvancedCache().getComponentRegistry().getComponent(StreamingMarshaller.class, CACHE_MARSHALLER);
//   }
//
//   private CacheStore initializeSourceStore(Properties properties) throws CacheLoaderException {
//      ConfigurationBuilder builder = new ConfigurationBuilder();
//      builder.clustering().cacheMode(CacheMode.DIST_SYNC);
//      Cache<?, ?> cache = new SyntheticCache(properties.getProperty("cacheName"), builder.build());
//      AbstractCacheStore store;
//      AbstractJdbcCacheStoreConfig config;
//      switch (storeType) {
//         case STRING:
//            config = new JdbcStringBasedCacheStoreConfig();
//            store = new JdbcStringBasedCacheStore();
//            break;
//         case BINARY:
//            config = new JdbcBinaryCacheStoreConfig();
//            store = new JdbcBinaryCacheStore();
//            break;
//         case MIXED:
//            config = new JdbcMixedCacheStoreConfig();
//            store = new JdbcMixedCacheStore();
//            break;
//         default:
//            throw new RuntimeException("Unknown store type : " + storeType);
//      }
//      // Common connection properties
//      config.setConnectionFactoryClass(SimpleConnectionFactory.class.getName());
//      config.setConnectionUrl(properties.getProperty("connectionUrl"));
//      config.setDriverClass(properties.getProperty("driverClass"));
//      config.setUserName(properties.getProperty("username"));
//      config.setPassword(properties.getProperty("password"));
//
//      if (config instanceof AbstractNonDelegatingJdbcCacheStoreConfig) {
//         AbstractNonDelegatingJdbcCacheStoreConfig cfg = (AbstractNonDelegatingJdbcCacheStoreConfig) config;
//         cfg.setCacheName(cache.getName());
//         cfg.setIdColumnName(properties.getProperty("idColumnName"));
//         cfg.setIdColumnType(properties.getProperty("idColumnType"));
//         cfg.setDataColumnName(properties.getProperty("dataColumnName"));
//         cfg.setDataColumnType(properties.getProperty("dataColumnType"));
//         cfg.setTimestampColumnName(properties.getProperty("timestampColumnName"));
//         cfg.setTimestampColumnType(properties.getProperty("timestampColumnType"));
//      }
//      if (config instanceof JdbcStringBasedCacheStoreConfig) {
//         JdbcStringBasedCacheStoreConfig cfg = (JdbcStringBasedCacheStoreConfig) config;
//         cfg.setTableNamePrefix(properties.getProperty("tableNamePrefix"));
//      }
//      if (config instanceof JdbcBinaryCacheStoreConfig) {
//         JdbcBinaryCacheStoreConfig cfg = (JdbcBinaryCacheStoreConfig) config;
//         cfg.setTableNamePrefix(properties.getProperty("tableNamePrefix"));
//         cfg.setBucketTableNamePrefix(properties.getProperty("bucketTableNamePrefix"));
//      }
//      if (config instanceof JdbcMixedCacheStoreConfig) {
//         JdbcMixedCacheStoreConfig cfg = (JdbcMixedCacheStoreConfig) config;
//         cfg.setIdColumnNameForStrings(properties.getProperty("idColumnNameForStrings"));
//         cfg.setIdColumnTypeForStrings(properties.getProperty("idColumnTypeForStrings"));
//         cfg.setDataColumnNameForStrings(properties.getProperty("dataColumnNameForStrings"));
//         cfg.setDataColumnTypeForStrings(properties.getProperty("dataColumnTypeForStrings"));
//         cfg.setTimestampColumnNameForStrings(properties.getProperty("timestampColumnNameForStrings"));
//         cfg.setTimestampColumnTypeForStrings(properties.getProperty("timestampColumnTypeForStrings"));
//         cfg.setTableNamePrefixForStrings(properties.getProperty("tableNamePrefixForStrings"));
//
//         cfg.setIdColumnNameForBinary(properties.getProperty("idColumnNameForBinary"));
//         cfg.setIdColumnTypeForBinary(properties.getProperty("idColumnTypeForBinary"));
//         cfg.setDataColumnNameForBinary(properties.getProperty("dataColumnNameForBinary"));
//         cfg.setDataColumnTypeForBinary(properties.getProperty("dataColumnTypeForBinary"));
//         cfg.setTimestampColumnNameForBinary(properties.getProperty("timestampColumnNameForBinary"));
//         cfg.setTimestampColumnTypeForBinary(properties.getProperty("timestampColumnTypeForBinary"));
//         cfg.setTableNamePrefixForBinary(properties.getProperty("tableNamePrefixForBinary"));
//      }
//
//      store.init(config, cache, marshaller);
//      return store;
//   }
//
//   private Properties filterProperties(Properties properties, String prefix) {
//      Properties p = new Properties();
//      for (Object okey : properties.keySet()) {
//         String key = (String) okey;
//         if (key.startsWith(prefix)) {
//            String localKey = key.substring(prefix.length());
//            p.put(localKey, properties.get(key));
//         }
//      }
//      return p;
//   }
//
//   public long run() throws Exception {
//      try {
//         sourceStore.start();
//         Set<Object> sourceKeys = sourceStore.loadAllKeys(null);
//         long count = 0;
//         for (Object key : sourceKeys) {
//            InternalCacheEntry entry = sourceStore.load(key);
//            targetCache.getAdvancedCache().put(entry.getKey(), entry.getValue(), entry.getMetadata());
//            ++count;
//         }
//         return count;
//      } finally {
//         sourceStore.stop();
//         cacheManager.stop();
//      }
//   }
//
//   public static void main(String[] args) {
//      if (args.length != 1) {
//         System.err.println("Usage: JdbcUpgradeTool config.properties");
//         System.exit(1);
//      }
//      try {
//         Properties props = new Properties();
//         props.load(new FileReader(args[0]));
//         JdbcUpgradeTool upgradeTool = new JdbcUpgradeTool(props);
//         upgradeTool.run();
//      } catch (Exception e) {
//         System.err.println("Error: " + e.getMessage());
//         e.printStackTrace(System.err);
//      }
//
//   }
//
//}
