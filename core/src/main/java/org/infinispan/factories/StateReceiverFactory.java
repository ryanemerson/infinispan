package org.infinispan.factories;

import org.infinispan.conflict.impl.StateReceiver;
import org.infinispan.conflict.impl.StateReceiverImpl;
import org.infinispan.factories.annotations.DefaultFactoryFor;

/**
 * @author Ryan Emerson
 * @since 9.1
 */
@DefaultFactoryFor(classes = StateReceiver.class)
public class StateReceiverFactory extends AbstractNamedCacheComponentFactory implements AutoInstantiableFactory {
   @Override
   public <T> T construct(Class<T> componentType) {
      return componentType.cast(new StateReceiverImpl());
   }
}
