package org.infinispan.objectfilter.impl;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.ProtoSchema;

@ProtoSchema(
      dependsOn = org.infinispan.marshall.core.next.impl.GlobalContextInitializer.class,
      includeClasses = {
            FilterResultImpl.class,
            org.infinispan.objectfilter.impl.syntax.parser.IckleParsingResult.StatementType.class
      },
      schemaFileName = "global.objectfilter.proto",
      schemaFilePath = "proto/generated",
      schemaPackageName = "org.infinispan.global.objectfilter",
      service = false
)
public interface GlobalContextInitializer extends SerializationContextInitializer {
}
