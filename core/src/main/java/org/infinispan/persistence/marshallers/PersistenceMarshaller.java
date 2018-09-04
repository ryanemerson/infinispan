package org.infinispan.persistence.marshallers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.SerializedLambda;
import java.util.Arrays;

import org.infinispan.atomic.impl.AtomicKeySetImpl;
import org.infinispan.commons.CacheException;
import org.infinispan.commons.io.ByteBuffer;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.commons.marshall.NotSerializableException;
import org.infinispan.commons.marshall.WrappedByteArray;
import org.infinispan.commons.marshall.protostream.BaseProtoStreamMarshaller;
import org.infinispan.commons.util.FastCopyHashMap;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.container.versioning.NumericVersion;
import org.infinispan.container.versioning.SimpleClusteredVersion;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.factories.annotations.Start;
import org.infinispan.marshall.core.ExternalPojo;
import org.infinispan.marshall.core.JBossMarshaller;
import org.infinispan.marshall.core.MarshallableFunctions;
import org.infinispan.metadata.impl.InternalMetadataImpl;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.config.Configuration;
import org.infinispan.util.KeyValuePair;
import org.infinispan.util.function.SerializableFunction;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * TODO How to allow none-core modules to add additional marshallers to protostream?
 *
 * TODO Make this extend/consume ProtoStreamMarshaller once StreamingMarshaller methods have been implemented
 *
 * @author Ryan Emerson
 * @since 9.4
 */
public class PersistenceMarshaller extends BaseProtoStreamMarshaller {

   private static final Log log = LogFactory.getLog(PersistenceMarshaller.class, Log.class);

   @Inject private GlobalComponentRegistry gcr;

   private final SerializationContext serializationContext = ProtobufUtil.newSerializationContext(Configuration.builder().build());
   private Marshaller userMarshaller;

   public PersistenceMarshaller() {
   }

   // Must be before PersistenceManager
   @Start(priority = 8)
   @Override
   public void start() {
      GlobalConfiguration globalConfiguration = gcr.getGlobalConfiguration();
      Marshaller marshaller = globalConfiguration.serialization().marshaller();
      if (marshaller == null) {
         marshaller = new JBossMarshaller(globalConfiguration);
      }
      marshaller.start();
      this.userMarshaller = marshaller;

      SerializationContext ctx = this.getSerializationContext();
      try {
         ctx.registerProtoFiles(FileDescriptorSource.fromResources("/persistence.proto"));
         ctx.registerMarshaller(new InternalMetadataImpl.Marshaller());
         ctx.registerMarshaller(new MetadataMarshaller());
         ctx.registerMarshaller(new MetadataMarshaller.TypeMarshaller());
         ctx.registerMarshaller(new WrappedByteArray.Marshaller());
         ctx.registerMarshaller(new NumericVersion.Marshaller());
         ctx.registerMarshaller(new SimpleClusteredVersion.Marshaller());
         ctx.registerMarshaller(new EntryVersionMarshaller());
         ctx.registerMarshaller(new UserObject.Marshaller(userMarshaller));
         ctx.registerMarshaller(new KeyValuePair.Marshaller(this));
         ctx.registerMarshaller(new MapMarshaller(FastCopyHashMap.class, this));
         ctx.registerMarshaller(new AtomicKeySetImpl.Marshaller(gcr));
      } catch (IOException e) {
         log.error("Exception thrown when reading 'persistence.proto'", e);
         throw new CacheException(e);
      }
   }

   @Override
   public void stop() {
   }

   @Override
   public SerializationContext getSerializationContext() {
      return serializationContext;
   }

   @Override
   protected ByteBuffer objectToBuffer(Object o, int estimatedSize) throws IOException, InterruptedException {
      try {
         return super.objectToBuffer(wrap(o), estimatedSize);
      } catch (java.io.NotSerializableException nse) {
         // TODO do we still want this? I think it assumes too much about the configured user marshaller as it may not
         // even support the Serializable interface. Replace with generic MarshallingException?
         if (log.isDebugEnabled()) log.debug("Object is not serializable", nse);
         throw new NotSerializableException(nse.getMessage(), nse.getCause());
      }
   }

   @Override
   public Object objectFromByteBuffer(byte[] buf, int offset, int length) throws IOException, ClassNotFoundException {
      return unwrap(super.objectFromByteBuffer(buf, offset, length));
   }

   @Override
   public void writeObject(Object o, OutputStream out) throws IOException {
      super.writeObject(wrap(o), out);
   }

   @Override
   public Object readObject(InputStream in) throws ClassNotFoundException, IOException {
      return unwrap(super.readObject(in));
   }

   private Object wrap(Object o) {
      return isInternalClass(o) ? o : new UserObject(o);
   }

   private Object unwrap(Object o) {
      return o instanceof UserObject ? ((UserObject)o).object : o;
   }

   @Override
   public boolean isMarshallable(Object o) {
      return !isBlacklisted(o) && (isInternalClass(o) || isUserMarshallable(o));
   }

   private boolean isBlacklisted(Object o) {
      Class clazz = o.getClass();
      if (clazz.isArray())
         return Arrays.stream((Object[]) o).anyMatch(this::isBlacklisted);

      // Should be handled by user marshaller
      if (o instanceof ExternalPojo)
         return true;

      // The persistence marshaller should not handle lambdas as these should never be persisted
      if (clazz.isSynthetic() || o instanceof SerializedLambda || o instanceof SerializableFunction)
         return true;

      Class enclosingClass = clazz.getEnclosingClass();
      return enclosingClass != null && enclosingClass.equals(MarshallableFunctions.class);
   }

   private boolean isInternalClass(Object o) {
      return serializationContext.canMarshall(o.getClass());
   }

   private boolean isUserMarshallable(Object o) {
      try {
         return userMarshaller.isMarshallable(o);
      } catch (Exception ignore) {
         return false;
      }
   }
}
