package org.infinispan.conflict.impl;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.util.Collection;

import org.infinispan.conflict.ConflictManager;
import org.infinispan.conflict.ConflictManagerFactory;
import org.infinispan.conflict.MergePolicy;
import org.infinispan.container.entries.InternalCacheValue;
import org.infinispan.context.Flag;
import org.infinispan.distribution.MagicKey;
import org.jgroups.util.Util;
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

   }

   @Override
   void duringSplit() {
      advancedCache(2).withFlags(Flag.CACHE_MODE_LOCAL).put(conflictKey, "DURING SPLIT");

//      fork(() -> {
//         Util.sleep(1000 * 5);
//         System.out.println("OLD: " + cache(0).replace(conflictKey, "FUCK"));
//      });
   }

   @Override
   void afterMerge() {
      Collection<InternalCacheValue> versions = conflictManager(0).getAllVersions(conflictKey).values();
      assertSameVersion(versions, 2, "BEFORE SPLIT");
   }

   ConflictManager conflictManager(int index) {
      return ConflictManagerFactory.get(advancedCache(index));
   }

   void assertSameVersion(Collection<InternalCacheValue> versions, int expectedSize, Object expectedValue) {
      System.out.println("VERSIONS: " + versions);
      assertNotNull(versions);
      assertEquals(expectedSize, versions.size());
      versions.stream().map(InternalCacheValue::getValue).forEach(v -> assertEquals(expectedValue, v));
   }
}
