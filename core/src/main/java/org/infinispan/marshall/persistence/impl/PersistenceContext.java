package org.infinispan.marshall.persistence.impl;

import java.io.IOException;
import java.util.Set;

import org.infinispan.atomic.impl.AtomicKeySetImpl;
import org.infinispan.commons.io.ByteBufferImpl;
import org.infinispan.commons.marshall.WrappedByteArray;
import org.infinispan.commons.util.Util;
import org.infinispan.container.versioning.NumericVersion;
import org.infinispan.container.versioning.SimpleClusteredVersion;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.functional.impl.MetaParamsInternalMetadata;
import org.infinispan.metadata.EmbeddedMetadata;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.annotations.ProtoSchemaBuilder;
import org.infinispan.util.ByteString;

/**
 * Class responsible for initialising the provided {@link org.infinispan.protostream.SerializationContext} with all of
 * the required {@link org.infinispan.protostream.MessageMarshaller} implementations and .proto files for persistence.
 *
 * @author Ryan Emerson
 * @since 10.0
 */
public class PersistenceContext {

   static final String PROTO_FILE = "org/infinispan/persistence/persistence.proto";
   static final String GENERATED_PROTO_PACKAGE = "g.persistence";

   public static void init(GlobalComponentRegistry gcr, PersistenceMarshallerImpl pm) throws IOException {
      ClassLoader classLoader = gcr.getGlobalConfiguration().classLoader();
      SerializationContext ctx = pm.serializationContext;

      Set<Class> internalClasses = Util.asSet(
            AtomicMapMarshaller.AtomicMapEntry.class,
            ByteBufferImpl.class,
            ByteString.class,
            EmbeddedMetadata.class,
            EmbeddedMetadata.EmbeddedExpirableMetadata.class,
            EmbeddedMetadata.EmbeddedLifespanExpirableMetadata.class,
            EmbeddedMetadata.EmbeddedMaxIdleExpirableMetadata.class,
            MarshalledValueImpl.class,
            MetaParamsInternalMetadata.class,
            NumericVersion.class,
            PersistenceMarshallerImpl.UserBytes.class,
            SimpleClusteredVersion.class,
            WrappedByteArray.class
      );
      buildPojoMarshallers(GENERATED_PROTO_PACKAGE, internalClasses, ctx);

      ctx.registerProtoFiles(FileDescriptorSource.fromResources(classLoader, PROTO_FILE));
      ctx.registerMarshaller(new AtomicKeySetImpl.KeyMarshaller(pm));
      ctx.registerMarshaller(new AtomicKeySetImpl.Marshaller(gcr, pm));
      ctx.registerMarshaller(new AtomicMapMarshaller(pm));
   }

   static void buildPojoMarshallers(String packageName, Set<Class> annotatedPojos, SerializationContext ctx) throws IOException {
      if (!annotatedPojos.isEmpty()) {
         ProtoSchemaBuilder builder = new ProtoSchemaBuilder()
               .fileName(String.format("%s.proto", packageName))
               .packageName(packageName);

         annotatedPojos.forEach(builder::addClass);
         builder.build(ctx);
      }
   }
}
