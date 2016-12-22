package org.infinispan.tools.jdbc.migrator;

import static org.infinispan.tools.jdbc.migrator.Element.BATCH;
import static org.infinispan.tools.jdbc.migrator.Element.CACHE_NAME;
import static org.infinispan.tools.jdbc.migrator.Element.CONNECTION_POOL;
import static org.infinispan.tools.jdbc.migrator.Element.CONNECTION_URL;
import static org.infinispan.tools.jdbc.migrator.Element.DATA;
import static org.infinispan.tools.jdbc.migrator.Element.DIALECT;
import static org.infinispan.tools.jdbc.migrator.Element.DRIVER_CLASS;
import static org.infinispan.tools.jdbc.migrator.Element.ID;
import static org.infinispan.tools.jdbc.migrator.Element.KEY_MAPPER;
import static org.infinispan.tools.jdbc.migrator.Element.NAME;
import static org.infinispan.tools.jdbc.migrator.Element.PASSWORD;
import static org.infinispan.tools.jdbc.migrator.Element.SIZE;
import static org.infinispan.tools.jdbc.migrator.Element.SOURCE;
import static org.infinispan.tools.jdbc.migrator.Element.TABLE;
import static org.infinispan.tools.jdbc.migrator.Element.TABLE_NAME_PREFIX;
import static org.infinispan.tools.jdbc.migrator.Element.TARGET;
import static org.infinispan.tools.jdbc.migrator.Element.TIMESTAMP;
import static org.infinispan.tools.jdbc.migrator.Element.TYPE;
import static org.infinispan.tools.jdbc.migrator.Element.USERNAME;

import java.io.InputStream;
import java.util.Properties;

import javax.transaction.Status;
import javax.transaction.TransactionManager;

import org.infinispan.AdvancedCache;
import org.infinispan.commons.CacheConfigurationException;
import org.infinispan.commons.marshall.StreamingMarshaller;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.context.Flag;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.marshall.core.MarshalledEntry;
import org.infinispan.persistence.jdbc.DatabaseType;
import org.infinispan.persistence.jdbc.configuration.JdbcStringBasedStoreConfiguration;
import org.infinispan.persistence.jdbc.configuration.JdbcStringBasedStoreConfigurationBuilder;
import org.infinispan.transaction.TransactionMode;
import org.infinispan.transaction.lookup.DummyTransactionManagerLookup;

/**
 * @author Ryan Emerson
 * @since 9.0
 */
public class JDBCMigrator {

   private static final int DEFAULT_BATCH_SIZE = 1000;

   private final GlobalConfiguration globalConfiguration = new GlobalConfigurationBuilder()
         .globalJmxStatistics()
         .allowDuplicateDomains(true)
         .build();
   private final Properties properties;
   private DefaultCacheManager targetCacheManager;

   public JDBCMigrator(Properties properties) {
      this.properties = properties;
   }

   private void run() throws Exception {
      String batchSizeProp = property(BATCH, SIZE);
      int batchSize = batchSizeProp != null ? new Integer(batchSizeProp) : DEFAULT_BATCH_SIZE;

      AdvancedCache targetCache = initAndGetTargetCache();
      try (JdbcStoreReader sourceReader = initAndGetSourceReader()) {
         // Txs used so that writes to the DB are batched. Migrator will always operate locally overhead should be negligible
         TransactionManager tm = targetCache.getTransactionManager();
         int txBatchSize = 0;
         for (MarshalledEntry entry : sourceReader) {
            if (txBatchSize == 0) tm.begin();

            targetCache.put(entry.getKey(), entry.getValue());
            txBatchSize++;

            if (txBatchSize == batchSize) {
               txBatchSize = 0;
               tm.commit();
            }
         }
         if (tm.getStatus() == Status.STATUS_ACTIVE) tm.commit();
      }
   }

