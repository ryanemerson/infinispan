package org.infinispan.marshall.persistence;

import org.infinispan.commons.marshall.StreamAwareMarshaller;
import org.infinispan.factories.scopes.Scope;
import org.infinispan.factories.scopes.Scopes;
import org.infinispan.persistence.spi.InitializationContext;
import org.infinispan.protostream.SerializationContext;

/**
 * The marshaller that is responsible serializaing/desearilizing objects which are to be persisted.
 *
 * @author Ryan Emerson
 * @since 10.0
 */
@Scope(Scopes.GLOBAL)
public interface PersistenceMarshaller extends StreamAwareMarshaller {

   /**
    * The {@link SerializationContext} of the marshaller. This can be use to register custom marshallers for objects
    * required by the store implementation. This context should not be used to register marshallers for users types, which
    * should be configured on the user marshaller via {@link org.infinispan.configuration.global.SerializationConfiguration}
    */
   // TODO don't expose this? Or expose Immutable?
   SerializationContext getSerializationContext();

   /**
    * Register annotated pojos with the PersistenceMarshallers {@link SerializationContext}. Marshallers are generated
    * for the annotated classes after the {@link org.infinispan.persistence.manager.PersistenceManager} has called
    * {@link org.infinispan.persistence.spi.CacheWriter#init(InitializationContext)} and {@link org.infinispan.persistence.spi.CacheLoader#init(InitializationContext)}
    * for all configured stores/loaders.
    *
    * @param protoPackage the name of the proto package to be generated
    * @param classes the classes of the annotated pojos to add to the {@link SerializationContext}
    */
   void registerAnnotatedPojos(String protoPackage, Class... classes);

   byte[] marshallUserObject(Object object);

   Object unmarshallUserBytes(byte[] bytes);
}
