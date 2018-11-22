package org.infinispan.marshall.core.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.infinispan.commands.ReplicableCommand;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.marshall.core.GlobalMarshaller;
import org.infinispan.metadata.Metadata;
import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.MessageMarshaller;
import org.infinispan.protostream.SerializationContext;

/**
 * Class responsible for initialising the provided {@link org.infinispan.protostream.SerializationContext} with all of
 * the required {@link org.infinispan.protostream.MessageMarshaller} implementations and .proto files for persistence.
 *
 * @author Ryan Emerson
 * @since 10.0
 */
public class InternalContext implements SerializationContext.MarshallerProvider {

   private static final String PROTO_FILE = "org/infinispan/marshall/core/core.proto";

   public static void init(GlobalComponentRegistry gcr, GlobalMarshaller gm) throws IOException {
      ClassLoader classLoader = gcr.getGlobalConfiguration().classLoader();
      SerializationContext ctx = gm.getSerializationContext();
      ctx.registerProtoFiles(FileDescriptorSource.fromResources(classLoader, PROTO_FILE));
      ctx.registerMarshaller(new GlobalMarshaller.WrappedObjectMarshaller("wrappedObject", "core.PersistenceObject", gm.getPersistenceMarshaller()));

      ctx.registerMarshallerProvider(new InternalContext());
   }

   private final Map<String, BaseMarshaller> marshallerMap = new HashMap<>();

   private InternalContext() {
//      addToLookupMap(new ReplicableCommandMarshaller, "persistence.Metadata", Metadata.class);
   }

   @Override
   public BaseMarshaller<?> getMarshaller(String s) {
      return marshallerMap.get(s);
   }

   @Override
   public BaseMarshaller<?> getMarshaller(Class<?> aClass) {
      // For ReplicableCommands, delegate to another class which provides delegating marshallers for specific
      // types of command, e.g. Replicable, RpcCacheCommand, VisitableCommand if possible

      // OR just have marshaller in command as replacement for writeTo and readFrom
      return null;
   }

   private void addToLookupMap(MessageMarshaller<?> marshaller, String messageName, Class lookupClass) {
      marshallerMap.put(messageName, marshaller);
      marshallerMap.put(lookupClass.getName(), marshaller);
   }
}
