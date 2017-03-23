package org.infinispan.conflict.impl;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.util.Collection;

import org.infinispan.conflict.ConflictManager;
import org.infinispan.conflict.ConflictManagerFactory;
import org.infinispan.conflict.MergePolicies;
import org.infinispan.container.entries.InternalCacheValue;
import org.infinispan.context.Flag;
import org.infinispan.distribution.MagicKey;
import org.infinispan.partitionhandling.PartitionHandling;
import org.infinispan.remoting.transport.Transport;
import org.infinispan.test.TestingUtil;
import org.testng.annotations.Test;

@Test(groups = "functional", testName = "partitionhandling.MergePolicyPreferredAlwaysTest")
public class MergePolicyPreferredAlwaysTest extends BaseMergePolicyTest {

   private MagicKey conflictKey;

   public MergePolicyPreferredAlwaysTest() {
      super();
      this.mergePolicy = MergePolicies.PREFERRED_ALWAYS;
   }

   @Override
   void beforeSplit() {
      conflictKey = new MagicKey(cache(2), cache(0));
      cache(0).put(conflictKey, "BEFORE SPLIT");
   }

   @Override
   void duringSplit() {
      advancedCache(2).withFlags(Flag.CACHE_MODE_LOCAL).put(conflictKey, "DURING SPLIT");
   }

   @Override
   void afterMerge() {
      Collection<InternalCacheValue> versions = conflictManager(0).getAllVersions(conflictKey).values();
      assertSameVersion(versions, 2, "BEFORE SPLIT");
      assertEquals(0, conflictManager(0).getConflicts().count());
   }
}
