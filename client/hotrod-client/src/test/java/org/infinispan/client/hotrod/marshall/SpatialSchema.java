package org.infinispan.client.hotrod.marshall;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.infinispan.query.dsl.embedded.testdomain.FlightRoute;

@AutoProtoSchemaBuilder(
      includeClasses = FlightRoute.class,
      schemaPackageName = "sample_bank_account",
      schemaFileName = "spatial.proto",
      marshallersOnly = true,
      service = false
)
public interface SpatialSchema extends SerializationContextInitializer {

   SerializationContextInitializer INSTANCE = new CustomSpatialSchemaImpl();
}