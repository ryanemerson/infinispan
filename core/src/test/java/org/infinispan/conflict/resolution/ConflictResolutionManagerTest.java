package org.infinispan.conflict.resolution;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.container.entries.InternalCacheValue;
import org.infinispan.context.Flag;
import org.infinispan.distribution.ch.ConsistentHash;
import org.infinispan.remoting.rpc.RpcManager;
import org.infinispan.remoting.transport.Address;
import org.infinispan.statetransfer.StateConsumer;
import org.infinispan.statetransfer.StateRequestCommand;
import org.infinispan.test.MultipleCacheManagersTest;
import org.infinispan.test.TestingUtil;
import org.infinispan.topology.CacheTopology;
import org.infinispan.tx.dld.ControlledRpcManager;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author Ryan Emerson
 * @since 9.0
 */
@Test(groups = "functional", testName = "conflict.resolution.ConflictResolutionManagerTest")
public class ConflictResolutionManagerTest extends MultipleCacheManagersTest {

   private static final int NUMBER_OF_OWNERS = 2;
   private static final int NUMBER_OF_NODES = NUMBER_OF_OWNERS + 2;
   private static final int NUMBER_OF_CACHE_ENTRIES = 100;
   private static final int INCONSISTENT_VALUE_INCREMENT = 10;
   private static final int NULL_VALUE_FREQUENCY = 20;

   @Override
   protected void createCacheManagers() throws Throwable {
      createClusteredCaches(NUMBER_OF_NODES, getConfigurationBuilder());
   }

   private ConfigurationBuilder getConfigurationBuilder() {
      ConfigurationBuilder cacheConfigBuilder = getDefaultClusteredCacheConfig(CacheMode.DIST_SYNC);
      cacheConfigBuilder.clustering()
            .hash().numOwners(NUMBER_OF_OWNERS)
            .stateTransfer().fetchInMemoryState(true);
      return cacheConfigBuilder;
   }

   @BeforeMethod
   public void populateCaches() {
      AdvancedCache<Integer, String> cache0 = advancedCache(0);
      IntStream.range(0, NUMBER_OF_CACHE_ENTRIES).forEach(i -> cache0.put(i, "v" + i));
   }

   @Test(expectedExceptions = IllegalStateException.class,
         expectedExceptionsMessageRegExp = ".* Unable to retrieve conflicts for key .*")
   public void testGetAllVersionsDuringStateTransfer() {
      AdvancedCache cache0 = advancedCache(0);
      StateConsumer sc = TestingUtil.extractComponent(cache0, StateConsumer.class);

      sc = spy(sc);
      doReturn(true).when(sc).isStateTransferInProgressForKey(any());
      TestingUtil.replaceComponent(cache0, StateConsumer.class, sc, true);
      cache0.getConflictResolutionManager().getAllVersions("testKey");
   }

   @Test(expectedExceptions = IllegalStateException.class,
   expectedExceptionsMessageRegExp = ".* Unable to retrieve conflicts as StateTransfer is currently in progress for this cache")
   public void testGetConflictsDuringStateTransfer() {
      assertClusterSize("Wrong number of caches.", NUMBER_OF_NODES);

      AdvancedCache cache0 = advancedCache(0);
      ControlledRpcManager controlledRpcManager = new ControlledRpcManager(TestingUtil.extractComponent(cache0, RpcManager.class));
      TestingUtil.replaceComponent(cache0, RpcManager.class, controlledRpcManager, true);
      controlledRpcManager.blockAfter(StateRequestCommand.class);

      StateConsumer sc = TestingUtil.extractComponent(cache0, StateConsumer.class);
      CacheTopology ct = sc.getCacheTopology();
      CacheTopology newTopology = new CacheTopology(ct.getTopologyId() + 1, ct.getRebalanceId() + 1, ct.getCurrentCH(), null, ct.getMembers(), ct.getMembersPersistentUUIDs());
      sc.onTopologyUpdate(newTopology, true);

      assertTrue(cache0.getDistributionManager().isRehashInProgress());
      cache0.getConflictResolutionManager().getConflicts();
   }

