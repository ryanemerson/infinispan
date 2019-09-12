package org.infinispan.marshall.protostream;

import org.infinispan.marshall.persistence.impl.PersistenceContextInitializer;
import org.infinispan.protostream.SerializationContext;

/**
 * An empty {@link PersistenceContextInitializer} that can be used to force the {@link
 * org.infinispan.marshall.persistence.impl.PersistenceMarshallerImpl} to not load a serialization based user marshaller
 * when no other {@link org.infinispan.protostream.SerializationContextInitializer} is configured. When this
 * implementation is configured, the PeristenceMarshaller utilise protostream for the marshalling of primitives and
 * other protostream supported types.
 *
 * @author Ryan Emerson
 * @since 10.0
 */
public class DefaultProtoStreamContextInitializer implements PersistenceContextInitializer {

   public String getProtoFileName() {
      return null;
   }

   public String getProtoFile() {
      return null;
   }

   @Override
   public void registerSchema(SerializationContext serCtx) {
      // no-op
   }

   @Override
   public void registerMarshallers(SerializationContext serCtx) {
      // no-op
   }
}
