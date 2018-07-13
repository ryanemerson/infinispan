package org.infinispan.marshall.protostream;

import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.commons.marshall.WrappedByteArray;
import org.infinispan.commons.marshall.protostream.BaseProtoStreamMarshaller;
import org.infinispan.commons.util.FastCopyHashMap;
import org.infinispan.container.versioning.NumericVersion;
import org.infinispan.container.versioning.SimpleClusteredVersion;
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
public class ProtoStreamMarshaller extends BaseProtoStreamMarshaller implements Marshaller {

   private static final Log log = LogFactory.getLog(ProtoStreamMarshaller.class, Log.class);

   private final SerializationContext serializationContext = ProtobufUtil.newSerializationContext(Configuration.builder().build());

   public ProtoStreamMarshaller() {
      try {
         // TODO update so that serializationContext is based upon cache config
         serializationContext.registerProtoFiles(FileDescriptorSource.fromResources("/core.proto"));
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

}
