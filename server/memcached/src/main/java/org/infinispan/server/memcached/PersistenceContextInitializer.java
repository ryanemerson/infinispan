package org.infinispan.server.memcached;

import org.infinispan.marshall.persistence.impl.PersistenceMarshallerImpl;
import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

/**
 * Interface used to initialise the {@link PersistenceMarshallerImpl}'s {@link org.infinispan.protostream.SerializationContext}
 * using the specified Pojos, Marshaller implementations and provided .proto schemas.
 *
 * @author Ryan Emerson
 * @since 10.0
 */
@AutoProtoSchemaBuilder(
      autoImportClasses = false,
      classes = MemcachedMetadata.class,
      dependsOn = org.infinispan.marshall.persistence.impl.PersistenceContextInitializer.class,
      schemaFileName = "persistence.memcached.proto",
      schemaFilePath = "schema.generated",
      schemaPackageName = "persistence.memcached")
interface PersistenceContextInitializer extends SerializationContextInitializer {
}
