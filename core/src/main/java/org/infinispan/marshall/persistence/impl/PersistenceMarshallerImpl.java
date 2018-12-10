package org.infinispan.marshall.persistence.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.SerializedLambda;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.infinispan.commons.CacheException;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.io.ByteBuffer;
import org.infinispan.commons.io.ByteBufferImpl;
import org.infinispan.commons.marshall.AbstractMarshaller;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.factories.annotations.Start;
import org.infinispan.marshall.core.JBossUserMarshaller;
import org.infinispan.marshall.core.MarshallableFunctions;
import org.infinispan.marshall.core.MarshallingException;
import org.infinispan.marshall.persistence.PersistenceMarshaller;
import org.infinispan.metadata.impl.InternalMetadataImpl;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.WrappedMessage;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.impl.RawProtoStreamReaderImpl;
import org.infinispan.protostream.impl.RawProtoStreamWriterImpl;
import org.infinispan.util.function.SerializableFunction;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * A Protostream based {@link org.infinispan.commons.marshall.StreamAwareMarshaller} implementation that is responsible
 * for marshalling/unmarshalling objects which are to be persisted.
 * <p>
 * Known internal objects, such as {@link InternalMetadataImpl}, are defined in "resources/persistence.proto" and are
 * marshalled using the provided {@link org.infinispan.protostream.MessageMarshaller} implementation. Non-core modules
 * can register additional {@link org.infinispan.protostream.MessageMarshaller} and .proto files via the {@link
 * SerializationContext} return by {@link #getSerializationContext()}.
 * <p>
 * If no {@link org.infinispan.protostream.MessageMarshaller} are registered for a provided object, then the marshalling
 * of said object is delegated to the user marshaller {@link org.infinispan.configuration.global.SerializationConfiguration#MARSHALLER}.
 * The bytes generated by the user marshaller are then wrapped in a {@link PersistenceMarshallerImpl.UserBytes} object
 * and marshalled by ProtoStream.
 *
 * @author Ryan Emerson
 * @since 10.0
 */
public class PersistenceMarshallerImpl extends AbstractMarshaller implements PersistenceMarshaller {

   private static final Log log = LogFactory.getLog(PersistenceMarshallerImpl.class, Log.class);
   // TODO move to internal marshaller?
   // TODO remove once the internal marshaller is not based upon jboss-marshalling and externalizers
   private static Set<String> blackListedClasses = new HashSet<>();
   static {
      blackListedClasses.add("org.infinispan.jcache.annotation.DefaultCacheKey");
      blackListedClasses.add("org.infinispan.server.core.transport.NettyTransportConnectionStats$ConnectionAdderTask");
      blackListedClasses.add("org.infinispan.server.hotrod.CheckAddressTask");
      blackListedClasses.add("org.infinispan.server.infinispan.task.DistributedServerTask");
      blackListedClasses.add("org.infinispan.scripting.impl.DataType");
      blackListedClasses.add("org.infinispan.scripting.impl.DistributedScript");
   }

   @Inject private GlobalComponentRegistry gcr;
   private final SerializationContext serializationContext = ProtobufUtil.newSerializationContext();
   final Set<Class> annotatedPojos = new HashSet<>();
   private Marshaller userMarshaller;

   public PersistenceMarshallerImpl() {
   }

   // Must be before PersistenceManager
   @Start(priority = 8)
   @Override
   public void start() {
      GlobalConfiguration globalConfig = gcr.getGlobalConfiguration();
      Marshaller marshaller = globalConfig.serialization().marshaller();
      if (marshaller == null) {
         marshaller = new JBossUserMarshaller(gcr);
      }
      marshaller.start();
      this.userMarshaller = marshaller;
      try {
         PersistenceContext.init(gcr, this);
      } catch (IOException e) {
         throw new CacheException("Exception encountered when initialising the PersistenceMarshaller SerializationContext", e);
      }
   }

   @Override
   public void registerAnnotatedPojos(String protoPackage, Class... classes) {
      Collections.addAll(annotatedPojos, classes);
   }

   public void generateStoreMarshallers() {
      try {
         PersistenceContext.buildPojoMarshallers("generated.stores", annotatedPojos, serializationContext);
         annotatedPojos.clear();
      } catch (IOException e) {
         throw new CacheException("Exception encountered when generating Protostream marshallers", e);
      }
   }

   @Override
   public SerializationContext getSerializationContext() {
      return serializationContext;
   }

   @Override
   public MediaType mediaType() {
      return MediaType.APPLICATION_PROTOSTREAM;
   }

   @Override
   protected ByteBuffer objectToBuffer(Object o, int estimatedSize) {
      try {
         byte[] bytes = ProtobufUtil.toWrappedByteArray(getSerializationContext(), wrap(o));
         return new ByteBufferImpl(bytes, 0, bytes.length);
      } catch (Throwable t) {
         if (log.isDebugEnabled()) log.debugf(t, "Cannot marshall %s", o.getClass().getName());
         throw new MarshallingException(t.getMessage(), t.getCause());
      }
   }

   @Override
   public Object objectFromByteBuffer(byte[] buf, int offset, int length) throws IOException {
      return unwrapAndInit(ProtobufUtil.fromWrappedByteArray(serializationContext, buf, offset, length));
   }

   @Override
   public void writeObject(Object o, OutputStream out) throws IOException {
      WrappedMessage.writeMessage(serializationContext, RawProtoStreamWriterImpl.newInstance(out), wrap(o));
   }

   @Override
   public Object readObject(InputStream in) throws ClassNotFoundException, IOException {
      return unwrapAndInit(WrappedMessage.readMessage(serializationContext, RawProtoStreamReaderImpl.newInstance(in)));
   }

   @Override
   public byte[] marshallUserObject(Object object) {
      try {
         return userMarshaller.objectToByteBuffer(object);
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
         throw new CacheException(e);
      } catch (IOException e) {
         throw new MarshallingException(e);
      }
   }

   @Override
   public Object unmarshallUserBytes(byte[] bytes) {
      try {
         return userMarshaller.objectFromByteBuffer(bytes);
      } catch (Exception e) {
         throw new MarshallingException(e);
      }
   }

   private Object wrap(Object o) {
      return isPersistenceClass(o) ? o : new UserBytes(marshallUserObject(o));
   }

   private Object unwrapAndInit(Object o) {
      if (o instanceof UserBytes) {
         UserBytes userBytes = (UserBytes) o;
         return unmarshallUserBytes(userBytes.bytes);
      }
      return o;
   }

   @Override
   public boolean isMarshallable(Object o) {
      return !isBlacklisted(o) && (isPersistenceClass(o) || isUserMarshallable(o));
   }

   private boolean isBlacklisted(Object o) {
      Class clazz = o.getClass();
      if (blackListedClasses.contains(clazz.getName()))
         return true;

      if (clazz.isArray())
         return Arrays.stream((Object[]) o).anyMatch(this::isBlacklisted);

      // The persistence marshaller should not handle lambdas as these should never be persisted
      if (clazz.isSynthetic() || o instanceof SerializedLambda || o instanceof SerializableFunction)
         return true;

      Class enclosingClass = clazz.getEnclosingClass();
      return enclosingClass != null && enclosingClass.equals(MarshallableFunctions.class);
   }

   private boolean isPersistenceClass(Object o) {
      return serializationContext.canMarshall(o.getClass());
   }

   private boolean isUserMarshallable(Object o) {
      try {
         return userMarshaller.isMarshallable(o);
      } catch (Exception ignore) {
         return false;
      }
   }

   static class UserBytes {

      @ProtoField(number = 1)
      byte[] bytes;

      public UserBytes() {}

      UserBytes(byte[] bytes) {
         this.bytes = bytes;
      }
   }
}
