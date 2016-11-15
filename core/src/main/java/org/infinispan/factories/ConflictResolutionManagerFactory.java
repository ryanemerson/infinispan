package org.infinispan.factories;

import org.infinispan.conflict.resolution.ConflictResolutionManager;
import org.infinispan.conflict.resolution.DefaultConflictResolutionManager;
import org.infinispan.factories.annotations.DefaultFactoryFor;

/**
 * @author Ryan Emerson
 * @since 9.0
 */
@DefaultFactoryFor(classes = ConflictResolutionManager.class)
public class ConflictResolutionManagerFactory extends AbstractNamedCacheComponentFactory implements AutoInstantiableFactory {

   @Override
   public <T> T construct(Class<T> componentType) {
      return componentType.cast(new DefaultConflictResolutionManager<>());
   }
}
