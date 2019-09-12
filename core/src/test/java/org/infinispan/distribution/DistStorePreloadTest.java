package org.infinispan.distribution;

import static org.testng.AssertJUnit.assertEquals;

import org.infinispan.Cache;
import org.infinispan.container.DataContainer;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.persistence.PersistenceUtil;
import org.infinispan.persistence.spi.AdvancedCacheLoader;
import org.infinispan.persistence.spi.AdvancedLoadWriteStore;
import org.infinispan.persistence.spi.PersistenceException;
import org.infinispan.test.TestDataSCI;
import org.infinispan.test.TestingUtil;
import org.infinispan.test.fwk.InTransactionMode;
import org.infinispan.test.fwk.TransportFlags;
import org.infinispan.transaction.TransactionMode;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

/**
 * Test preloading with a distributed cache.
 *
 * @author Dan Berindei &lt;dan@infinispan.org&gt;
 * @since 5.1
 */
@Test(groups = "functional", testName = "distribution.DistStorePreloadTest")
@InTransactionMode({ TransactionMode.TRANSACTIONAL, TransactionMode.NON_TRANSACTIONAL })
public class DistStorePreloadTest<D extends DistStorePreloadTest> extends BaseDistStoreTest<Object, String, D> {

   public static final int NUM_KEYS = 10;

   public DistStorePreloadTest() {
      INIT_CLUSTER_SIZE = 1;
      testRetVals = true;
      preload = true;
      // Has to be shared as well, otherwise preload can't load anything
      shared = true;
   }

   @Override
   public Object[] factory() {
      return new Object[] {
            new DistStorePreloadTest().segmented(true),
            new DistStorePreloadTest().segmented(false),
      };
   }

   @AfterMethod
   public void clearStats() {
      for (Cache<?, ?> c: caches) {
         log.trace("Clearing stats for cache store on cache "+ c);
         AdvancedLoadWriteStore cs = TestingUtil.getFirstLoader(c);
         cs.clear();
      }
   }

   public void testPreloadOnStart() throws PersistenceException {
      for (int i = 0; i < NUM_KEYS; i++) {
         c1.put("k" + i, "v" + i);
      }
      DataContainer dc1 = c1.getAdvancedCache().getDataContainer();
      assert dc1.size() == NUM_KEYS;

      AdvancedCacheLoader cs = TestingUtil.getFirstLoader(c1);
      assert PersistenceUtil.count(cs, null) == NUM_KEYS;

      addClusterEnabledCacheManager(TestDataSCI.INSTANCE, configuration, new TransportFlags().withFD(false));
      EmbeddedCacheManager cm2 = cacheManagers.get(1);
      cm2.defineConfiguration(cacheName, buildConfiguration().build());
      c2 = cache(1, cacheName);
      caches.add(c2);
      waitForClusterToForm();

      DataContainer dc2 = c2.getAdvancedCache().getDataContainer();
      assertEquals("Expected all the cache store entries to be preloaded on the second cache", NUM_KEYS, dc2.size());

      for (int i = 0; i < NUM_KEYS; i++) {
         assertOwnershipAndNonOwnership("k" + i, true);
      }
   }
}
