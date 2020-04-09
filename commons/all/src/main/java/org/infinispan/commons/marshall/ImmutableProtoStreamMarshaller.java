package org.infinispan.commons.marshall;

import java.io.IOException;

import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.io.ByteBuffer;
import org.infinispan.commons.io.ByteBufferImpl;
import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.ProtobufUtil;

/**
 * A ProtoStream {@link org.infinispan.commons.marshall.Marshaller} implementation that uses Protobuf encoding.
 *
 * @author Ryan Emerson
 * @since 11.0
 */
public class ImmutableProtoStreamMarshaller extends AbstractMarshaller {

   protected final ImmutableSerializationContext serializationContext;

   public ImmutableProtoStreamMarshaller(ImmutableSerializationContext serializationContext) {
      this.serializationContext = serializationContext;
   }

   public ImmutableSerializationContext getSerializationContext() {
      return serializationContext;
   }

   @Override
   public Object objectFromByteBuffer(byte[] buf, int offset, int length) throws IOException, ClassNotFoundException {
      return ProtobufUtil.fromWrappedByteArray(getSerializationContext(), buf, offset, length);
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
   protected ByteBuffer objectToBuffer(Object o, int estimatedSize) throws IOException {
      byte[] bytes = ProtobufUtil.toWrappedByteArray(getSerializationContext(), o);
      return new ByteBufferImpl(bytes, 0, bytes.length);
   }

   @Override
   public MediaType mediaType() {
      return MediaType.APPLICATION_PROTOSTREAM;
   }
}
