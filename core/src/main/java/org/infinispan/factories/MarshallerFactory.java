package org.infinispan.factories;

import static org.infinispan.factories.KnownComponentNames.INTERNAL_MARSHALLER;
import static org.infinispan.factories.KnownComponentNames.PERSISTENCE_MARSHALLER;

import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.commons.marshall.StreamAwareMarshaller;
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
@DefaultFactoryFor(classes = {StreamingMarshaller.class, StreamAwareMarshaller.class, Marshaller.class})
public class MarshallerFactory extends NamedComponentFactory implements AutoInstantiableFactory {

   private StreamingMarshaller internalMarshaller;
   private StreamAwareMarshaller persistenceMarshaller;

   @Override
   public <T> T construct(Class<T> componentType) {
      return construct(componentType, INTERNAL_MARSHALLER);
   }

   @Override
   @SuppressWarnings("unchecked")
   public <T> T construct(Class<T> componentType, String componentName) {
      switch (componentName) {
         case PERSISTENCE_MARSHALLER:
            if (persistenceMarshaller == null) {
               persistenceMarshaller = new PersistenceMarshallerImpl();
            }
            return (T) persistenceMarshaller;
         case INTERNAL_MARSHALLER:
            if (internalMarshaller == null) {
               internalMarshaller = new GlobalMarshaller();
            }
            return (T) internalMarshaller;
         default:
            throw new IllegalArgumentException(String.format("Marshaller name '%s' not recognised", componentName));
      }
   }
}
