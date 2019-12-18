package org.infinispan.persistence.jdbc.stringbased;

import static org.testng.AssertJUnit.assertNotNull;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.infinispan.Cache;
import org.infinispan.commons.time.TimeService;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.context.Flag;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.persistence.jdbc.UnitTestDatabaseManager;
import org.infinispan.persistence.jdbc.configuration.JdbcStringBasedStoreConfigurationBuilder;
import org.infinispan.persistence.manager.PersistenceManager;
import org.infinispan.test.MultipleCacheManagersTest;
import org.infinispan.test.TestingUtil;
import org.infinispan.util.ControlledTimeService;
import org.jgroups.util.Util;
import org.testng.annotations.Test;

@Test(groups = "functional", testName = "persistence.jdbc.stringbased.JdbcPurgeDeadlockTest")
public class JdbcPurgeDeadlockTest extends MultipleCacheManagersTest {

   private final ControlledTimeService timeService = new ControlledTimeService();

   @Override
   protected void createCacheManagers() throws Throwable {
      ConfigurationBuilder config = new ConfigurationBuilder();
      config.clustering().cacheMode(CacheMode.DIST_SYNC);
      JdbcStringBasedStoreConfigurationBuilder storeBuilder = config.persistence()
            .addStore(JdbcStringBasedStoreConfigurationBuilder.class)
            .shared(true);
      UnitTestDatabaseManager.configureUniqueConnectionFactory(storeBuilder);
      UnitTestDatabaseManager.buildTableManipulation(storeBuilder.table());
      createCluster(config, 2);
      for (EmbeddedCacheManager cm : managers())
         TestingUtil.replaceComponent(cm, TimeService.class, timeService, true);
   }

   public void sharedStorePurgeDeadlockTest() {
      int lifespan = 1000;
      Cache<Integer, String> cache = cache(0);
//      IntStream.range(0, 1000).forEach(key -> cache.put(key, "Value", lifespan, TimeUnit.MILLISECONDS));
      cache.put(999, "Value", 1000, TimeUnit.MILLISECONDS);
      assertNotNull(advancedCache(1).withFlags(Flag.CACHE_MODE_LOCAL).get(999));
      timeService.advance(lifespan * 2);
      // Purge entries on the coordinator
      TestingUtil.extractComponent(cache, PersistenceManager.class).purgeExpired();
      Util.sleep(5000);
   }
}
