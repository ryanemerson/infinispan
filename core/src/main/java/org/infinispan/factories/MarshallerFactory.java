package org.infinispan.factories;

import static org.infinispan.factories.KnownComponentNames.INTERNAL_MARSHALLER;
import static org.infinispan.factories.KnownComponentNames.PERSISTENCE_MARSHALLER;

import org.infinispan.commons.marshall.StreamingMarshaller;
import org.infinispan.factories.annotations.DefaultFactoryFor;
import org.infinispan.marshall.core.GlobalMarshaller;
import org.infinispan.persistence.marshaller.PersistenceMarshallerImpl;

/**
 * MarshallerFactory.
 *
 * @author Galder Zamarreño
 * @since 4.0
 */
@DefaultFactoryFor(classes = StreamingMarshaller.class)
public class MarshallerFactory extends NamedComponentFactory implements AutoInstantiableFactory {

   @Override
   public <T> T construct(Class<T> componentType) {
      return construct(componentType, INTERNAL_MARSHALLER);
   }

   @Override
   @SuppressWarnings("unchecked")
   public <T> T construct(Class<T> componentType, String componentName) {
      switch (componentName) {
         case PERSISTENCE_MARSHALLER:
            return (T) new PersistenceMarshallerImpl();
         case INTERNAL_MARSHALLER:
            return (T) new GlobalMarshaller();
         default:
            throw new IllegalArgumentException(String.format("Marshaller name '%s' not recognised", componentName));
      }
   }
}
