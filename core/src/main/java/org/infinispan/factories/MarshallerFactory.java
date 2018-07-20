package org.infinispan.factories;

import static org.infinispan.factories.KnownComponentNames.INTERNAL_MARSHALLER;
import static org.infinispan.factories.KnownComponentNames.PERSISTENCE_MARSHALLER;

import org.infinispan.commons.CacheException;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.commons.marshall.StreamingMarshaller;
import org.infinispan.factories.annotations.DefaultFactoryFor;
import org.infinispan.marshall.core.GlobalMarshaller;
import org.infinispan.marshall.core.JBossMarshaller;
import org.infinispan.marshall.core.PersistenceMarshaller;

/**
 * MarshallerFactory.
 *
 * @author Galder Zamarreño
 * @since 4.0
 */
@DefaultFactoryFor(classes = {StreamingMarshaller.class, Marshaller.class})
public class MarshallerFactory extends NamedComponentFactory implements AutoInstantiableFactory {

   @Override
   public <T> T construct(Class<T> componentType) {
      return construct(componentType, INTERNAL_MARSHALLER);
   }

   @Override
   public <T> T construct(Class<T> componentType, String componentName) {
      Object comp;
      if (componentName.equals(INTERNAL_MARSHALLER)) {
         comp = new GlobalMarshaller();
      } else if (componentName.equals(PERSISTENCE_MARSHALLER)) {
         comp = new PersistenceMarshaller();
      } else {
         Marshaller userMarshaller = globalConfiguration.serialization().marshaller();
         if (userMarshaller == null) {
            userMarshaller = new JBossMarshaller(globalConfiguration);
         }
         comp = userMarshaller;
      }

      try {
         return componentType.cast(comp);
      } catch (Exception e) {
         throw new CacheException("Problems casting bootstrap component " + comp.getClass() + " to type " + componentType, e);
      }
   }
}
