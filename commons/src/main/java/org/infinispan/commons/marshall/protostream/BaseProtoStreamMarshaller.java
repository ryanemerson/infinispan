package org.infinispan.commons.marshall.protostream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.io.ByteBuffer;
import org.infinispan.commons.io.ByteBufferImpl;
import org.infinispan.commons.marshall.AbstractMarshaller;
import org.infinispan.commons.marshall.StreamAwareMarshaller;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.WrappedMessage;
import org.infinispan.protostream.impl.RawProtoStreamReaderImpl;
import org.infinispan.protostream.impl.RawProtoStreamWriterImpl;

/**
 * Provides the starting point for implementing a {@link org.infinispan.commons.marshall.Marshaller} that uses Protobuf
 * encoding. Subclasses must implement just a single {@link #getSerializationContext} lookup method.
 *
 * @author anistor@redhat.com
 * @since 6.0
 */
public abstract class BaseProtoStreamMarshaller extends AbstractMarshaller implements StreamAwareMarshaller {

   protected BaseProtoStreamMarshaller() {
   }

   /**
    * Subclasses must implement this method in order to provide a way to lookup the {@link SerializationContext}
    *
    * @return the SerializationContext instance to use
    */
   protected abstract SerializationContext getSerializationContext();

   @Override
   public Object objectFromByteBuffer(byte[] buf, int offset, int length) throws IOException, ClassNotFoundException {
      return ProtobufUtil.fromWrappedByteArray(getSerializationContext(), buf, offset, length);
   }

   @Override
   public void writeObject(Object o, OutputStream out) throws IOException {
      WrappedMessage.writeMessage(getSerializationContext(), RawProtoStreamWriterImpl.newInstance(out), o);
   }

   @Override
   public Object readObject(InputStream in) throws ClassNotFoundException, IOException {
      return WrappedMessage.readMessage(getSerializationContext(), RawProtoStreamReaderImpl.newInstance(in));
   }

   @Override
   public boolean isMarshallable(Object o) {
      // our marshaller can handle all of these primitive/scalar types as well even if we do not
      // have a per-type marshaller defined in our SerializationContext
      return o instanceof String ||
            o instanceof Long ||
            o instanceof Integer ||
            o instanceof Double ||
            o instanceof Float ||
            o instanceof Boolean ||
            o instanceof byte[] ||
            o instanceof Byte ||
            o instanceof Short ||
            o instanceof Character ||
            o instanceof java.util.Date ||
            o instanceof java.time.Instant ||
            getSerializationContext().canMarshall(o.getClass());
   }

   @Override
   protected ByteBuffer objectToBuffer(Object o, int estimatedSize) throws IOException, InterruptedException {
      byte[] bytes = ProtobufUtil.toWrappedByteArray(getSerializationContext(), o);
      return new ByteBufferImpl(bytes, 0, bytes.length);
   }

   @Override
   public MediaType mediaType() {
      return MediaType.APPLICATION_PROTOSTREAM;
   }
}
