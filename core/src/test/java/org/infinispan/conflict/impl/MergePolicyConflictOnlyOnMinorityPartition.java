package org.infinispan.conflict.impl;

import org.infinispan.AdvancedCache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.conflict.MergePolicy;
import org.infinispan.distribution.MagicKey;
import org.testng.annotations.Test;

@Test(groups = "functional", testName = "partitionhandling.MergePolicyConflictOnlyOnMinoryPartition")
public class MergePolicyConflictOnlyOnMinorityPartition extends BaseMergePolicyTest {


   public MergePolicyConflictOnlyOnMinorityPartition() {
      super(CacheMode.DIST_SYNC, "", new int[]{0,1,2}, new int[]{3,4});
      this.valueAfterMerge = "MINORITY PARTITION";
      this.mergePolicy = MergePolicy.NONE;
   }

   @Override
   protected void beforeSplit() {
      conflictKey = new MagicKey(cache(p1.node(0)), cache(p1.node(1)));
      cache(p1.node(0)).put(conflictKey, "MINORITY PARTITION");
   }

   @Override
   protected void duringSplit(AdvancedCache preferredPartitionCache, AdvancedCache otherCache) {
   }
}
