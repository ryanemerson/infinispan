package org.infinispan.conflict.impl;

import static org.testng.AssertJUnit.assertEquals;

import java.util.Collection;

import org.infinispan.container.entries.InternalCacheValue;
import org.infinispan.context.Flag;
import org.infinispan.distribution.MagicKey;
import org.infinispan.test.fwk.TestInternalCacheEntryFactory;
import org.testng.annotations.Test;

@Test(groups = "functional", testName = "partitionhandling.MergePolicyCustomTest")
public class MergePolicyCustomTest extends BaseMergePolicyTest {

   MagicKey conflictKey;

   public MergePolicyCustomTest() {
      super();
      this.mergePolicy = ((preferredEntry, otherEntries) -> TestInternalCacheEntryFactory.create(conflictKey, "Custom"));
   }

   @Override
   void beforeSplit() {
      conflictKey = new MagicKey(cache(2), cache(0));
      System.out.println(advancedCache(0).getCacheConfiguration().clustering().partitionHandling().getMergePolicy());
      cache(0).put(conflictKey, "BEFORE SPLIT");
   }

   @Override
   void duringSplit() {
      advancedCache(2).withFlags(Flag.CACHE_MODE_LOCAL).put(conflictKey, "DURING SPLIT");   }

   @Override
   void afterMerge() {
      Collection<InternalCacheValue> versions = conflictManager(0).getAllVersions(conflictKey).values();
      assertSameVersion(versions, 2, "Custom");
      assertEquals(0, conflictManager(0).getConflicts().count());
   }
}
