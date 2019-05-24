package org.infinispan.commons.marshall;

import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.io.ByteBufferImpl;
import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

/**
 * Interface used to initialise a {@link org.infinispan.protostream.SerializationContext} using the specified Pojos,
 * Marshaller implementations and provided .proto schemas.
 *
 * @author Ryan Emerson
 * @since 10.0
 */
@AutoProtoSchemaBuilder(
      includeClasses = {
            ByteBufferImpl.class,
            MediaType.class,
            WrappedByteArray.class
      },
      schemaFileName = "persistence.commons.proto",
      schemaFilePath = "schema.generated",
      schemaPackageName = "persistence.commons")
public interface PersistenceContextInitializer extends SerializationContextInitializer {
}
