package org.infinispan.marshall.protostream;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;

import org.infinispan.commons.marshall.StreamingMarshaller;
import org.infinispan.commons.marshall.WrappedByteArray;
import org.infinispan.commons.marshall.protostream.BaseProtoStreamMarshaller;
import org.infinispan.commons.util.FastCopyHashMap;
import org.infinispan.container.versioning.NumericVersion;
import org.infinispan.container.versioning.SimpleClusteredVersion;
import org.infinispan.marshall.core.MarshalledEntryImpl;
import org.infinispan.marshall.protostream.marshallers.EntryVersionMarshaller;
import org.infinispan.marshall.protostream.marshallers.MapMarshaller;
import org.infinispan.metadata.EmbeddedMetadata;
import org.infinispan.metadata.impl.InternalMetadataImpl;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.config.Configuration;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * A marshaller that uses Protocol Buffers.
 *
 * @author remerson@redhat.com
 * @since 9.4
 */
public class ProtoStreamMarshaller extends BaseProtoStreamMarshaller implements StreamingMarshaller {

   private static final Log log = LogFactory.getLog(ProtoStreamMarshaller.class, Log.class);

   private final SerializationContext serializationContext = ProtobufUtil.newSerializationContext(Configuration.builder().build());

   public ProtoStreamMarshaller() {
      try {
//         Code for importing generate POJOs. We probably don't want to do this as we want the .proto files to be as
//         self descriptive as possible for potential external uses ... although we could also export the generated schemas
//         new ProtoSchemaBuilder()
//               .fileName("core.generated")
//               .packageName("core.generated")
//               .addClass(EmbeddedMetadata.Marshaller.Type.class)
//               .addClass(EntryVersionMarshaller.VersionType.class)
//               .build(serializationContext);

         // TODO update so that serializationContext is based upon cache config
         serializationContext.registerProtoFiles(FileDescriptorSource.fromResources("/core.proto"));
         serializationContext.registerMarshaller(new MarshalledEntryImpl.MarshallerImpl(this));
         serializationContext.registerMarshaller(new InternalMetadataImpl.Marshaller());
         serializationContext.registerMarshaller(new EmbeddedMetadata.Marshaller());
         serializationContext.registerMarshaller(new EmbeddedMetadata.TypeMarshaller());
         serializationContext.registerMarshaller(new WrappedByteArray.Marshaller());
         serializationContext.registerMarshaller(new NumericVersion.Marshaller());
         serializationContext.registerMarshaller(new SimpleClusteredVersion.Marshaller());
         serializationContext.registerMarshaller(new EntryVersionMarshaller());
         serializationContext.registerMarshaller(new MapMarshaller(FastCopyHashMap.class, this));
      } catch (Exception e) {
         log.errorf("Error upon creating marshaller", e);
      }
   }

   @Override
   public SerializationContext getSerializationContext() {
      return serializationContext;
   }

   @Override
   public ObjectOutput startObjectOutput(OutputStream os, boolean isReentrant, int estimatedSize) throws IOException {
      log.errorf(stackTrace(Thread.currentThread().getStackTrace()));
      throw new IllegalStateException();
   }

   @Override
   public void finishObjectOutput(ObjectOutput oo) {
      log.errorf(stackTrace(Thread.currentThread().getStackTrace()));
      throw new IllegalStateException();
   }

   @Override
   public void objectToObjectStream(Object obj, ObjectOutput out) throws IOException {
      log.errorf(stackTrace(Thread.currentThread().getStackTrace()));
      throw new IllegalStateException();
   }

   @Override
   public ObjectInput startObjectInput(InputStream is, boolean isReentrant) throws IOException {
      log.errorf(stackTrace(Thread.currentThread().getStackTrace()));
      throw new IllegalStateException();
   }

   @Override
   public void finishObjectInput(ObjectInput oi) {
      log.errorf(stackTrace(Thread.currentThread().getStackTrace()));
   }

   @Override
   public Object objectFromObjectStream(ObjectInput in) throws IOException, ClassNotFoundException, InterruptedException {
      log.errorf(stackTrace(Thread.currentThread().getStackTrace()));
      throw new IllegalStateException();
   }

   @Override
   public void stop() {
      // TODO: Customise this generated block
   }

   @Override
   public void start() {
      // TODO: Customise this generated block
   }

   private String stackTrace(StackTraceElement[] elements) {
      StringBuilder sb = new StringBuilder();
      for (StackTraceElement element : elements) {
         sb.append(element).append("\n");
      }
      return sb.toString();
   }
}
