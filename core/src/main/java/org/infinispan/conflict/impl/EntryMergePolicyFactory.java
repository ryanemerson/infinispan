package org.infinispan.conflict.impl;

import org.infinispan.conflict.EntryMergePolicy;
import org.infinispan.factories.AbstractNamedCacheComponentFactory;
import org.infinispan.factories.AutoInstantiableFactory;
import org.infinispan.factories.annotations.DefaultFactoryFor;

/**
 * @author Ryan Emerson
 * @since 9.1
 */
@DefaultFactoryFor(classes = EntryMergePolicy.class)
public class EntryMergePolicyFactory extends AbstractNamedCacheComponentFactory implements AutoInstantiableFactory {

   @Override
   public <T> T construct(Class<T> componentType) {
      if (configuration.clustering().cacheMode().isClustered()) {
         return componentType.cast(configuration.clustering().partitionHandling().getMergePolicy());
      }
      return null;
   }
}
