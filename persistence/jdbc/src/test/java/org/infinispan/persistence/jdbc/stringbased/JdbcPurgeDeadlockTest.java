package org.infinispan.persistence.jdbc.stringbased;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import java.util.stream.IntStream;

import org.infinispan.Cache;
import org.infinispan.commons.time.TimeService;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.context.Flag;
import org.infinispan.distribution.MagicKey;
import org.infinispan.expiration.impl.InternalExpirationManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.persistence.jdbc.UnitTestDatabaseManager;
import org.infinispan.persistence.jdbc.configuration.JdbcStringBasedStoreConfigurationBuilder;
import org.infinispan.persistence.keymappers.TwoWayKey2StringMapper;
import org.infinispan.test.MultipleCacheManagersTest;
import org.infinispan.test.TestDataSCI;
import org.infinispan.test.TestingUtil;
import org.infinispan.util.ControlledTimeService;
import org.jgroups.util.Util;
import org.testng.annotations.Test;

@Test(groups = "functional", testName = "persistence.jdbc.stringbased.JdbcPurgeDeadlockTest")
public class JdbcPurgeDeadlockTest extends MultipleCacheManagersTest {

   private static final int LIFESPAN = 1000;
   private static Object key;
   private final ControlledTimeService timeService = new ControlledTimeService();

   @Override
   protected void createCacheManagers() throws Throwable {
      ConfigurationBuilder config = new ConfigurationBuilder();
      config.clustering().cacheMode(CacheMode.DIST_SYNC).expiration().lifespan(LIFESPAN);
      JdbcStringBasedStoreConfigurationBuilder storeBuilder = config.persistence()
            .addStore(JdbcStringBasedStoreConfigurationBuilder.class)
//            .key2StringMapper(Mapper.class)
            .shared(true);
      UnitTestDatabaseManager.configureUniqueConnectionFactory(storeBuilder);
      UnitTestDatabaseManager.buildTableManipulation(storeBuilder.table());
      createCluster(TestDataSCI.INSTANCE, config, 2);
//      key = new MagicKey(cache(0), cache(1));
      for (EmbeddedCacheManager cm : managers())
         TestingUtil.replaceComponent(cm, TimeService.class, timeService, true);
   }

   public void sharedStorePurgeDeadlockTest() {
      Cache<Object, String> cache = cache(0);
      IntStream.range(0, 1000).forEach(key -> cache.put(key, "Value"));
//      cache.put(key, "Value");
      key = 999;
      assertNotNull(advancedCache(1).withFlags(Flag.CACHE_MODE_LOCAL).get(key));
      timeService.advance(LIFESPAN * 2);
      // Purge entries on the coordinator
      TestingUtil.extractComponent(cache, InternalExpirationManager.class).processExpiration();
      Util.sleep(5000);
//      assertNull(advancedCache(1).withFlags(Flag.CACHE_MODE_LOCAL).get(key));
   }

   static class Mapper implements TwoWayKey2StringMapper {

      @Override
      public boolean isSupportedType(Class<?> keyType) {
         return keyType == MagicKey.class;
      }

      @Override
      public String getStringMapping(Object key) {
         return "1";
      }

      @Override
      public Object getKeyMapping(String stringKey) {
         return key;
      }
   }
}
