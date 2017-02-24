package org.infinispan.tools.jdbc.migrator;

import static org.infinispan.tools.jdbc.migrator.Element.BATCH;
import static org.infinispan.tools.jdbc.migrator.Element.SIZE;

import java.io.FileReader;
import java.util.Properties;

import javax.transaction.Status;
import javax.transaction.TransactionManager;

import org.infinispan.AdvancedCache;
import org.infinispan.commons.marshall.StreamingMarshaller;
import org.infinispan.commons.marshall.jboss.GenericJBossMarshaller;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.context.Flag;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.marshall.core.JBossMarshaller;
import org.infinispan.marshall.core.MarshalledEntry;
import org.infinispan.tools.jdbc.migrator.marshaller.LegacyJBossMarshaller;
import org.infinispan.tools.jdbc.migrator.marshaller.VersionAwareMarshaller;

/**
 * @author Ryan Emerson
 * @since 9.0
 */
public class JDBCMigrator {

   private static final int DEFAULT_BATCH_SIZE = 1000;
   private final String defaultCacheName = this.getClass().getName();
   private final GlobalConfiguration globalConfiguration = new GlobalConfigurationBuilder()
         .defaultCacheName(defaultCacheName)
         .globalJmxStatistics()
         .allowDuplicateDomains(true)
         .build();
   private final Properties properties;

   private JDBCMigrator(Properties properties) {
      this.properties = properties;
   }

   private void run() throws Exception {
      String batchSizeProp = properties.getProperty(BATCH + "." + SIZE);
      int batchLimit = batchSizeProp != null ? new Integer(batchSizeProp) : DEFAULT_BATCH_SIZE;

      try (JdbcStoreReader sourceReader = initAndGetSourceReader()) {
         AdvancedCache targetCache = initAndGetTargetCache();
         // Txs used so that writes to the DB are batched. Migrator will always operate locally Tx overhead should be negligible
         TransactionManager tm = targetCache.getTransactionManager();
         int txBatchSize = 0;
         for (MarshalledEntry entry : sourceReader) {
            if (txBatchSize == 0) tm.begin();

            targetCache.put(entry.getKey(), entry.getValue());
            txBatchSize++;

            if (txBatchSize == batchLimit) {
               txBatchSize = 0;
               tm.commit();
            }
         }
         if (tm.getStatus() == Status.STATUS_ACTIVE) tm.commit();
      }
   }

   private JdbcStoreReader initAndGetSourceReader() {
      MigratorConfiguration config = new MigratorConfiguration(true, properties);
      if (!config.hasCustomMarshaller()) {
         // TODO configure user Externalizers
         config.setMarshaller(new VersionAwareMarshaller());
      }
      return new JdbcStoreReader(config);
   }

   private AdvancedCache initAndGetTargetCache() {
      MigratorConfiguration config = new MigratorConfiguration(false, properties);
      GlobalConfiguration globalConfig = globalConfiguration;
      if (config.hasCustomMarshaller()) {
         globalConfig = new GlobalConfigurationBuilder()
               .globalJmxStatistics().allowDuplicateDomains(true)
               .serialization().marshaller(config.getMarshaller())
               .build();
      }
      Configuration cacheConfig = new ConfigurationBuilder().persistence().addStore(config.getJdbcConfigBuilder()).build();
      DefaultCacheManager targetCacheManager = new DefaultCacheManager(globalConfig, new ConfigurationBuilder().build());
      targetCacheManager.defineConfiguration(config.cacheName, cacheConfig);
      return targetCacheManager.getCache(config.cacheName).getAdvancedCache().withFlags(Flag.SKIP_CACHE_LOAD);
   }

   public static void main(String[] args) throws Exception {
      if (args.length != 1) {
         System.err.println("Usage: JDBCMigrator migrator.properties");
         System.exit(1);
      }
      Properties properties = new Properties();
      properties.load(new FileReader(args[0]));
      new JDBCMigrator(properties).run();
   }
}
