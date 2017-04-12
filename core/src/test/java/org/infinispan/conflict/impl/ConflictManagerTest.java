package org.infinispan.conflict.impl;

import static org.infinispan.test.TestingUtil.wrapPerCacheInboundInvocationHandler;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotSame;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.infinispan.AdvancedCache;
import org.infinispan.commands.remote.CacheRpcCommand;
import org.infinispan.commands.remote.ClusteredGetCommand;
import org.infinispan.commons.CacheException;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.conflict.ConflictManagerFactory;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.container.entries.InternalCacheValue;
import org.infinispan.context.Flag;
import org.infinispan.distribution.ch.ConsistentHash;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.DataRehashed;
import org.infinispan.notifications.cachelistener.event.DataRehashedEvent;
import org.infinispan.partitionhandling.BasePartitionHandlingTest;
import org.infinispan.partitionhandling.PartitionHandling;
import org.infinispan.remoting.inboundhandler.DeliverOrder;
import org.infinispan.remoting.inboundhandler.PerCacheInboundInvocationHandler;
import org.infinispan.remoting.inboundhandler.Reply;
import org.infinispan.remoting.transport.Address;
import org.infinispan.statetransfer.StateResponseCommand;
import org.infinispan.test.TestingUtil;
import org.testng.annotations.Test;

@Test(groups = "functional", testName = "conflict.resolution.ConflictManagerTest")
public class ConflictManagerTest extends BasePartitionHandlingTest {

   private static final String CACHE_NAME = "conflict-cache";
   private static final int NUMBER_OF_OWNERS = 2;
   private static final int NUMBER_OF_CACHE_ENTRIES = 100;
   private static final int INCONSISTENT_VALUE_INCREMENT = 10;
   private static final int NULL_VALUE_FREQUENCY = 20;
   private static final int STATE_TRANSFER_DELAY = 4000;

   public ConflictManagerTest() {
      this.cacheMode = CacheMode.DIST_SYNC;
      this.partitionHandling = PartitionHandling.ALLOW_ALL;
   }

   @Override
   protected void createCacheManagers() throws Throwable {
      super.createCacheManagers();
      ConfigurationBuilder builder = getDefaultClusteredCacheConfig(CacheMode.DIST_SYNC);
      builder.clustering().stateTransfer().fetchInMemoryState(true);
      defineConfigurationOnAllManagers(CACHE_NAME, builder);
   }

   public void testGetAllVersionsDuringStateTransfer() throws Exception {
      final int key = 1;
      final int value = 1;
      createCluster();
      getCache(2).put(key, value);
      splitCluster();

      delayStateTransferCompletion();
      RehashListener listener = new RehashListener();
      getCache(2).addListener(listener);

      Future<Map<Address, InternalCacheValue<Object>>> versionFuture = fork(() -> {
         while (!listener.isRehashInProgress.get())
            TestingUtil.sleepThread(100);

         return getAllVersions(0, key);
      });
      fork(() -> partition(0).merge(partition(1)));

      Map<Address, InternalCacheValue<Object>> versionMap = versionFuture.get(STATE_TRANSFER_DELAY * 4, TimeUnit.MILLISECONDS);
      assertTrue(versionMap != null);
      assertTrue(!versionMap.isEmpty());
      assertEquals(versionMap.size(), 2);
      versionMap.values().forEach(icv -> assertEquals(icv.getValue(), value));
   }

   @Test(expectedExceptions = CacheException.class,
         expectedExceptionsMessageRegExp = ".* encountered when trying to receive all versions .*")
   public void testGetAllVersionsTimeout() throws Throwable {
      ConfigurationBuilder builder = getDefaultClusteredCacheConfig(CacheMode.DIST_SYNC);
      builder.clustering().remoteTimeout(5000).stateTransfer().fetchInMemoryState(true);
      String cacheName = CACHE_NAME + "2";
      defineConfigurationOnAllManagers(cacheName, builder);
      waitForClusterToForm(cacheName);
      dropClusteredGetCommands();
      getAllVersions(0, "Test");
   }

