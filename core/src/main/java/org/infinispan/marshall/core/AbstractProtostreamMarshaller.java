package org.infinispan.marshall.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.io.ByteBuffer;
import org.infinispan.commons.io.ByteBufferImpl;
import org.infinispan.commons.marshall.AbstractMarshaller;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.commons.marshall.StreamAwareMarshaller;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.protostream.MessageMarshaller;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.WrappedMessage;
import org.infinispan.protostream.impl.RawProtoStreamReaderImpl;
import org.infinispan.protostream.impl.RawProtoStreamWriterImpl;

/**
 * @author Ryan Emerson
 * @since 10.0
 */
abstract public class AbstractProtostreamMarshaller extends AbstractMarshaller implements StreamAwareMarshaller {

   private final SerializationContext serCtx = ProtobufUtil.newSerializationContext();
   @Inject protected GlobalComponentRegistry gcr;

   @Override
   protected ByteBuffer objectToBuffer(Object o, int estimatedSize) {
      try {
         byte[] bytes = ProtobufUtil.toWrappedByteArray(getSerializationContext(), wrap(o));
         return new ByteBufferImpl(bytes, 0, bytes.length);
      } catch (Throwable t) {
         throw new MarshallingException(t.getMessage(), t.getCause());
      }
   }

   @Override
   public Object objectFromByteBuffer(byte[] buf, int offset, int length) throws IOException {
      return unwrap(ProtobufUtil.fromWrappedByteArray(serCtx, buf, offset, length));
   }

   @Override
   public void writeObject(Object o, OutputStream out) throws IOException {
      WrappedMessage.writeMessage(serCtx, RawProtoStreamWriterImpl.newInstance(out), wrap(o));
   }

   @Override
   public Object readObject(InputStream in) throws ClassNotFoundException, IOException {
      return unwrap(WrappedMessage.readMessage(serCtx, RawProtoStreamReaderImpl.newInstance(in)));
   }

   @Override
   public MediaType mediaType() {
      return MediaType.APPLICATION_PROTOSTREAM;
   }

   public SerializationContext getSerializationContext() {
      return serCtx;
   }

   private Object wrap(Object o) {
      return getSerializationContext().canMarshall(o.getClass()) ? o : new WrappedObject(o);
   }

   private Object unwrap(Object o) {
      return o instanceof WrappedObject ? ((WrappedObject) o).object : o;
   }

   public static class WrappedObject {
      private final Object object;

      public WrappedObject(Object object) {
         this.object = object;
      }

      public Object get() {
         return object;
      }
   }

   public static class WrappedObjectMarshaller implements MessageMarshaller<WrappedObject> {

      private final String fieldName;
      private final String typeName;
      private final Marshaller marshaller;

      public WrappedObjectMarshaller(String fieldName, String typeName, Marshaller marshaller) {
         this.fieldName = fieldName;
         this.typeName = typeName;
         this.marshaller = marshaller;
      }

      @Override
      public WrappedObject readFrom(ProtoStreamReader reader) throws IOException {
         byte[] bytes = reader.readBytes(fieldName);
         try {
            return new WrappedObject(marshaller.objectFromByteBuffer(bytes, 0, bytes.length));
         } catch (ClassNotFoundException e) {
            throw new MarshallingException(e);
         }
      }

      @Override
      public void writeTo(ProtoStreamWriter writer, WrappedObject wrappedObject) throws IOException {
         try {
            writer.writeBytes(fieldName, marshaller.objectToByteBuffer(wrappedObject.object));
         } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
         }
      }

      @Override
      public Class<? extends WrappedObject> getJavaClass() {
         return WrappedObject.class;
      }

      @Override
      public String getTypeName() {
         return typeName;
      }
   }
}
