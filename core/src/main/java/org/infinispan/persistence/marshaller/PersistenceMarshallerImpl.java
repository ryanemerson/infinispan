package org.infinispan.persistence.marshaller;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.lang.invoke.SerializedLambda;
import java.util.Arrays;

import org.infinispan.atomic.impl.AtomicKeySetImpl;
import org.infinispan.commons.CacheException;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.io.ByteBuffer;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.commons.marshall.NotSerializableException;
import org.infinispan.commons.marshall.StreamingMarshaller;
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
import org.infinispan.marshall.protostream.marshallers.EntryVersionMarshaller;
import org.infinispan.marshall.protostream.marshallers.MapMarshaller;
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
 * How to handle common classes such as JGroups Address? Do we add as a default externalizer for the JbossMarshaller?
 *    - Failing Tests:
 *       - MemoryBasedEvictionFunctionalStoreAsBinaryTest#testJGroupsAddress
 *       - NumOwnersNodeCrashInSequenceTest
 *
 * @author Ryan Emerson
 * @since 9.4
 */
public class PersistenceMarshallerImpl extends BaseProtoStreamMarshaller implements StreamingMarshaller {

   private static final Log log = LogFactory.getLog(PersistenceMarshallerImpl.class, Log.class);

   @Inject private GlobalComponentRegistry gcr;

   private final SerializationContext serializationContext = ProtobufUtil.newSerializationContext(Configuration.builder().build());
   private Marshaller userMarshaller;

   public PersistenceMarshallerImpl() {
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
         log.error(e);
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
         if (isInternalClass(o)) {
            return super.objectToBuffer(o, estimatedSize);
         }
         return super.objectToBuffer(new UserObject(o));
      } catch (java.io.NotSerializableException nse) {
         // TODO do we still want this? I think it assumes too much about the configured user marshaller as it may not
         // even support the Serializable interface. Replace with generic MarshallingException?
         if (log.isDebugEnabled()) log.debug("Object is not serializable", nse);
         throw new NotSerializableException(nse.getMessage(), nse.getCause());
      }
   }

   @Override
   public Object objectFromByteBuffer(byte[] buf, int offset, int length) throws IOException, ClassNotFoundException {
      try {
         Object o = super.objectFromByteBuffer(buf, offset, length);
         if (o instanceof UserObject)
            return ((UserObject) o).object;
         return o;
      } catch (Exception e) {
         throw e;
      }
   }

   @Override
   public boolean isMarshallable(Object o) {
      return !isBlacklisted(o) && (isInternalClass(o) || isUserMarshallable(o));
   }

   @Override
   public ObjectOutput startObjectOutput(OutputStream os, boolean isReentrant, int estimatedSize) throws IOException {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public void finishObjectOutput(ObjectOutput oo) {
      // TODO: Customise this generated block
   }

   @Override
   public void objectToObjectStream(Object obj, ObjectOutput out) throws IOException {
      // TODO: Customise this generated block
   }

   @Override
   public ObjectInput startObjectInput(InputStream is, boolean isReentrant) throws IOException {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public void finishObjectInput(ObjectInput oi) {
      // TODO: Customise this generated block
   }

   @Override
   public Object objectFromObjectStream(ObjectInput in) throws IOException, ClassNotFoundException, InterruptedException {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public MediaType mediaType() {
      return null;  // TODO: Customise this generated block
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
