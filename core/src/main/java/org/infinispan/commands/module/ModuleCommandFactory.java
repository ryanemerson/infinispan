package org.infinispan.commands.module;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.infinispan.commands.ReplicableCommand;
import org.infinispan.commands.remote.CacheRpcCommand;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.factories.scopes.Scope;
import org.infinispan.factories.scopes.Scopes;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.util.ByteString;

/**
 * Modules which wish to implement their own commands and visitors must also provide an implementation of this
 * interface.
 * <p>
 * Note that this is a {@link Scopes#GLOBAL} component and as such cannot have {@link Inject} methods referring to
 * {@link Scopes#NAMED_CACHE} scoped components.  For such components, use a corresponding {@link
 * Scopes#NAMED_CACHE}-scoped {@link ModuleCommandInitializer}.
 *
 * @author Manik Surtani
 * @since 5.0
 */
@Scope(Scopes.GLOBAL)
public interface ModuleCommandFactory {

   /**
    * Provides a map of command IDs to command types of all the commands handled by the command factory instance.
    * Unmarshalling requests for these command IDs will be dispatched to this implementation.
    *
    * @return map of command IDs to command types handled by this implementation.
    * @deprecated since 10.0 implement {@link #getModuleCommandSet()} instead.
    */
   default Map<Byte, Class<? extends ReplicableCommand>> getModuleCommands() {
      return new HashMap<>();
   }

   default Set<Class<? extends ReplicableCommand>> getModuleCommandSet() {
      return new HashSet<>(getModuleCommands().values());
   }

   /**
    * Construct and initialize a {@link ReplicableCommand} based on the command id.
    *
    * @param commandId command id to construct
    * @return a ReplicableCommand
    * @deprecated since 10.0 custom commands should be registered via {@link #registerInternalClasses(SerializationContext)}.
    * The return value of this method is now ignored.
    */
   default ReplicableCommand fromStream(byte commandId) {
      return null;
   }

   /**
    * Construct and initialize a {@link CacheRpcCommand} based on the command id.
    *
    * @param commandId command id to construct
    * @param cacheName cache name at which command to be created is directed
    * @return a {@link CacheRpcCommand}
    * @deprecated since 10.0 custom commands should be registered via {@link #registerInternalClasses(SerializationContext)}.
    * The return value of this method is now ignored.
    */
   default CacheRpcCommand fromStream(byte commandId, ByteString cacheName) {
      return null;
   }

   /**
    * Register {@link org.infinispan.commands.ReplicableCommand}s to this {@link SerializationContext}.
    *
    * @param serializationContext the {@link org.infinispan.marshall.core.GlobalMarshaller}'s {@link
    *                             SerializationContext}
    * @throws IOException
    * @since 10.0
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
    * @since 10.0
    */
   void registerPersistenceClasses(SerializationContext serializationContext) throws IOException;
}
