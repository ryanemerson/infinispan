package org.infinispan.factories;

import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.marshall.ImmutableProtoStreamMarshaller;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.commons.marshall.StreamAwareMarshaller;
import org.infinispan.factories.annotations.DefaultFactoryFor;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.factories.impl.ComponentRef;
import org.infinispan.marshall.core.GlobalMarshaller;
import org.infinispan.marshall.core.impl.DelegatingUserMarshaller;
import org.infinispan.marshall.core.proto.DelegatingGlobalMarshaller;
import org.infinispan.marshall.persistence.impl.PersistenceMarshallerImpl;
import org.infinispan.marshall.protostream.impl.SerializationContextRegistry;

/**
 * MarshallerFactory.
 *
 * @author Galder Zamarreño
 * @since 4.0
 */
@DefaultFactoryFor(
      classes = {
            Marshaller.class,
            StreamAwareMarshaller.class
      },
      names = {
            KnownComponentNames.INTERNAL_MARSHALLER,
            KnownComponentNames.PERSISTENCE_MARSHALLER,
            KnownComponentNames.USER_MARSHALLER
      }
)
public class MarshallerFactory extends AbstractComponentFactory implements AutoInstantiableFactory {

   @Inject
   ComponentRef<SerializationContextRegistry> contextRegistry;

   @Override
   public Object construct(String componentName) {
      switch (componentName) {
         case KnownComponentNames.PERSISTENCE_MARSHALLER:
            return new PersistenceMarshallerImpl();
         case KnownComponentNames.INTERNAL_MARSHALLER:
            org.infinispan.marshall.core.next.GlobalMarshaller newGm = new org.infinispan.marshall.core.next.GlobalMarshaller();
            GlobalMarshaller oldGm = new GlobalMarshaller();
            return new DelegatingGlobalMarshaller(newGm, oldGm, MediaType.APPLICATION_INFINISPAN_MARSHALLED);
         case KnownComponentNames.USER_MARSHALLER:
            Marshaller marshaller = globalConfiguration.serialization().marshaller();
            if (marshaller != null) {
               marshaller.initialize(globalComponentRegistry.getCacheManager().getClassAllowList());
            } else {
               marshaller = new ImmutableProtoStreamMarshaller(contextRegistry.wired().getUserCtx());
            }
            return new DelegatingUserMarshaller(marshaller);
         default:
            throw new IllegalArgumentException(String.format("Marshaller name '%s' not recognised", componentName));
      }
   }
}
