package org.infinispan.tools.jdbc.migrator;

import static org.infinispan.tools.jdbc.migrator.Element.CACHE_NAME;
import static org.infinispan.tools.jdbc.migrator.Element.CONNECTION_POOL;
import static org.infinispan.tools.jdbc.migrator.Element.CONNECTION_URL;
import static org.infinispan.tools.jdbc.migrator.Element.DATA;
import static org.infinispan.tools.jdbc.migrator.Element.DIALECT;
import static org.infinispan.tools.jdbc.migrator.Element.DRIVER_CLASS;
import static org.infinispan.tools.jdbc.migrator.Element.ID;
import static org.infinispan.tools.jdbc.migrator.Element.NAME;
import static org.infinispan.tools.jdbc.migrator.Element.PASSWORD;
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
import javax.transaction.Transaction;
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
import org.infinispan.transaction.tm.DummyBaseTransactionManager;
import org.infinispan.transaction.tm.DummyTransaction;

/**
 * @author Ryan Emerson
 * @since 9.0
 */
public class JDBCMigrator {

   // **************** Requirements ****************

   // Take properties from command line args that has source and destination details
   // Require TwoWayKey2StringMapper
   // Properties of string-keyed-table
   // All existing store properties; including type of source (MIXED, BINARY, STRING)
   // If STRING, then update tool can be used to change mapper implementation
   // If source is MIXED or STRING, then we need both a source and target Mapper for ::supportsKey(Key);

   // Use Flag SKIP_CACHE_LOAD for puts

   // Use Hikari CP and write batches to improve speed

   private final GlobalConfiguration globalConfiguration = new GlobalConfigurationBuilder()
         .globalJmxStatistics()
         .allowDuplicateDomains(true)
         .build();
   private final Properties properties;
   private DefaultCacheManager targetCacheManager;

   public JDBCMigrator(Properties properties) {
      this.properties = properties;
   }

   public void run() throws Exception {
      AdvancedCache targetCache = initAndGetTargetCache();
      try (JdbcStoreReader sourceReader = initAndGetSourceReader()) {
         TransactionManager tm = targetCache.getTransactionManager();
         int batchSize = 20;
         int txEntryCount = 0;
         for (MarshalledEntry entry : sourceReader) {
            if (txEntryCount == 0) tm.begin();

            System.out.println("Key: " + entry.getKey() + " | val: " + entry.getValue());
            targetCache.put(entry.getKey(), entry.getValue());
            txEntryCount++;

            if (txEntryCount == batchSize) {
               txEntryCount = 0;
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

      builder.transaction().transactionMode(TransactionMode.TRANSACTIONAL).transactionManagerLookup(new DummyTransactionManagerLookup());
      initStoreConfig(TARGET, storeBuilder, true);
      // TODO specify other settings

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
