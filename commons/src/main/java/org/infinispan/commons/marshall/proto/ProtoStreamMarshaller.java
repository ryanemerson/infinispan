package org.infinispan.commons.marshall.proto;

import java.io.IOException;

import org.infinispan.commons.CacheException;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.io.ByteBuffer;
import org.infinispan.commons.io.ByteBufferImpl;
import org.infinispan.commons.logging.Log;
import org.infinispan.commons.logging.LogFactory;
import org.infinispan.commons.marshall.AbstractMarshaller;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;

/**
 * Provides the starting point for implementing a {@link org.infinispan.commons.marshall.Marshaller} that uses Protobuf
 * encoding. Subclasses must implement just a single {@link #getSerializationContext} lookup method.
 *
 * @author anistor@redhat.com
 * @since 6.0
 */
public class ProtoStreamMarshaller extends AbstractMarshaller {

   private static final Log log = LogFactory.getLog(ProtoStreamMarshaller.class);

   protected final SerializationContext serializationContext;

   public ProtoStreamMarshaller(SerializationContext serializationContext) {
      this.serializationContext = serializationContext;
      SerializationContextInitializer initializer = new UserSerializationContextInternalizerImpl();
      try {
         initializer.registerSchema(serializationContext);
         initializer.registerMarshallers(serializationContext);
      } catch (IOException e) {
         throw new CacheException("Exception encountered when initialising SerializationContext", e);
      }
   }

   /**
    * @return the SerializationContext instance to use
    */
   public SerializationContext getSerializationContext() {
      return serializationContext;
   }

   @Override
   public Object objectFromByteBuffer(byte[] buf, int offset, int length) throws IOException, ClassNotFoundException {
      Object o = ProtobufUtil.fromWrappedByteArray(getSerializationContext(), buf, offset, length);
      if (o instanceof RuntimeMarshallableWrapper)
         ((RuntimeMarshallableWrapper) o).unmarshall(this);
      return o;
   }

   @Override
   public boolean isMarshallable(Object o) {
      if (o instanceof RuntimeMarshallableWrapper)
         o = ((RuntimeMarshallableWrapper) o).get();
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
      if (o instanceof RuntimeMarshallableWrapper) {
         try {
            ((RuntimeMarshallableWrapper) o).marshall(this);
         } catch (IOException | InterruptedException e) {
            throw log.unableToMarshallRuntimeObject(o.getClass().getSimpleName(), RuntimeMarshallableWrapper.class.getSimpleName());
         }
      }

      byte[] bytes = ProtobufUtil.toWrappedByteArray(getSerializationContext(), o);
      return new ByteBufferImpl(bytes, 0, bytes.length);
   }

   @Override
   public MediaType mediaType() {
      return MediaType.APPLICATION_PROTOSTREAM;
   }
}
