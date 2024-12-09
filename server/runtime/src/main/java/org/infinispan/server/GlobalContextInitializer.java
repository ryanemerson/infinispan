package org.infinispan.server;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.ProtoSchema;

@ProtoSchema(
      includeClasses = {
            ExitStatus.class,
            ExitStatus.ExitMode.class,
            Server.ShutdownRunnable.class
      },
      schemaFileName = "global.server.runtime.proto",
      schemaFilePath = "proto/generated",
      schemaPackageName = "org.infinispan.global.server.runtime",
      service = false

)
interface GlobalContextInitializer extends SerializationContextInitializer {
}