   private JdbcStoreReader initAndGetSourceReader() {
      StoreType storeType = StoreType.valueOf(property(SOURCE, TYPE).toUpperCase());
      String sourceCacheName = property(SOURCE, CACHE_NAME);

      JdbcStringBasedStoreConfigurationBuilder storeBuilder = new ConfigurationBuilder().persistence()
            .addStore(JdbcStringBasedStoreConfigurationBuilder.class);
      initStoreConfig(SOURCE, storeBuilder, false);

      JdbcStringBasedStoreConfiguration config = storeBuilder.create();
      Configuration configuration = new ConfigurationBuilder().persistence().addStore(storeBuilder).build();
      EmbeddedCacheManager manager = new DefaultCacheManager(globalConfiguration, configuration);
      StreamingMarshaller marshaller = manager.getCache().getAdvancedCache().getComponentRegistry().getComponent(StreamingMarshaller.class);

      return new JdbcStoreReader(sourceCacheName, storeType, marshaller, config);
   }

   private AdvancedCache initAndGetTargetCache() {
      String cacheName = property(TARGET, CACHE_NAME);
      ConfigurationBuilder builder = new ConfigurationBuilder();
      JdbcStringBasedStoreConfigurationBuilder storeBuilder = builder.persistence()
            .addStore(JdbcStringBasedStoreConfigurationBuilder.class)
            .transactional(true);

      builder.transaction()
            .transactionMode(TransactionMode.TRANSACTIONAL)
            .transactionManagerLookup(new DummyTransactionManagerLookup());

      initStoreConfig(TARGET, storeBuilder, true);
      targetCacheManager = new DefaultCacheManager(globalConfiguration);
      targetCacheManager.defineConfiguration(cacheName, builder.build());
      return targetCacheManager.getCache(cacheName).getAdvancedCache().withFlags(Flag.SKIP_CACHE_LOAD);
   }

   private void initStoreConfig(Element type, JdbcStringBasedStoreConfigurationBuilder storeBuilder, boolean createOnStart) {
      storeBuilder.dialect(DatabaseType.valueOf(property(type, DIALECT).toUpperCase()));

      storeBuilder.table()
            .createOnStart(createOnStart)
            .tableNamePrefix(property(type, TABLE, TABLE_NAME_PREFIX))
            .idColumnName(property(type, TABLE, ID, NAME))
            .idColumnType(property(type, TABLE, ID, TYPE))
            .dataColumnName(property(type, TABLE, DATA, NAME))
            .dataColumnType(property(type, TABLE, DATA, TYPE))
            .timestampColumnName(property(type, TABLE, TIMESTAMP, NAME))
            .timestampColumnType(property(type, TABLE, TIMESTAMP, TYPE));

      storeBuilder.connectionPool()
            .connectionUrl(property(type, CONNECTION_POOL, CONNECTION_URL))
            .driverClass(property(type, CONNECTION_POOL, DRIVER_CLASS))
            .username(property(type, CONNECTION_POOL, USERNAME))
            .password(property(type, CONNECTION_POOL, PASSWORD));

      String mapperProp = property(type, KEY_MAPPER);
      if (mapperProp != null)
         storeBuilder.key2StringMapper(mapperProp);

      storeBuilder.validate();

      if (property(type, CACHE_NAME) == null) {
         String msg = String.format("The cache name property must be specified for the %1$s store. e.g. '%1$s.%2$s=some_cache'",
               type, CACHE_NAME.toString());
         throw new CacheConfigurationException(msg);
      }
   }

   private String property(Element... elements) {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < elements.length; i++) {
         sb.append(elements[i].toString());
         if (i != elements.length - 1) sb.append(".");
      }
      return properties.getProperty(sb.toString());
   }

   public static void main(String[] args) throws Exception {
      Properties properties = new Properties();
      ClassLoader classLoader = JDBCMigrator.class.getClassLoader();
      try (InputStream in = classLoader.getResourceAsStream("example.properties")) {
         properties.load(in);
         new JDBCMigrator(properties).run();
      }
   }
}
