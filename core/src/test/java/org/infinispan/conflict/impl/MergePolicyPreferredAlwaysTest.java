package org.infinispan.conflict.impl;

import java.util.stream.IntStream;

import org.infinispan.AdvancedCache;
import org.infinispan.conflict.ConflictManager;
import org.infinispan.conflict.ConflictManagerFactory;
import org.infinispan.context.Flag;
import org.infinispan.distribution.MagicKey;
import org.infinispan.conflict.MergePolicy;
import org.testng.annotations.Test;

/**
 * @author Ryan Emerson
 * @since 9.0
 */
@Test(groups = "functional", testName = "partitionhandling.MergePolicyPreferredAlwaysTest")
public class MergePolicyPreferredAlwaysTest extends BaseMergePolicyTest {

   private MagicKey conflictKey;

   public MergePolicyPreferredAlwaysTest() {
      super(MergePolicy.PREFERRED_ALWAYS);
   }

   @Override
   void beforeSplit() {
      conflictKey = new MagicKey(cache(2), cache(0));
      cache(0).put(conflictKey, "BEFORE SPLIT");
      getAllVersions("BEFORE SPLIT: ", advancedCache(0));

   }

   @Override
   void duringSplit() {
      // TODO why does this not cause the conflict to be seen during the partition merge?
      advancedCache(2).withFlags(Flag.CACHE_MODE_LOCAL).put(conflictKey, "DURING SPLIT");
      getAllVersions("DURING SPLIT CACHE 0: ", advancedCache(0));
      getAllVersions("DURING SPLIT CACHE 2: ", advancedCache(2));
   }

   @Override
   void afterMerge() {
      IntStream.range(0, 4).forEach(i -> getAllVersions("AFTER HEAL " + i + " : ", advancedCache(i)));
//      ConflictManager<Object, Object> cm = ConflictManagerFactory.get(advancedCache(0));
//      Collection<Map<Address, InternalCacheEntry<Object, Object>>> conflicts = cm.getConflicts().collect(Collectors.toList());
//      assertEquals(0, conflicts.size());
//
//      List<Object> values = cm.getAllVersions(conflictKey).values().stream()
//            .map(InternalCacheValue::getValue)
//            .distinct()
//            .collect(Collectors.toList());
//
//      assertEquals(1, values.size());
//      assertEquals(values.get(0), "DURING SPLIT");
   }

   void getAllVersions(String phase, AdvancedCache<Object, Object> cache) {
      ConflictManager<Object, Object> cm = ConflictManagerFactory.get(cache);
      System.out.println(phase + ": " + cm.getAllVersions(conflictKey));
   }
}
