package org.infinispan.query.core.impl;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.ProtoSchema;

@ProtoSchema(
      dependsOn = org.infinispan.marshall.core.next.impl.GlobalContextInitializer.class,
      includeClasses = {
            org.infinispan.query.core.impl.EmbeddedQuery.DeleteFunction.class,
            org.infinispan.query.core.impl.continuous.ContinuousQueryResult.class,
            org.infinispan.query.core.impl.continuous.ContinuousQueryResult.ResultType.class,
            org.infinispan.query.core.impl.continuous.IckleContinuousQueryCacheEventFilterConverter.class,
            org.infinispan.query.core.impl.eventfilter.IckleCacheEventFilterConverter.class,
            org.infinispan.query.core.impl.eventfilter.IckleFilterAndConverter.class,
      },
      schemaFileName = "global.query.core.proto",
      schemaFilePath = "proto/generated",
      schemaPackageName = "org.infinispan.global.query.core",
      service = false
)
public interface GlobalContextInitializer extends SerializationContextInitializer {
}