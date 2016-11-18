package org.infinispan.conflict.resolution;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.container.entries.InternalCacheValue;
import org.infinispan.context.Flag;
import org.infinispan.distribution.ch.ConsistentHash;
import org.infinispan.remoting.transport.Address;
import org.infinispan.test.MultipleCacheManagersTest;
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
   private ConfigurationBuilder cacheConfigBuilder;

   @Override
   protected void createCacheManagers() throws Throwable {
      cacheConfigBuilder = getDefaultClusteredCacheConfig(CacheMode.DIST_SYNC);
      cacheConfigBuilder.clustering().hash().numOwners(NUMBER_OF_OWNERS);
      createCluster(cacheConfigBuilder, NUMBER_OF_NODES);
      waitForClusterToForm();
   }

   @BeforeMethod
   public void populateCaches() {
      AdvancedCache<Object, Object> cache0 = advancedCache(0);
      IntStream.range(0, NUMBER_OF_CACHE_ENTRIES).forEach(i -> cache0.put("k" + i, "v" + i));
   }

   public void testAllVersionsOfKeyReturned() {
      // Test with and without conflicts
      compareCacheValuesForKey("k10", true);
      introduceCacheConflicts();
      compareCacheValuesForKey("k10", false);
   }

   public void testConflictsDetected() {
      // Test that no conflicts are detected at the start
      // Deliberately introduce conflicts and make sure they are detected
      final int cacheIndex = NUMBER_OF_NODES - 1;
      assertEquals(getConflicts(advancedCache(cacheIndex)).count(), 0);
      introduceCacheConflicts();
      List<Map<Address, InternalCacheValue<Object>>> conflicts = getConflicts(advancedCache(cacheIndex)).collect(Collectors.toList());

      assertEquals(conflicts.size(), (NUMBER_OF_CACHE_ENTRIES / 10), "The number of conflicts returned is not equal to those in the cache");
      for (Map<Address, InternalCacheValue<Object>> map : conflicts) {
         assertEquals(map.keySet().size(), NUMBER_OF_OWNERS);
         List<Object> values = map.values().stream().map(InternalCacheValue::getValue).distinct().collect(Collectors.toList());
         assertEquals(values.size(), NUMBER_OF_OWNERS);
         assertTrue(values.contains("INCONSISTENT"), "Expected one of the conflicting string values to be 'INCONSISTENT'");
      }
   }

   private void introduceCacheConflicts() {
      ConsistentHash hash = advancedCache(0).getDistributionManager().getConsistentHash();
      for (int i = 0; i < NUMBER_OF_CACHE_ENTRIES; i += 10) {
         String key = "k" + i;
         Address primary = hash.locatePrimaryOwner(key);
         AdvancedCache<Object, Object> primaryCache = manager(primary).getCache().getAdvancedCache();
         primaryCache.withFlags(Flag.CACHE_MODE_LOCAL).put(key, "INCONSISTENT");
      }
   }

   private void compareCacheValuesForKey(Object key, boolean expectEquality) {
      List<Map<Address, InternalCacheValue<Object>>> cacheVersions = new ArrayList<>();
      for (int i = 0; i < NUMBER_OF_NODES; i++) {
         cacheVersions.add(getAllVersions(advancedCache(i), key));
      }

      for (Map<Address, InternalCacheValue<Object>> map : cacheVersions) {
         checkReturnedVersions(map, expectEquality);
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

   private void checkReturnedVersions(Map<Address, InternalCacheValue<Object>> map, boolean checkEquality) {
      assertEquals(map.keySet().size(), NUMBER_OF_OWNERS);
      List<Object> values = map.values().stream()
            .filter(Objects::nonNull)
            .map(InternalCacheValue::getValue)
            .collect(Collectors.toList());
      assertEquals(values.size(), NUMBER_OF_OWNERS);

      if (checkEquality) {
         assertTrue(values.stream().allMatch(v -> v.equals(values.get(0))), "Inconsistent values returned, they should be the same");
      } else {
         assertTrue(values.stream().distinct().count() > 1, "Expected inconsistent values, but all values were equal");
      }
   }

   private Stream<Map<Address, InternalCacheValue<Object>>> getConflicts(Cache<Object, Object> cache) {
      return cache.getAdvancedCache().getConflictResolutionManager().getConflicts();
   }

   private Map<Address, InternalCacheValue<Object>> getAllVersions(Cache<Object, Object> cache, Object key) {
      return cache.getAdvancedCache().getConflictResolutionManager().getAllVersions(key);
   }
}
