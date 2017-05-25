package org.infinispan.conflict.impl;

import org.infinispan.AdvancedCache;
import org.infinispan.conflict.MergePolicies;
import org.infinispan.distribution.MagicKey;

/**
 * @author Ryan Emerson
 * @since 9.1
 */
public class MergePolicyRemoveAllTest extends BaseMergePolicyTest {

   private MagicKey conflictKey;

   public MergePolicyRemoveAllTest() {
      super();
      this.mergePolicy = MergePolicies.REMOVE_ALL;
   }

   @Override
   void beforeSplit() {
      conflictKey = new MagicKey(cache(2), cache(0));
      cache(0).put(conflictKey, "BEFORE SPLIT");
   }

   @Override
   void duringSplit() {
      AdvancedCache<Object, Object> cache = getCacheFromPreferredPartition(advancedCache(0), advancedCache(2));
      cache.put(conflictKey, "DURING SPLIT");
   }

   @Override
   void afterMerge() {
      assert cache(0).get(conflictKey) == null;
      assert cache(1).get(conflictKey) == null;
      assert cache(2).get(conflictKey) == null;
      assert cache(3).get(conflictKey) == null;
   }
}
