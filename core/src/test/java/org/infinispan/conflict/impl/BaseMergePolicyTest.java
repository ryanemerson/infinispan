package org.infinispan.conflict.impl;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.infinispan.AdvancedCache;
import org.infinispan.conflict.ConflictManager;
import org.infinispan.conflict.ConflictManagerFactory;
import org.infinispan.container.entries.InternalCacheValue;
import org.infinispan.partitionhandling.BasePartitionHandlingTest;
import org.infinispan.partitionhandling.PartitionHandling;
import org.infinispan.partitionhandling.impl.PreferAvailabilityStrategy;
import org.infinispan.remoting.transport.Address;
import org.infinispan.test.TestingUtil;
import org.infinispan.topology.CacheStatusResponse;
import org.infinispan.topology.CacheTopology;
import org.infinispan.topology.LocalTopologyManager;
import org.infinispan.topology.ManagerStatusResponse;

public abstract class BaseMergePolicyTest extends BasePartitionHandlingTest {

   BaseMergePolicyTest() {
      this.partitionHandling = PartitionHandling.ALLOW_READ_WRITES;
   }

   abstract void beforeSplit();
   abstract void duringSplit();
   abstract void afterMerge();

   public void testPartitionMergePolicy() throws Throwable {
      final List<ViewChangedHandler> listeners = new ArrayList<>();
      for (int i = 0; i < caches().size(); i++) {
         ViewChangedHandler listener = new ViewChangedHandler();
         cache(i).getCacheManager().addListener(listener);
         listeners.add(listener);
      }

      beforeSplit();
      splitCluster(new int[]{0, 1}, new int[]{2, 3});

      eventually(() -> listeners.stream().allMatch(ViewChangedHandler::isNotified));
      eventuallyEquals(2, () -> advancedCache(0).getRpcManager().getTransport().getMembers().size());
      eventually(() -> clusterAndChFormed(0, 2));
      eventually(() -> clusterAndChFormed(1, 2));
      eventually(() -> clusterAndChFormed(2, 2));
      eventually(() -> clusterAndChFormed(3, 2));

      duringSplit();

      partition(0).merge(partition(1));
      assertTrue(clusterAndChFormed(0, 4));
      assertTrue(clusterAndChFormed(1, 4));
      assertTrue(clusterAndChFormed(2, 4));
      assertTrue(clusterAndChFormed(3, 4));
      TestingUtil.waitForNoRebalance(caches());

      afterMerge();
   }

   protected <A, B> AdvancedCache<A, B> getCacheFromNonPreferredPartition(AdvancedCache... caches) {
      AdvancedCache<A, B> preferredCache = getCacheFromPreferredPartition(caches);
      List<AdvancedCache> cacheList = new ArrayList<>(Arrays.asList(caches));
      cacheList.remove(preferredCache);
      return cacheList.get(0);
   }

   protected <A, B> AdvancedCache<A, B> getCacheFromPreferredPartition(AdvancedCache... caches) {
      List<CacheStatusResponse> statusResponses = Arrays.stream(caches)
            .map(this::getCacheStatus)
            .flatMap(Collection::stream)
            .sorted(PreferAvailabilityStrategy.RESPONSE_COMPARATOR)
            .collect(Collectors.toList());

      CacheTopology maxStableTopology = null;
      for (CacheStatusResponse response : statusResponses) {
         CacheTopology stableTopology = response.getStableTopology();
         if (stableTopology == null) continue;

         if (maxStableTopology == null || maxStableTopology.getMembers().size() < stableTopology.getMembers().size()) {
            maxStableTopology = stableTopology;
         }
      }

      int cacheIndex = -1;
      int count = -1;
      CacheTopology maxTopology = null;
      for (CacheStatusResponse response : statusResponses) {
         count++;
         CacheTopology stableTopology = response.getStableTopology();
         if (!Objects.equals(stableTopology, maxStableTopology)) continue;

         CacheTopology topology = response.getCacheTopology();
         if (topology == null) continue;

         if (maxTopology == null || maxTopology.getMembers().size() < topology.getMembers().size()) {
            maxTopology = topology;
            cacheIndex = count;
         }
      }
      return caches[cacheIndex];
   }

   private Collection<CacheStatusResponse> getCacheStatus(AdvancedCache cache) {
      LocalTopologyManager localTopologyManager = cache.getComponentRegistry().getComponent(LocalTopologyManager.class);
      int viewId = cache.getRpcManager().getTransport().getViewId();
      ManagerStatusResponse statusResponse = localTopologyManager.handleStatusRequest(viewId);
      return statusResponse.getCaches().values();
   }

   protected boolean clusterAndChFormed(int cacheIndex, int memberCount) {
      return advancedCache(cacheIndex).getRpcManager().getTransport().getMembers().size() == memberCount &&
            advancedCache(cacheIndex).getDistributionManager().getWriteConsistentHash().getMembers().size() == memberCount;
   }

   protected ConflictManager conflictManager(int index) {
      return ConflictManagerFactory.get(advancedCache(index));
   }

   protected void assertSameVersionAndNoConflicts(int cacheIndex, int numberOfVersions, Object key, Object expectedValue) {
      ConflictManager cm = conflictManager(cacheIndex);
      assert !cm.isConflictResolutionInProgress();
      Map<Address, InternalCacheValue> versionMap = cm.getAllVersions(key);
      assertNotNull(versionMap);
      assertEquals("Versions: " + versionMap, numberOfVersions, versionMap.size());
      for (InternalCacheValue icv : versionMap.values()) {
         if (expectedValue != null) {
            assertNotNull(icv);
            assertNotNull(icv.getValue());
         }
         assertEquals(expectedValue, icv.getValue());
      }
      assertEquals(0, cm.getConflicts().count());
   }
}
