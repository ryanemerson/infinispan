package org.infinispan.factories;

import static org.infinispan.util.logging.Log.CONFIG;

import org.infinispan.commons.marshall.ImmutableProtoStreamMarshaller;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.commons.marshall.StreamAwareMarshaller;
import org.infinispan.commons.util.Util;
import org.infinispan.configuration.global.SerializationConfiguration;
import org.infinispan.factories.annotations.DefaultFactoryFor;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.factories.impl.ComponentRef;
import org.infinispan.marshall.core.GlobalMarshaller;
import org.infinispan.marshall.core.impl.DelegatingUserMarshaller;
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
            return new GlobalMarshaller();
         case KnownComponentNames.USER_MARSHALLER:
            return createUserMarshaller();
         default:
            throw new IllegalArgumentException(String.format("Marshaller name '%s' not recognised", componentName));
      }
   }

   private Marshaller createUserMarshaller() {
      SerializationConfiguration serializationConfig = globalConfiguration.serialization();
      Marshaller userMarshaller = serializationConfig.marshaller();
      if (userMarshaller != null) {
         Class<? extends Marshaller> clazz = userMarshaller.getClass();
         if (clazz.getName().equals(Util.JBOSS_USER_MARSHALLER_CLASS)) {
            CONFIG.jbossMarshallingDetected();
            return new DelegatingUserMarshaller(userMarshaller);
         }
      } else {
         userMarshaller = new ImmutableProtoStreamMarshaller(contextRegistry.wired().getUserCtx());
      }

      userMarshaller.initialize(globalComponentRegistry.getCacheManager().getClassWhiteList());
      return new DelegatingUserMarshaller(userMarshaller);
   }
}
