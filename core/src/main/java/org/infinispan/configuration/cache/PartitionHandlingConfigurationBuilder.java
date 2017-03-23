package org.infinispan.configuration.cache;

import static org.infinispan.configuration.cache.PartitionHandlingConfiguration.MERGE_POLICY;
import static org.infinispan.configuration.cache.PartitionHandlingConfiguration.MERGE_POLICY_CLASS;
import static org.infinispan.configuration.cache.PartitionHandlingConfiguration.TYPE;

import org.infinispan.commons.configuration.Builder;
import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.partitionhandling.MergePolicy;
import org.infinispan.partitionhandling.PartitionHandling;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;
/**
 * Controls how the cache handles partitioning and/or multiple node failures.
 *
 * @author Mircea Markus
 * @since 7.0
 */
public class PartitionHandlingConfigurationBuilder extends AbstractClusteringConfigurationChildBuilder implements Builder<PartitionHandlingConfiguration> {

   private static Log log = LogFactory.getLog(PartitionHandlingConfigurationBuilder.class);

   private final AttributeSet attributes;

   public PartitionHandlingConfigurationBuilder(ClusteringConfigurationBuilder builder) {
      super(builder);
      attributes = PartitionHandlingConfiguration.attributeDefinitionSet();
   }

   /**
    * @param enabled If {@code true}, partitions will enter degraded mode. If {@code false}, they will keep working independently.
    */
   @Deprecated
   public PartitionHandlingConfigurationBuilder enabled(boolean enabled) {
      type(enabled ? PartitionHandling.DENY_ALL : PartitionHandling.ALLOW_ALL);
      return this;
   }

   public PartitionHandlingConfigurationBuilder type(PartitionHandling partitionHandling) {
      attributes.attribute(TYPE).set(partitionHandling);
      return this;
   }

   public PartitionHandlingConfigurationBuilder mergePolicy(MergePolicy mergePolicy) {
      attributes.attribute(MERGE_POLICY).set(mergePolicy);
      return this;
   }

   public PartitionHandlingConfigurationBuilder mergePolicyClass(Class<?> clazz) {
      attributes.attribute(MERGE_POLICY_CLASS).set(clazz);
      return this;
   }

   @Override
   public void validate() {
   }

   @Override
   public void validate(GlobalConfiguration globalConfig) {
   }

   @Override
   public PartitionHandlingConfiguration create() {
      return new PartitionHandlingConfiguration(attributes.protect());
   }

   @Override
   public Builder<?> read(PartitionHandlingConfiguration template) {
      attributes.read(template.attributes());
      return this;
   }
}
