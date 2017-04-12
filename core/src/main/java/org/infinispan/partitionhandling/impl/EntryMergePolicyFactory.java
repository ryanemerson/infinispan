package org.infinispan.partitionhandling.impl;

import org.infinispan.factories.AbstractNamedCacheComponentFactory;
import org.infinispan.factories.AutoInstantiableFactory;
import org.infinispan.factories.annotations.DefaultFactoryFor;
import org.infinispan.conflict.EntryMergePolicy;
import org.infinispan.conflict.MergePolicy;

/**
 * @author Ryan Emerson
 * @since 9.0
 */
@DefaultFactoryFor(classes = EntryMergePolicy.class)
public class EntryMergePolicyFactory extends AbstractNamedCacheComponentFactory implements AutoInstantiableFactory {

   @Override
   @SuppressWarnings("unchecked")
   public <T> T construct(Class<T> componentType) {
      MergePolicy policy = configuration.clustering().partitionHandling().getMergePolicy();
      switch (policy) {
         case CUSTOM:
            return (T) getCustomMergePolicy();
         case VERSION_BASED:
         case PREFERRED_ALWAYS:
         case PREFERRED_NON_NULL:
            return null;
      }
      return null;
   }

   private EntryMergePolicy getCustomMergePolicy() {
      Class<?> c = configuration.clustering().partitionHandling().getMergePolicyClass();
      // TODO
      return null;
   }
}