   public void testAllVersionsOfKeyReturned() {
      // Test with and without conflicts
      compareCacheValuesForKey(INCONSISTENT_VALUE_INCREMENT, true);
      compareCacheValuesForKey(NULL_VALUE_FREQUENCY, true);
      introduceCacheConflicts();
      compareCacheValuesForKey(INCONSISTENT_VALUE_INCREMENT, false);
      compareCacheValuesForKey(NULL_VALUE_FREQUENCY, false);
   }

   public void testConflictsDetected() {
      // Test that no conflicts are detected at the start
      // Deliberately introduce conflicts and make sure they are detected
      final int cacheIndex = NUMBER_OF_NODES - 1;
      assertEquals(getConflicts(advancedCache(cacheIndex)).count(), 0);
      introduceCacheConflicts();
      List<Map<Address, InternalCacheEntry<Object, Object>>> conflicts = getConflicts(advancedCache(cacheIndex)).collect(Collectors.toList());

      assertEquals(conflicts.size(), (NUMBER_OF_CACHE_ENTRIES / NULL_VALUE_FREQUENCY), "The number of conflicts returned is not equal to those in the cache");
      for (Map<Address, InternalCacheEntry<Object, Object>> map : conflicts) {
         assertEquals(map.keySet().size(), NUMBER_OF_OWNERS);
         List<Object> values = map.values().stream().map(InternalCacheEntry::getValue).distinct().collect(Collectors.toList());
         assertEquals(values.size(), NUMBER_OF_OWNERS);
         assertTrue(values.contains("INCONSISTENT"), "Expected one of the conflicting string values to be 'INCONSISTENT'");
      }
   }

   private void introduceCacheConflicts() {
      ConsistentHash hash = advancedCache(0).getDistributionManager().getConsistentHash();
      for (int i = 0; i < NUMBER_OF_CACHE_ENTRIES; i += INCONSISTENT_VALUE_INCREMENT) {
         Address primary = hash.locatePrimaryOwner(i);
         AdvancedCache<Object, Object> primaryCache = manager(primary).getCache().getAdvancedCache().withFlags(Flag.CACHE_MODE_LOCAL);

         if (i % NULL_VALUE_FREQUENCY == 0)
            primaryCache.remove(i);
         else
            primaryCache.put(i, "INCONSISTENT");
      }
   }

   private void compareCacheValuesForKey(int key, boolean expectEquality) {
      List<Map<Address, InternalCacheValue<Object>>> cacheVersions = new ArrayList<>();
      for (int i = 0; i < NUMBER_OF_NODES; i++)
         cacheVersions.add(getAllVersions(advancedCache(i), key));

      for (Map<Address, InternalCacheValue<Object>> map : cacheVersions) {
         assertEquals(map.keySet().size(), NUMBER_OF_OWNERS);

         if (map.values().contains(null)) {
            if (expectEquality) {
               fail("Inconsistent values returned, they should be the same");
            } else {
               assertTrue(key % NULL_VALUE_FREQUENCY == 0, String.format("Null CacheValue encountered unexpectedly for key '%s'", key));
               return;
            }
         }

         List<Object> values = map.values().stream()
               .map(InternalCacheValue::getValue)
               .collect(Collectors.toList());
         assertEquals(values.size(), NUMBER_OF_OWNERS);

         if (expectEquality) {
            assertTrue(values.stream().allMatch(v -> v.equals(values.get(0))), "Inconsistent values returned, they should be the same");
         } else {
            assertTrue(values.stream().distinct().count() > 1, "Expected inconsistent values, but all values were equal");
         }
      }

      List<Object> uniqueValues = cacheVersions.stream()
            .map(Map::values)
            .flatMap(allValues -> allValues.stream()
                  .map(InternalCacheValue::getValue))
            .distinct()
            .collect(Collectors.toList());

      if (expectEquality) {
         assertEquals(uniqueValues.size(), 1, "More than one version of Key/Value returned. Expected all versions to be equal.");
      } else {
         assertTrue(uniqueValues.size() > 1, "Only one version of Key/Value returned. Expected inconsistent values to be returned.");
      }
   }

   private Stream<Map<Address, InternalCacheEntry<Object, Object>>> getConflicts(Cache<Object, Object> cache) {
      return cache.getAdvancedCache().getConflictResolutionManager().getConflicts();
   }

   private Map<Address, InternalCacheValue<Object>> getAllVersions(Cache<Object, Object> cache, Object key) {
      return cache.getAdvancedCache().getConflictResolutionManager().getAllVersions(key);
   }
}
