package org.infinispan.scripting.impl;

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
      classes = {
            ExecutionMode.class,
            ScriptMetadata.class
      },
      dependsOn = org.infinispan.commons.marshall.PersistenceContextInitializer.class,
      schemaFileName = "persistence.scripting.proto",
      schemaFilePath = "schema.generated",
      schemaPackageName = "persistence.scripting")
interface PersistenceContextInitializer extends SerializationContextInitializer {
}
