package org.infinispan.commands.module;

import java.io.IOException;

import org.infinispan.factories.scopes.Scope;
import org.infinispan.factories.scopes.Scopes;
import org.infinispan.protostream.SerializationContext;

/**
 * Modules which need to serialize custom classes using the {@link org.infinispan.marshall.core.GlobalMarshaller} or the
 * {@link org.infinispan.marshall.persistence.PersistenceMarshaller}, must register them with the appropriate {@link
 * SerializationContext}.
 * <p>
 * Classes can be serialized by either registering a .proto file and accompanying {@link
 * org.infinispan.protostream.MessageMarshaller} implementation, or by registering annotated POJOs.
 *
 * @author Ryan Emerson
 * @since 10.0
 */
@Scope(Scopes.GLOBAL)
public interface ModuleSerializationContextInitializer {

   /**
    * Register {@link org.infinispan.commands.ReplicableCommand}s to this {@link SerializationContext}.
    *
    * @param serializationContext the {@link org.infinispan.marshall.core.GlobalMarshaller}'s {@link
    *                             SerializationContext}
    * @throws IOException
    */
   void registerInternalClasses(SerializationContext serializationContext) throws IOException;

   /**
    * Register classes to the {@link SerializationContext} utilised by the {@link org.infinispan.marshall.persistence.PersistenceMarshaller}.
    * Example implementation that should be registered with this are context are custom implementations of {@link
    * org.infinispan.container.versioning.EntryVersion}.
    *
    * @param serializationContext the {@link org.infinispan.marshall.persistence.PersistenceMarshaller}'s {@link
    *                             SerializationContext}
    * @throws IOException
    */
   void registerPersistenceClasses(SerializationContext serializationContext) throws IOException;
}
