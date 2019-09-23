package org.infinispan.marshall.persistence.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Objects;

import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.io.ByteBuffer;
import org.infinispan.commons.io.ByteBufferImpl;
import org.infinispan.commons.marshall.BufferSizePredictor;
import org.infinispan.commons.marshall.MarshallingException;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.factories.annotations.Start;
import org.infinispan.factories.scopes.Scope;
import org.infinispan.factories.scopes.Scopes;
import org.infinispan.marshall.persistence.PersistenceMarshaller;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * A ProtoStream based {@link PersistenceMarshaller} implementation that is responsible for marshalling/unmarshalling
 * objects which are to be persisted.
 * <p>
 * Known internal objects that are required by stores and loaders, such as {@link org.infinispan.metadata.EmbeddedMetadata},
 * are registered with this marshaller's {@link SerializationContext} so that they can be natively marshalled by the
 * underlying ProtoStream marshaller. Marshallers for custom user types can be configured directly with the {@link
 * PersistenceMarshaller} by passing a {@link SerializationContextInitializer} to the {@link
 * #register(SerializationContextInitializer)} method. Similarly, implementations can be specified via xml or by the
 * configuration builder method {@link org.infinispan.configuration.global.SerializationConfigurationBuilder#addContextInitializer(SerializationContextInitializer)}.
 *
 * @author Ryan Emerson
 * @since 10.0
 */
@Scope(Scopes.GLOBAL)
public class PersistenceMarshallerImpl implements PersistenceMarshaller {

   private static final Log log = LogFactory.getLog(PersistenceMarshallerImpl.class, Log.class);
   private static final int PROTOSTREAM_DEFAULT_BUFFER_SIZE = 4096;
   private static final BufferSizePredictor BUFFER_SIZE_PREDICTOR = new BufferSizePredictor() {
      @Override
      public int nextSize(Object obj) {
         // Return the CodedOutputStream.DEFAULT_BUFFER_SIZE as this is equivalent to passing no estimate
         return PROTOSTREAM_DEFAULT_BUFFER_SIZE;
      }

      @Override
      public void recordSize(int previousSize) {
      }
   };

   @Inject GlobalComponentRegistry gcr;
   private final SerializationContext serializationContext = ProtobufUtil.newSerializationContext();

   public PersistenceMarshallerImpl() {
   }

   @Start
   @Override
   public void start() {
      // The user has specified a SerializationContextInitializer, so jboss-marshalling is ignored and serializationContext updated
      Collection<SerializationContextInitializer> scis = gcr.getGlobalConfiguration().serialization().contextInitializers();
      if (scis != null) {
         for (SerializationContextInitializer sci : scis)
            register(serializationContext, sci);
      }
      register(new PersistenceContextInitializerImpl());
   }

   @Override
   public void register(SerializationContextInitializer initializer) {
      Objects.requireNonNull(initializer);
      register(serializationContext, initializer);
   }

   private void register(SerializationContext ctx, SerializationContextInitializer initializer) {
      initializer.registerSchema(ctx);
      initializer.registerMarshallers(ctx);
   }


   @Override
   public MediaType mediaType() {
      return MediaType.APPLICATION_PROTOSTREAM;
   }

   @Override
   public ByteBuffer objectToBuffer(Object o) {
      return objectToBuffer(o, -1);
   }

   @Override
   public byte[] objectToByteBuffer(Object obj, int estimatedSize) throws IOException, InterruptedException {
      ByteBuffer b = objectToBuffer(obj, estimatedSize);
      byte[] bytes = new byte[b.getLength()];
      System.arraycopy(b.getBuf(), b.getOffset(), bytes, 0, b.getLength());
      return bytes;
   }

   @Override
   public byte[] objectToByteBuffer(Object obj) throws IOException, InterruptedException {
      return objectToByteBuffer(obj, sizeEstimate(obj));
   }

   private ByteBuffer objectToBuffer(Object o, int estimatedSize) {
      if (o == null)
         return null;

      try {
         int size = estimatedSize < 0 ? PROTOSTREAM_DEFAULT_BUFFER_SIZE : estimatedSize;
         ByteArrayOutputStream baos = new ByteArrayOutputStream(size);
         ProtobufUtil.toWrappedStream(serializationContext, baos, o, size);
         byte[] bytes = baos.toByteArray();
         return new ByteBufferImpl(bytes, 0, bytes.length);
      } catch (Throwable t) {
         log.warnf(t, "Cannot marshall %s", o.getClass().getName());
         if (t instanceof MarshallingException)
            throw (MarshallingException) t;
         throw new MarshallingException(t.getMessage(), t.getCause());
      }
   }

   @Override
   public Object objectFromByteBuffer(byte[] buf) throws IOException, ClassNotFoundException {
      return objectFromByteBuffer(buf, 0, buf.length);
   }

   @Override
   public Object objectFromByteBuffer(byte[] buf, int offset, int length) throws IOException {
      return ProtobufUtil.fromWrappedByteArray(serializationContext, buf, offset, length);
   }

   @Override
   public BufferSizePredictor getBufferSizePredictor(Object o) {
      // TODO IPROTO-89 return estimate based upon schema
      return BUFFER_SIZE_PREDICTOR;
   }

   @Override
   public void writeObject(Object o, OutputStream out) throws IOException {
      ProtobufUtil.toWrappedStream(serializationContext, out, o);
   }

   @Override
   public Object readObject(InputStream in) throws ClassNotFoundException, IOException {
      return ProtobufUtil.fromWrappedStream(serializationContext, in);
   }

   @Override
   public boolean isMarshallable(Object o) {
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
            serializationContext.canMarshall(o.getClass());
   }

   @Override
   public int sizeEstimate(Object o) {
      // Return the CodedOutputStream.DEFAULT_BUFFER_SIZE as this is equivalent to passing no estimate
      // Dynamic estimates will be provided in a future protostream version IPROTO-89
      return PROTOSTREAM_DEFAULT_BUFFER_SIZE;
   }
}
