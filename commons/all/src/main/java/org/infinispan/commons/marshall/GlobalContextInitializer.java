package org.infinispan.commons.marshall;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

/**
 * Interface used to initialise the global {@link org.infinispan.protostream.SerializationContext} using the specified Pojos,
 * and the generated proto files and marshallers.
 *
 * @author Ryan Emerson
 * @since 10.0
 */
@AutoProtoSchemaBuilder(
      includeClasses = {
            org.infinispan.commons.api.CacheContainerAdmin.AdminFlag.class,
            org.infinispan.commons.tx.XidImpl.class,
            org.infinispan.commons.util.KeyValueWithPrevious.class,
      },
      schemaFileName = "global.commons.proto",
      schemaFilePath = "proto/generated",
      schemaPackageName = "org.infinispan.global.commons")
public interface GlobalContextInitializer extends SerializationContextInitializer {
   org.infinispan.commons.marshall.GlobalContextInitializer INSTANCE = new GlobalContextInitializerImpl();
}
