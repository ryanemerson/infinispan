package org.infinispan.marshall.protostream.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.io.ByteBuffer;
import org.infinispan.commons.io.ByteBufferImpl;
import org.infinispan.commons.marshall.BufferSizePredictor;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.commons.marshall.MarshallingException;
import org.infinispan.commons.marshall.StreamAwareMarshaller;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.factories.KnownComponentNames;
import org.infinispan.factories.annotations.ComponentName;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.factories.annotations.Start;
import org.infinispan.factories.impl.ComponentRef;
import org.infinispan.factories.scopes.Scope;
import org.infinispan.factories.scopes.Scopes;
import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.util.logging.Log;

/**
 * An abstract ProtoStream based {@link Marshaller} and {@link StreamAwareMarshaller} implementation that is the basis
 * of the Persistence and Global marshallers.
 *
 * @author Ryan Emerson
 * @since 11.0
 */
@Scope(Scopes.GLOBAL)
public abstract class AbstractInternalProtoStreamMarshaller implements Marshaller, StreamAwareMarshaller {
   private static final int PROTOSTREAM_DEFAULT_BUFFER_SIZE = 4096;

   @Inject protected GlobalComponentRegistry gcr;
   @Inject protected ComponentRef<SerializationContextRegistry> ctxRegistry;
   @Inject @ComponentName(KnownComponentNames.USER_MARSHALLER)
   ComponentRef<Marshaller> userMarshallerRef;

   protected final Log log;
   protected ClassLoader classLoader;
   protected Marshaller userMarshaller;

   abstract public ImmutableSerializationContext getSerializationContext();

   protected AbstractInternalProtoStreamMarshaller(Log log) {
      this.log = log;
   }

   @Start
   @Override
   public void start() {
      classLoader = gcr.getGlobalConfiguration().classLoader();
      userMarshaller = userMarshallerRef.running();
   }

   public Marshaller getUserMarshaller() {
      return userMarshaller;
   }

   @Override
   public ByteBuffer objectToBuffer(Object o) {
      return nullableObjectToBuffer(o, -1);
   }

   @Override
   public byte[] objectToByteBuffer(Object obj, int estimatedSize) {
      ByteBuffer b = nullableObjectToBuffer(obj, estimatedSize);
      if (b == null)
         return null;
      byte[] bytes = new byte[b.getLength()];
      System.arraycopy(b.getBuf(), b.getOffset(), bytes, 0, b.getLength());
      return bytes;
   }

   @Override
   public byte[] objectToByteBuffer(Object obj) {
      return objectToByteBuffer(obj, sizeEstimate(obj));
   }

   private ByteBuffer nullableObjectToBuffer(Object o, int estimatedSize) {
      if (o == null)
         return null;
      return objectToBuffer(o, estimatedSize);
   }

   protected ByteBuffer objectToBuffer(Object o, int estimatedSize) {
      try {
         if (requiresWrapping(o))
            o = new MarshallableUserObject<>(o);
         int size = estimatedSize < 0 ? PROTOSTREAM_DEFAULT_BUFFER_SIZE : estimatedSize;
         ByteArrayOutputStream baos = new ByteArrayOutputStream(size);
         ProtobufUtil.toWrappedStream(getSerializationContext(), baos, o, size);
         byte[] bytes = baos.toByteArray();
         return new ByteBufferImpl(bytes, 0, bytes.length);
      } catch (Throwable t) {
         log.cannotMarshall(o.getClass(), t);
         if (t instanceof MarshallingException)
            throw (MarshallingException) t;
         throw new MarshallingException(t.getMessage(), t.getCause());
      }
   }

   @Override
   public Object objectFromByteBuffer(byte[] buf) throws IOException {
      return objectFromByteBuffer(buf, 0, buf.length);
   }

   @Override
   public Object objectFromByteBuffer(byte[] buf, int offset, int length) throws IOException {
      return unwrapAndInit(ProtobufUtil.fromWrappedByteArray(getSerializationContext(), buf, offset, length));
   }

   @Override
   public BufferSizePredictor getBufferSizePredictor(Object o) {
      // TODO if protobuf based, return estimate based upon schema
      return userMarshaller.getBufferSizePredictor(o);
   }

   @Override
   public void writeObject(Object o, OutputStream out) throws IOException {
      if (requiresWrapping(o))
         o = new MarshallableUserObject<>(o);
      ProtobufUtil.toWrappedStream(getSerializationContext(), out, o, PROTOSTREAM_DEFAULT_BUFFER_SIZE);
   }

   @Override
   public Object readObject(InputStream in) throws ClassNotFoundException, IOException {
      return unwrapAndInit(ProtobufUtil.fromWrappedStream(getSerializationContext(), in));
   }

   protected Object unwrapAndInit(Object o) {
      if (o instanceof MarshallableUserObject)
         return ((MarshallableUserObject<?>) o).get();

      return o;
   }

   @Override
   public boolean isMarshallable(Object o) {
      return isMarshallableWithProtoStream(o) || isUserMarshallable(o);
   }

   @Override
   public int sizeEstimate(Object o) {
      if (isMarshallableWithProtoStream(o))
         return PROTOSTREAM_DEFAULT_BUFFER_SIZE;

      int userBytesEstimate = userMarshaller.getBufferSizePredictor(o.getClass()).nextSize(o);
      return MarshallableUserObject.size(userBytesEstimate);
   }

   @Override
   public MediaType mediaType() {
      return MediaType.APPLICATION_PROTOSTREAM;
   }

   private boolean requiresWrapping(Object o) {
      return !isMarshallableWithProtoStream(o);
   }

   private boolean isMarshallableWithProtoStream(Object o) {
      return getSerializationContext().canMarshall(o.getClass());
   }

   private boolean isUserMarshallable(Object o) {
      try {
         return userMarshaller.isMarshallable(o);
      } catch (Exception ignore) {
         return false;
      }
   }
}
