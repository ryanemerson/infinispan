package org.infinispan.tasks.impl;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.ProtoSchema;

/**
 * @author Ryan Emerson
 * @since 16.0
 */
@ProtoSchema(
      dependsOn = org.infinispan.protostream.types.java.CommonTypes.class,
      includeClasses = TaskExecutionImpl.class,
      schemaFileName = "global.tasks.manager.proto",
      schemaFilePath = "proto/generated",
      schemaPackageName = "org.infinispan.global.tasks.manager",
      service = false
)
interface GlobalContextInitializer extends SerializationContextInitializer {
}