   @Test(expectedExceptions = IllegalStateException.class,
         expectedExceptionsMessageRegExp = ".* Unable to retrieve conflicts as StateTransfer is currently in progress for cache .*")
   public void testGetConflictsDuringStateTransfer() throws Throwable {
      createAndSplitCluster();
      delayStateTransferCompletion();
      RehashListener listener = new RehashListener();
      getCache(2).addListener(listener);

      Future<Void> conflictsFuture = fork(() -> {
         while (!listener.isRehashInProgress.get())
            TestingUtil.sleepThread(100);

         getConflicts(0);
         return null;
      });
      fork(() -> partition(0).merge(partition(1)));
      try {
         conflictsFuture.get();
      } catch (ExecutionException e) {
         throw e.getCause();
      }
   }

   public void testAllVersionsOfKeyReturned() {
      // Test with and without conflicts
      waitForClusterToForm(CACHE_NAME);
      IntStream.range(0, NUMBER_OF_CACHE_ENTRIES).forEach(i -> getCache(0).put(i, "v" + i));
      compareCacheValuesForKey(INCONSISTENT_VALUE_INCREMENT, true);
      compareCacheValuesForKey(NULL_VALUE_FREQUENCY, true);
      introduceCacheConflicts();
      compareCacheValuesForKey(INCONSISTENT_VALUE_INCREMENT, false);
      compareCacheValuesForKey(NULL_VALUE_FREQUENCY, false);
   }

   public void testConsecutiveInvocationOfAllVersionsForKey() throws Exception {
      waitForClusterToForm(CACHE_NAME);
      int key = 1;
      Map<Address, InternalCacheValue<Object>> result1 = getAllVersions(0, key);
      Map<Address, InternalCacheValue<Object>> result2 = getAllVersions(0, key);
      assertNotSame(result1, result2); // Assert that a different map is returned, i.e. a new CompletableFuture was created
      assertEquals(result1, result2); // Assert that returned values are still logically equivalent
   }

   public void testConflictsDetected() {
      // Test that no conflicts are detected at the start
      // Deliberately introduce conflicts and make sure they are detected
      waitForClusterToForm(CACHE_NAME);
      IntStream.range(0, NUMBER_OF_CACHE_ENTRIES).forEach(i -> getCache(0).put(i, "v" + i));
      final int cacheIndex = numMembersInCluster - 1;
      assertEquals(getConflicts(cacheIndex).count(), 0);
      introduceCacheConflicts();
      List<Map<Address, InternalCacheEntry<Object, Object>>> conflicts = getConflicts(cacheIndex).collect(Collectors.toList());

      assertEquals(conflicts.size(), (NUMBER_OF_CACHE_ENTRIES / NULL_VALUE_FREQUENCY));
      for (Map<Address, InternalCacheEntry<Object, Object>> map : conflicts) {
         assertEquals(map.keySet().size(), NUMBER_OF_OWNERS);
         List<Object> values = map.values().stream().map(InternalCacheEntry::getValue).distinct().collect(Collectors.toList());
         assertEquals(values.size(), NUMBER_OF_OWNERS);
         assertTrue("Expected one of the conflicting string values to be 'INCONSISTENT'", values.contains("INCONSISTENT"));
      }
   }

   private void introduceCacheConflicts() {
      ConsistentHash hash = getCache(0).getDistributionManager().getConsistentHash();
      for (int i = 0; i < NUMBER_OF_CACHE_ENTRIES; i += INCONSISTENT_VALUE_INCREMENT) {
         Address primary = hash.locatePrimaryOwner(i);
         AdvancedCache<Object, Object> primaryCache = manager(primary).getCache(CACHE_NAME).getAdvancedCache().withFlags(Flag.CACHE_MODE_LOCAL);

         if (i % NULL_VALUE_FREQUENCY == 0)
            primaryCache.remove(i);
         else
            primaryCache.put(i, "INCONSISTENT");
      }
   }

