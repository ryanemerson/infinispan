package org.infinispan.marshall.persistence;

import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.commons.marshall.StreamAwareMarshaller;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;

/**
 * The marshaller that is responsible serializaing/desearilizing objects which are to be persisted.
 *
 * @author Ryan Emerson
 * @since 10.0
 */
public interface PersistenceMarshaller extends Marshaller, StreamAwareMarshaller {

   /**
    * Initiates the marshallers {@link SerializationContext} using the supplied {@link
    * SerializationContextInitializer}.
    *
    * @param initializer whose schemas and marshallers' will be registered with the {@link PersistenceMarshaller} {@link
    *                    SerializationContext}
    */
   void init(SerializationContextInitializer initializer);

   /**
    * Convenience method to register a proto file on the classpath with the {@link SerializationContext}. The resource
    * must be available to the configured global {@link ClassLoader}.
    */
   void registerProtoFile(String classPathResource);

   /**
    * The {@link SerializationContext} of the marshaller. This context is not used for users types, which should be
    * configured on the user marshaller via {@link org.infinispan.configuration.global.SerializationConfiguration}
    */
   SerializationContext getSerializationContext();
}
