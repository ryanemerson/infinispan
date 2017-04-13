package org.infinispan.conflict.impl;

import java.util.List;

import org.infinispan.container.entries.CacheEntry;
import org.infinispan.factories.AbstractNamedCacheComponentFactory;
import org.infinispan.factories.AutoInstantiableFactory;
import org.infinispan.factories.annotations.DefaultFactoryFor;
import org.infinispan.conflict.EntryMergePolicy;
import org.infinispan.conflict.MergePolicy;
import org.infinispan.security.impl.GlobalSecurityManagerImpl;

/**
 * @author Ryan Emerson
 * @since 9.1
 */
@DefaultFactoryFor(classes = EntryMergePolicy.class)
public class EntryMergePolicyFactory extends AbstractNamedCacheComponentFactory implements AutoInstantiableFactory {

   private static <K,V> EntryMergePolicy<K,V> getPreferredAlways() {
      return (CacheEntry<K,V> preferredEntry, List<CacheEntry<K,V>> otherEntries) -> preferredEntry;
   }

   private static <K,V> EntryMergePolicy<K,V> getPreferredNonNull() {
      return (CacheEntry<K,V> preferredEntry, List<CacheEntry<K,V>> otherEntries) -> {
         if (preferredEntry != null || otherEntries.isEmpty()) return preferredEntry;

         return otherEntries.get(0);
      };
   }

   @Override
   public <T> T construct(Class<T> componentType) {
      MergePolicy policy = configuration.clustering().partitionHandling().getMergePolicy();
      switch (policy) {
         case CUSTOM:
            return null;
         case PREFERRED_ALWAYS:
            return componentType.cast(getPreferredAlways());
         case PREFERRED_NON_NULL:
            return componentType.cast(getPreferredNonNull());
      }
      return null;
   }

   private EntryMergePolicy getCustomMergePolicy() {
      Class<?> c = configuration.clustering().partitionHandling().getMergePolicyClass();
      // TODO
      return null;
   }
}