   private void compareCacheValuesForKey(int key, boolean expectEquality) {
      List<Map<Address, InternalCacheValue<Object>>> cacheVersions = new ArrayList<>();
      for (int i = 0; i < numMembersInCluster; i++)
         cacheVersions.add(getAllVersions(i, key));

      for (Map<Address, InternalCacheValue<Object>> map : cacheVersions) {
         assertEquals(map.keySet().size(), NUMBER_OF_OWNERS);

         if (map.values().contains(null)) {
            if (expectEquality) {
               fail("Inconsistent values returned, they should be the same");
            } else {
               assertTrue(String.format("Null CacheValue encountered unexpectedly for key '%s'", key), key % NULL_VALUE_FREQUENCY == 0);
               return;
            }
         }

         List<Object> values = map.values().stream()
               .map(InternalCacheValue::getValue)
               .collect(Collectors.toList());
         assertEquals(values.size(), NUMBER_OF_OWNERS);

         if (expectEquality) {
            assertTrue("Inconsistent values returned, they should be the same", values.stream().allMatch(v -> v.equals(values.get(0))));
         } else {
            assertTrue("Expected inconsistent values, but all values were equal", values.stream().distinct().count() > 1);
         }
      }

      List<Object> uniqueValues = cacheVersions.stream()
            .map(Map::values)
            .flatMap(allValues -> allValues.stream()
                  .map(InternalCacheValue::getValue))
            .distinct()
            .collect(Collectors.toList());

      if (expectEquality) {
         assertEquals(uniqueValues.size(), 1);
      } else {
         assertTrue("Only one version of Key/Value returned. Expected inconsistent values to be returned.", uniqueValues.size() > 1);
      }
   }

   private void createCluster() {
      waitForClusterToForm(CACHE_NAME);
      List<Address> members = getCache(0).getRpcManager().getMembers();

      TestingUtil.waitForRehashToComplete(caches());
      assertTrue(members.size() == 4);
   }

   private void splitCluster() {
      splitCluster(new int[]{0, 1}, new int[]{2, 3});
      TestingUtil.blockUntilViewsChanged(10000, 2, getCache(0), getCache(1), getCache(2), getCache(3));
   }

   private void createAndSplitCluster() {
      createCluster();
      splitCluster();
   }

   private AdvancedCache<Object, Object> getCache(int index) {
      return advancedCache(index, CACHE_NAME);
   }

   private Stream<Map<Address, InternalCacheEntry<Object, Object>>> getConflicts(int index) {
      return ConflictManagerFactory.get(getCache(index)).getConflicts();
   }

   private Map<Address, InternalCacheValue<Object>> getAllVersions(int index, Object key) {
      return ConflictManagerFactory.get(getCache(index)).getAllVersions(key);
   }

   private void dropClusteredGetCommands() {
      IntStream.range(0, numMembersInCluster).forEach(i ->
            wrapPerCacheInboundInvocationHandler(getCache(i), (wrapOn, current) -> new DropClusteredGetCommandHandler(current), true));
   }

   private void delayStateTransferCompletion() {
      IntStream.range(0, numMembersInCluster).forEach(i ->
            wrapPerCacheInboundInvocationHandler(getCache(i), (wrapOn, current) -> new DelayStateRequestCommandHandler(current), true));
   }

   private class DelayStateRequestCommandHandler implements PerCacheInboundInvocationHandler {
      final PerCacheInboundInvocationHandler delegate;

      DelayStateRequestCommandHandler(PerCacheInboundInvocationHandler delegate) {
         this.delegate = delegate;
      }

      @Override
      public void handle(CacheRpcCommand command, Reply reply, DeliverOrder order) {
         if (command instanceof StateResponseCommand) {
            TestingUtil.sleepThread(STATE_TRANSFER_DELAY);
         }
         delegate.handle(command, reply, order);
      }
   }

   private class DropClusteredGetCommandHandler implements PerCacheInboundInvocationHandler {
      final PerCacheInboundInvocationHandler delegate;

      DropClusteredGetCommandHandler(PerCacheInboundInvocationHandler delegate) {
         this.delegate = delegate;
      }

      @Override
      public void handle(CacheRpcCommand command, Reply reply, DeliverOrder order) {
         if (!(command instanceof ClusteredGetCommand)) {
            delegate.handle(command, reply, order);
         }
      }
   }

   @Listener
   private class RehashListener {
      final AtomicBoolean isRehashInProgress = new AtomicBoolean();

      @DataRehashed
      @SuppressWarnings("unused")
      public void onDataRehashed(DataRehashedEvent event) {
         isRehashInProgress.set(event.isPre());
      }
   }
}
