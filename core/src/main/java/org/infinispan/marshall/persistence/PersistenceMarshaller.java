package org.infinispan.marshall.persistence;

import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.commons.marshall.StreamAwareMarshaller;
import org.infinispan.factories.scopes.Scope;
import org.infinispan.factories.scopes.Scopes;
import org.infinispan.persistence.spi.InitializationContext;
import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.SerializationContext;

/**
 * The marshaller that is responsible serializaing/desearilizing objects which are to be persisted.
 *
 * @author Ryan Emerson
 * @since 10.0
 */
@Scope(Scopes.GLOBAL)
public interface PersistenceMarshaller extends StreamAwareMarshaller, Marshaller {

   /**
    * The {@link ImmutableSerializationContext} of the marshaller. This context is not used for users types, which
    * should be configured on the user marshaller via {@link org.infinispan.configuration.global.SerializationConfiguration}
    */
   ImmutableSerializationContext getSerializationContext();

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
}
