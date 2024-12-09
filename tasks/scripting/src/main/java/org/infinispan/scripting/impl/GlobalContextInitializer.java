package org.infinispan.scripting.impl;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.ProtoSchema;

/**
 * @author Ryan Emerson
 * @since 16.0
 */
@ProtoSchema(
      dependsOn = {
            PersistenceContextInitializer.class,
            org.infinispan.marshall.core.next.impl.GlobalContextInitializer.class,
      },
      includeClasses = DistributedScript.class,
      schemaFileName = "global.scripting.proto",
      schemaFilePath = "proto/generated",
      schemaPackageName = "org.infinispan.global.scripting",
      service = false
)
interface GlobalContextInitializer extends SerializationContextInitializer {
}
