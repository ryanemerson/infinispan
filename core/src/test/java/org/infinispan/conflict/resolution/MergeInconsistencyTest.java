package org.infinispan.conflict.resolution;

import static org.infinispan.test.TestingUtil.waitForRehashToComplete;
import static org.testng.AssertJUnit.assertTrue;

import java.util.List;
import java.util.stream.IntStream;

import org.infinispan.AdvancedCache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.distribution.MagicKey;
import org.infinispan.partitionhandling.AvailabilityMode;
import org.infinispan.partitionhandling.BasePartitionHandlingTest;
import org.infinispan.partitionhandling.impl.PartitionHandlingManager;
import org.infinispan.remoting.transport.Address;
import org.infinispan.test.TestingUtil;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;
import org.testng.annotations.BeforeMethod;

/**
 * @author Ryan Emerson
 * @since 9.0
 */
@org.testng.annotations.Test(groups = "functional", testName = "conflict.resolution.MergeInconsistencyTest")
public class MergeInconsistencyTest extends BasePartitionHandlingTest {

   private static final String CACHE_NAME = "conflict-cache";

   public MergeInconsistencyTest() {
      this.cacheMode = CacheMode.DIST_SYNC;
      this.partitionHandling = false;
   }

   @Override
   protected void createCacheManagers() throws Throwable {
      super.createCacheManagers();
      ConfigurationBuilder builder = getDefaultClusteredCacheConfig(CacheMode.DIST_SYNC);
      builder.clustering().partitionHandling().enabled(false).stateTransfer().fetchInMemoryState(true);
      builder.clustering().l1().disable();
      defineConfigurationOnAllManagers(CACHE_NAME, builder);
   }

   public void doTest() {
      final String key = "Key";
      final String value = "Value";

      waitForClusterToForm(CACHE_NAME);
      List<Address> members = advancedCache(0).getRpcManager().getMembers();

      TestingUtil.waitForRehashToComplete(caches());
      assertTrue(members.size() == 4);

      splitCluster(new int[]{0, 1}, new int[]{2, 3});
      TestingUtil.blockUntilViewsChanged(10000, 2, cache(0), cache(1), cache(2), cache(3));

      getCache(2).put(key, value);
      System.out.println("Pre-merge cache values");
      IntStream.range(0, 4).forEach(i -> System.out.println(String.format("Cache %s | Key:= %s", i, getCache(i).get(key))));

      mergeCluster();
      TestingUtil.blockUntilViewsChanged(10000, 4, cache(0), cache(1), cache(2), cache(3));
      System.out.println("Post-merge cache values");
      IntStream.range(0, 4).forEach(i -> System.out.println(String.format("Cache %s | Key:= %s", i, getCache(i).get(key))));
   }

   private void mergeCluster() {
      partition(0).merge(partition(1));
      waitForRehashToComplete(caches(CACHE_NAME));
      for (int i = 0; i < numMembersInCluster; i++) {
         PartitionHandlingManager phmI = partitionHandlingManager(getCache(i));
         eventuallyEquals(AvailabilityMode.AVAILABLE, phmI::getAvailabilityMode);
      }
   }

   private AdvancedCache<Object, Object> getCache(int index) {
      return advancedCache(index, CACHE_NAME);
   }

}
