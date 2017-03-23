package org.infinispan.partitionhandling;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;

/**
 * @author Ryan Emerson
 * @since 9.0
 */
public abstract class BaseMergePolicyTest extends BasePartitionHandlingTest {

   private static final String CACHE_NAME = BaseMergePolicyTest.class.getName();
   private final MergePolicy mergePolicy;

   BaseMergePolicyTest(MergePolicy mergePolicy) {
      this.mergePolicy = mergePolicy;
   }

   @Override
   protected void createCacheManagers() throws Throwable {
      super.createCacheManagers();
      ConfigurationBuilder builder = getDefaultClusteredCacheConfig(CacheMode.DIST_SYNC);
      builder.clustering().partitionHandling().type(PartitionHandling.ALLOW_ALL).mergePolicy(mergePolicy)
            .stateTransfer().fetchInMemoryState(true);
      defineConfigurationOnAllManagers(CACHE_NAME, builder);
   }
}
