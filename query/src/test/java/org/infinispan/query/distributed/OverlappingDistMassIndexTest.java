package org.infinispan.query.distributed;

import static org.infinispan.configuration.cache.IndexStorage.LOCAL_HEAP;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.query.test.Block;
import org.infinispan.query.test.QueryTestSCI;
import org.infinispan.query.test.Transaction;
import org.testng.annotations.Test;

/**
 * Tests for entities sharing the same index in DIST caches.
 *
 * @author gustavonalle
 * @since 7.1
 */
@Test(groups = "functional", testName = "query.distributed.OverlappingDistMassIndexTest")
public class OverlappingDistMassIndexTest extends OverlappingIndexMassIndexTest {

   @Override
   @SuppressWarnings("unchecked")
   protected void createCacheManagers() throws Throwable {
      ConfigurationBuilder cacheCfg = getDefaultClusteredCacheConfig(CacheMode.DIST_SYNC, false);
      cacheCfg
            .indexing()
            .enable()
            .storage(LOCAL_HEAP)
            .addIndexedEntity(Transaction.class)
            .addIndexedEntity(Block.class);

      createClusteredCaches(NUM_NODES, QueryTestSCI.INSTANCE, cacheCfg);

      waitForClusterToForm(getDefaultCacheName());

      caches = caches();
   }
}
