package org.infinispan.marshall.persistence.impl;

import static org.infinispan.util.logging.Log.PERSISTENCE;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;

import org.infinispan.commons.CacheException;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.io.ByteBuffer;
import org.infinispan.commons.io.ByteBufferImpl;
import org.infinispan.commons.marshall.BufferSizePredictor;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.commons.marshall.MarshallingException;
import org.infinispan.commons.util.Util;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.SerializationConfiguration;
import org.infinispan.configuration.internal.PrivateGlobalConfiguration;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.factories.annotations.Start;
import org.infinispan.factories.scopes.Scope;
import org.infinispan.factories.scopes.Scopes;
import org.infinispan.marshall.persistence.PersistenceMarshaller;
import org.infinispan.marshall.protostream.impl.MarshallableUserObject;
import org.infinispan.marshall.protostream.impl.SerializationContextRegistry;
import org.infinispan.marshall.protostream.impl.SerializationContextRegistry.MarshallerType;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;

/**
 * A Protostream based {@link PersistenceMarshaller} implementation that is responsible
 * for marshalling/unmarshalling objects which are to be persisted.
 * <p>
 * Known internal objects that are required by stores and loaders, such as {@link org.infinispan.metadata.EmbeddedMetadata},
 * are registered with this marshaller's {@link SerializationContext} so that they can be natively marshalled by the
 * underlying Protostream marshaller. If no entry exists in the {@link SerializationContext} for a given object, then
 * the marshalling of said object is delegated to a user marshaller if configured
 * ({@link org.infinispan.configuration.global.SerializationConfiguration#MARSHALLER}) and the generated bytes are wrapped
 * in a {@link MarshallableUserObject} object and marshalled by ProtoStream.
 *
 * @author Ryan Emerson
 * @since 10.0
 */
@Scope(Scopes.GLOBAL)
public class PersistenceMarshallerImpl implements PersistenceMarshaller {
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
   @Inject SerializationContextRegistry ctxRegistry;

   private Marshaller userMarshaller;

   public PersistenceMarshallerImpl() {
   }

   public SerializationContext getSerializationContext() {
      return ctxRegistry.getPersistenceCtx();
   }

   @Start
   @Override
   public void start() {
      userMarshaller = createUserMarshaller();
      if (userMarshaller != null) {
         PERSISTENCE.startingUserMarshaller(userMarshaller.getClass().getName());
         userMarshaller.start();
      }

      // TODO can we move this to the registry?
      String messageName = PersistenceContextInitializer.getFqTypeName(MarshallableUserObject.class);
      ctxRegistry.addMarshaller(MarshallerType.PERSISTENCE, new MarshallableUserObject.Marshaller(messageName, getUserMarshaller()));
   }

   private Marshaller createUserMarshaller() {
      GlobalConfiguration globalConfig = gcr.getGlobalConfiguration();
      SerializationConfiguration serializationConfig = globalConfig.serialization();
      Marshaller marshaller = serializationConfig.marshaller();
      if (marshaller != null) {
         marshaller.initialize(gcr.getCacheManager().getClassWhiteList());
         return marshaller;
      }

      // If no marshaller or SerializationContextInitializer specified, then we attempt to load `infinispan-jboss-marshalling`
      // and the JBossUserMarshaller, however if it does not exist then we default to the JavaSerializationMarshaller
      try {
         Class<Marshaller> clazz = Util.loadClassStrict("org.infinispan.jboss.marshalling.core.JBossUserMarshaller", globalConfig.classLoader());
         try {
            PrivateGlobalConfiguration privateGlobalCfg = globalConfig.module(PrivateGlobalConfiguration.class);
            if (privateGlobalCfg == null || !privateGlobalCfg.isServerMode()) {
               PERSISTENCE.jbossMarshallingDetected();
            }
            return clazz.getConstructor(GlobalComponentRegistry.class).newInstance(gcr);
         } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new CacheException("Unable to start PersistenceMarshaller with JBossUserMarshaller", e);
         }
      } catch (ClassNotFoundException ignore) {
      }
      return null;
   }

   public Marshaller getUserMarshaller() {
      return userMarshaller == null ? this : userMarshaller;
   }

   @Override
   public void register(SerializationContextInitializer initializer) {
      ctxRegistry.addContextInitializer(MarshallerType.PERSISTENCE, initializer);
   }

   @Override
   public void stop() {
      if (userMarshaller != null)
         userMarshaller.stop();
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
         if (requiresWrapping(o))
            o = new MarshallableUserObject(o);
         int size = estimatedSize < 0 ? PROTOSTREAM_DEFAULT_BUFFER_SIZE : estimatedSize;
         ByteArrayOutputStream baos = new ByteArrayOutputStream(size);
         ProtobufUtil.toWrappedStream(getSerializationContext(), baos, o, size);
         byte[] bytes = baos.toByteArray();
         return new ByteBufferImpl(bytes, 0, bytes.length);
      } catch (Throwable t) {
         PERSISTENCE.cannotMarshall(o.getClass(), t);
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
      return unwrapAndInit(ProtobufUtil.fromWrappedByteArray(getSerializationContext(), buf, offset, length));
   }

   @Override
   public BufferSizePredictor getBufferSizePredictor(Object o) {
      // TODO if persistenceClass, i.e. protobuf based, return estimate based upon schema
      return userMarshaller != null ? userMarshaller.getBufferSizePredictor(o) : BUFFER_SIZE_PREDICTOR;
   }

   @Override
   public void writeObject(Object o, OutputStream out) throws IOException {
      if (requiresWrapping(o))
         o = new MarshallableUserObject(o);
      ProtobufUtil.toWrappedStream(getSerializationContext(), out, o, PROTOSTREAM_DEFAULT_BUFFER_SIZE);
   }

   @Override
   public Object readObject(InputStream in) throws ClassNotFoundException, IOException {
      return unwrapAndInit(ProtobufUtil.fromWrappedStream(getSerializationContext(), in));
   }

   private Object unwrapAndInit(Object o) {
      if (o instanceof MarshallableUserObject) {
         MarshallableUserObject wrapper = (MarshallableUserObject) o;
         return wrapper.get();
      }
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

      if (userMarshaller == null)
         return 0;

      int userBytesEstimate = userMarshaller.getBufferSizePredictor(o.getClass()).nextSize(o);
      return MarshallableUserObject.size(userBytesEstimate);
   }

   private boolean requiresWrapping(Object o) {
      return !isMarshallableWithProtoStream(o) && userMarshaller != null;
   }

   private boolean isMarshallableWithProtoStream(Object o) {
      // If the user marshaller is null, then we rely on ProtoStream for all marshalling
      if (userMarshaller == null) {
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
      return getSerializationContext().canMarshall(o.getClass());
   }

   private boolean isUserMarshallable(Object o) {
      try {
         return userMarshaller != null && userMarshaller.isMarshallable(o);
      } catch (Exception ignore) {
         return false;
      }
   }
}
