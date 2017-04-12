package org.infinispan.configuration.cache;

import org.infinispan.commons.configuration.attributes.AttributeDefinition;
import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.infinispan.conflict.MergePolicy;
import org.infinispan.partitionhandling.PartitionHandling;

/**
 * Controls how the cache handles partitioning and/or multiple node failures.
 *
 * @author Mircea Markus
 * @since 7.0
 */
public class PartitionHandlingConfiguration {

   @Deprecated
   public static final AttributeDefinition<Boolean> ENABLED = AttributeDefinition.builder("enabled", false).immutable()
         .build();
   public static final AttributeDefinition<PartitionHandling> TYPE = AttributeDefinition.builder("type", PartitionHandling.ALLOW_ALL)
         .immutable().build();
   public static final AttributeDefinition<MergePolicy> MERGE_POLICY = AttributeDefinition.builder("mergePolicy", MergePolicy.PREFERRED_ALWAYS)
         .immutable().build();
   // TODO do we want to pass this as just a class? Can we do better i.e. ? extends EntryMergePolicy etc
   public static final AttributeDefinition<Class> MERGE_POLICY_CLASS = AttributeDefinition.builder("mergePolicyClass", null, Class.class)
         .immutable().build();

   static AttributeSet attributeDefinitionSet() {
      return new AttributeSet(PartitionHandlingConfiguration.class, ENABLED, TYPE, MERGE_POLICY, MERGE_POLICY_CLASS);
   }

   private final AttributeSet attributes;

   public PartitionHandlingConfiguration(AttributeSet attributes) {
      this.attributes = attributes.checkProtection();
   }

   @Deprecated
   public boolean enabled() {
      return getType() != PartitionHandling.ALLOW_ALL;
   }

   public PartitionHandling getType() {
      return attributes.attribute(TYPE).get();
   }

   public MergePolicy getMergePolicy() {
      return attributes.attribute(MERGE_POLICY).get();
   }

   public Class<?> getMergePolicyClass() {
      return attributes.attribute(MERGE_POLICY_CLASS).get();
   }

   public AttributeSet attributes() {
      return attributes;
   }

   @Override
   public String toString() {
      return "PartitionHandlingConfiguration [attributes=" + attributes + "]";
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      PartitionHandlingConfiguration other = (PartitionHandlingConfiguration) obj;
      if (attributes == null) {
         if (other.attributes != null)
            return false;
      } else if (!attributes.equals(other.attributes))
         return false;
      return true;
   }
}
