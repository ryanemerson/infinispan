package org.infinispan.marshall.persistence.impl;

import static org.infinispan.marshall.persistence.impl.PersistenceContextInitializer.PACKAGE_NAME;

import org.infinispan.container.versioning.NumericVersion;
import org.infinispan.container.versioning.SimpleClusteredVersion;
import org.infinispan.functional.impl.MetaParamsInternalMetadata;
import org.infinispan.metadata.EmbeddedMetadata;
import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.infinispan.util.ByteString;
import org.infinispan.util.logging.events.EventLogCategory;
import org.infinispan.util.logging.events.EventLogLevel;

/**
 * Interface used to initialise the {@link PersistenceMarshallerImpl}'s {@link org.infinispan.protostream.SerializationContext}
 * using the specified Pojos, Marshaller implementations and provided .proto schemas.
 *
 * @author Ryan Emerson
 * @since 10.0
 */
@AutoProtoSchemaBuilder(
      dependsOn = org.infinispan.commons.marshall.PersistenceContextInitializer.class,
      includeClasses = {
            ByteString.class,
            EmbeddedMetadata.class,
            EmbeddedMetadata.EmbeddedExpirableMetadata.class,
            EmbeddedMetadata.EmbeddedLifespanExpirableMetadata.class,
            EmbeddedMetadata.EmbeddedMaxIdleExpirableMetadata.class,
            EventLogCategory.class,
            EventLogLevel.class,
            MarshalledValueImpl.class,
            MetaParamsInternalMetadata.class,
            NumericVersion.class,
            PersistenceMarshallerImpl.UserBytes.class,
            SimpleClusteredVersion.class,
      },
      schemaFileName = PACKAGE_NAME + ".gen.proto",
      schemaFilePath = "schema/generated",
      schemaPackageName = PACKAGE_NAME)
public interface PersistenceContextInitializer extends SerializationContextInitializer {
      String PACKAGE_NAME = "persistence.core";
}
