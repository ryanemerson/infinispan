package org.infinispan.server.core;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.ProtoSchema;

@ProtoSchema(
      dependsOn = {
            org.infinispan.commons.marshall.PersistenceContextInitializer.class,
            org.infinispan.marshall.core.next.impl.GlobalContextInitializer.class
      },
      includeClasses = org.infinispan.server.iteration.IterationFilter.class,
      schemaFileName = "global.server.core.proto",
      schemaFilePath = "proto/generated",
      schemaPackageName = "org.infinispan.global.server.core",
      service = false

)
public interface GlobalContextInitializer extends SerializationContextInitializer {
}
