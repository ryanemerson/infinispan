package org.infinispan.server.eventlogger;

import org.infinispan.marshall.persistence.PersistenceMarshaller;
import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

/**
 * Interface used to initialise the {@link PersistenceMarshaller}'s {@link org.infinispan.protostream.SerializationContext}
 * using the specified Pojos, Marshaller implementations and provided .proto schemas.
 *
 * @author Ryan Emerson
 * @since 10.0
 */
@AutoProtoSchemaBuilder(
      autoImportClasses = false,
      classes = ServerEventImpl.class,
      dependsOn = org.infinispan.marshall.persistence.impl.PersistenceContextInitializer.class,
      schemaFileName = "persistence.event_logger.proto",
      schemaFilePath = "schema.generated",
      schemaPackageName = "persistence.event_logger")
interface PersistenceContextInitializer extends SerializationContextInitializer {
}
