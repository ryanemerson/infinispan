package org.infinispan.conflict.impl;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.infinispan.conflict.ConflictManager;
import org.infinispan.conflict.ConflictManagerFactory;
import org.infinispan.conflict.EntryMergePolicy;
import org.infinispan.conflict.MergePolicies;
import org.infinispan.container.entries.InternalCacheValue;
import org.infinispan.partitionhandling.BasePartitionHandlingTest;
import org.infinispan.partitionhandling.PartitionHandling;

public abstract class BaseMergePolicyTest extends BasePartitionHandlingTest {


   BaseMergePolicyTest() {
      this.partitionHandling = PartitionHandling.ALLOW_ALL;
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

      eventually(() -> {
         for (ViewChangedHandler l : listeners)
            if (!l.isNotified()) return false;
         return true;
      });

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

      afterMerge();
   }

   protected boolean clusterAndChFormed(int cacheIndex, int memberCount) {
      return advancedCache(cacheIndex).getRpcManager().getTransport().getMembers().size() == memberCount &&
            advancedCache(cacheIndex).getDistributionManager().getWriteConsistentHash().getMembers().size() == memberCount;
   }

   protected ConflictManager conflictManager(int index) {
      return ConflictManagerFactory.get(advancedCache(index));
   }

   protected void assertSameVersion(Collection<InternalCacheValue> versions, int expectedSize, Object expectedValue) {
      assertNotNull(versions);
      assertEquals(expectedSize, versions.size());
      versions.stream().map(InternalCacheValue::getValue).forEach(v -> assertEquals(expectedValue, v));
   }
}
