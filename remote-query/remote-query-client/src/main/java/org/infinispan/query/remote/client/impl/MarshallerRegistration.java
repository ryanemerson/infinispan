package org.infinispan.query.remote.client.impl;

import java.io.IOException;
import java.util.Map;

import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.WrappedMessage;
import org.infinispan.protostream.config.Configuration;
import org.infinispan.protostream.descriptors.Descriptor;
import org.infinispan.protostream.descriptors.EnumDescriptor;
import org.infinispan.protostream.descriptors.FileDescriptor;
import org.infinispan.protostream.descriptors.GenericDescriptor;
import org.infinispan.protostream.impl.SerializationContextImpl;

/**
 * Registers protobuf schemas and marshsallers for the objects used by remote query request and response objects.
 *
 * @author anistor@redhat.com
 * @since 6.0
 */
public final class MarshallerRegistration {

   private static final String QUERY_PROTO_RES = "org/infinispan/query/remote/client/query.proto";

   private MarshallerRegistration() {
   }

   /**
    * Registers proto files and marshallers.
    *
    * @param ctx the serialization context
    * @throws org.infinispan.protostream.DescriptorParserException if a proto definition file fails to parse correctly
    * @throws IOException if proto file registration fails
    */
   public static void init(SerializationContext ctx) throws IOException {
      registerProtoFiles(ctx);
      registerMarshallers(ctx);
   }

   /**
    * Registers proto files.
    *
    * @param ctx the serialization context
    * @throws org.infinispan.protostream.DescriptorParserException if a proto definition file fails to parse correctly
    * @throws IOException if proto file registration fails
    */
   public static void registerProtoFiles(SerializationContext ctx) throws IOException {
      ctx.registerProtoFiles(FileDescriptorSource.fromResources(MarshallerRegistration.class.getClassLoader(), QUERY_PROTO_RES, WrappedMessage.PROTO_FILE));
   }

   /**
    * Registers marshallers.
    *
    * @param ctx the serialization context
    */
   public static void registerMarshallers(SerializationContext ctx) {
      ctx.registerMarshaller(new QueryRequest.NamedParameter.Marshaller());
      ctx.registerMarshaller(new QueryRequest.Marshaller());
      ctx.registerMarshaller(new QueryResponse.Marshaller());
      ctx.registerMarshaller(new FilterResultMarshaller());
      ctx.registerMarshaller(new ContinuousQueryResult.ResultType.Marshaller());
      ctx.registerMarshaller(new ContinuousQueryResult.Marshaller());
   }

   class Context implements ImmutableSerializationContext {

      SerializationContext delegate = ProtobufUtil.newSerializationContext();

      @Override
      public Map<String, FileDescriptor> getFileDescriptors() {
         return null;  // TODO: Customise this generated block
      }

      @Override
      public Map<String, GenericDescriptor> getGenericDescriptors() {
         return null;  // TODO: Customise this generated block
      }

      @Override
      public Descriptor getMessageDescriptor(String fullTypeName) {
         return null;  // TODO: Customise this generated block
      }

      @Override
      public EnumDescriptor getEnumDescriptor(String fullTypeName) {
         return null;  // TODO: Customise this generated block
      }

      @Override
      public boolean canMarshall(Class<?> javaClass) {
         return false;  // TODO: Customise this generated block
      }

      @Override
      public boolean canMarshall(String fullTypeName) {
         return false;  // TODO: Customise this generated block
      }

      @Override
      public <T> BaseMarshaller<T> getMarshaller(String fullTypeName) {
         return null;  // TODO: Customise this generated block
      }

      @Override
      public <T> BaseMarshaller<T> getMarshaller(Class<T> clazz) {
         return null;  // TODO: Customise this generated block
      }

      @Override
      public String getTypeNameById(Integer typeId) {
         return null;  // TODO: Customise this generated block
      }

      @Override
      public Integer getTypeIdByName(String fullTypeName) {
         return null;  // TODO: Customise this generated block
      }

      @Override
      public GenericDescriptor getDescriptorByTypeId(Integer typeId) {
         switch (typeId) {
            case 1000000: {
               // Modified version of descriptor returned
               return getDescriptorByName(WrappedMessage.PROTOBUF_TYPE_NAME);
            }
         }
         return null;
      }

      @Override
      public GenericDescriptor getDescriptorByName(String fullTypeName) {
         return null;  // TODO: Customise this generated block
      }
   }
}
