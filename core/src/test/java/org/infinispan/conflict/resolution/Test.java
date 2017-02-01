package org.infinispan.conflict.resolution;

import static org.infinispan.test.TestingUtil.blockUntilViewReceived;
import static org.infinispan.test.TestingUtil.waitForRehashToComplete;
import static org.testng.AssertJUnit.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.infinispan.AdvancedCache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.context.Flag;
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
@org.testng.annotations.Test(groups = "functional", testName = "conflict.resolution.Test")
public class Test extends BasePartitionHandlingTest {

   private static final Log log = LogFactory.getLog(Test.class);
   private static final String CACHE_NAME = "conflict-cache";
   private static final String VALUE = "value";
   private static final String INCONSISTENT_VALUE = "inconsistent-value";
   private static final int NUMBER_OF_CACHE_ENTRIES = 100;
   private static final int INCONSISTENT_VALUE_INCREMENT = 5;
   private static final int NULL_VALUE_FREQUENCY = 10;

   private MagicKey[] keys = new MagicKey[NUMBER_OF_CACHE_ENTRIES];

   public Test() {
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

   @BeforeMethod
   public void populateCaches() {
      waitForClusterToForm(CACHE_NAME);
//      int half = NUMBER_OF_CACHE_ENTRIES / 2;
//      for (int i = 0; i < NUMBER_OF_CACHE_ENTRIES; i++) {
////         MagicKey key = (i < half) ? new MagicKey("k" + i, getCache(1), getCache(2)) : new MagicKey("k" + i, getCache(2), getCache(1));
//         MagicKey key = new MagicKey("k + i", getCache(1), getCache(2));
//         keys[i] = key;
//         getCache(1).put(key, VALUE);
//      }
//      log.info(CACHE_NAME + " populated");
   }

   public void testGetConflictsDuringStateTransfer() {
      doTest();
   }

   private void doTest() {
//      TestingUtil.blockUntilViewsChanged(10000, 4, cache(0), cache(1), cache(2), cache(3));
      List<Address> members = advancedCache(0).getRpcManager().getMembers();
      log.info("START!!! " + members);
      TestingUtil.waitForRehashToComplete(caches());
      assertTrue(members.size() == 4);
      printAllCacheDetails(true);

      log.info("PRE-SPLIT");
      splitCluster(new int[]{0, 1}, new int[]{2, 3});
      TestingUtil.blockUntilViewsChanged(10000, 2, cache(0), cache(1), cache(2), cache(3));
      log.info("POST-SPLIT********************************");

      createInconsistenciesInPartition1();
      printAllCacheDetails(true);

      log.info("PRE-MERGE%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
      mergeCluster();
      TestingUtil.blockUntilViewsChanged(10000, 4, cache(0), cache(1), cache(2), cache(3));
      log.info("POST-MERGE££££££££££££££££££££££££££££££££££££");
      printAllCacheDetails(true);
   }

   private void printAllCacheDetails(boolean withEntries) {
      IntStream.range(0, 4).forEach(i -> printCacheDetails(i, withEntries));
   }

   private void printCacheDetails(int index, boolean withEntries) {
      DistributionManager dm = getCache(index).getDistributionManager();
      StringBuilder sb = new StringBuilder();
      sb.append("Cache Details at Index " + index + "\n");
      sb.append("Rehash in progress? " + dm.isRehashInProgress() + "\n");
      sb.append("ReadHash: " + dm.getReadConsistentHash().getMembers() + "\n");
      sb.append("Write Hash: " + dm.getWriteConsistentHash().getMembers() + "\n");

      if (withEntries) {
//         List<Map<Address, InternalCacheEntry<Object, Object>>> conflicts = getCache(index).getConflictResolutionManager()
//               .getConflicts()
//               .collect(Collectors.toList());
//         sb.append("CONFLICTS>>>>>>" + "\n");
//         if (!conflicts.isEmpty())
//            sb.append(conflicts.get(0));
         sb.append("ENTRIES>>>>>>" + "\n");
//         sb.append("Cache" + index + " keys[5]: " + getCache(index).withFlags(Flag.CACHE_MODE_LOCAL).get(keys[5]) + "\n");
//         sb.append("Cache" + index + " keys[10]: " + getCache(index).withFlags(Flag.CACHE_MODE_LOCAL).get(keys[10]) + "\n");
//         sb.append("Cache" + index + " keys[5] (NO FLAGS): " + getCache(index).get(keys[5]));
//         sb.append("Cache" + index + " keys[10] (NO FLAGS): " + getCache(index).get(keys[10]));
         sb.append("Test Value: " + getCache(index).get("Test") + "\n");
      }
      sb.append("----------------------");
      log.info(sb.toString());
   }

   private void mergeCluster() {
      log.debugf("Merging cluster");
      partition(0).merge(partition(1));
//      partition(1).merge(partition(0));
      waitForRehashToComplete(caches(CACHE_NAME));
      for (int i = 0; i < numMembersInCluster; i++) {
         PartitionHandlingManager phmI = partitionHandlingManager(getCache(i));
         eventuallyEquals(AvailabilityMode.AVAILABLE, phmI::getAvailabilityMode);
      }
      log.debugf("Cluster merged");
   }

   private void createInconsistenciesInPartition1() {
      int cacheIndex = 1;
      System.out.println("Create inconsistencies");
      System.out.println("Members of cache 1: "  + getCache(cacheIndex).getRpcManager().getMembers());
//      getCache(cacheIndex).put(keys[5], INCONSISTENT_VALUE);
//      getCache(cacheIndex + 1).put(keys[5], INCONSISTENT_VALUE + 1);
      getCache(cacheIndex + 1).put("Test", "Test");
//      getCache(cacheIndex).withFlags(Flag.CACHE_MODE_LOCAL).put(keys[5], INCONSISTENT_VALUE);
//      getCache(cacheIndex).withFlags(Flag.CACHE_MODE_LOCAL).remove(keys[10]);
//      for (int i = 0; i < (NUMBER_OF_CACHE_ENTRIES / 2); i++) {
//         MagicKey key = keys[i];
//         if (i % NULL_VALUE_FREQUENCY == 0)
//            getCache(cacheIndex).remove(key);
//         else if (i % INCONSISTENT_VALUE_INCREMENT == 0)
//            getCache(cacheIndex).put(key, INCONSISTENT_VALUE);
//      }
   }

   private AdvancedCache<Object, Object> getCache(int index) {
      return advancedCache(index, CACHE_NAME);
   }

}
