package org.infinispan.distribution.rehash;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

@AutoProtoSchemaBuilder(
      includeClasses = {
            NonTxBackupOwnerBecomingPrimaryOwnerTest.CustomConsistentHashFactory.class,
            NonTxPrimaryOwnerBecomingNonOwnerTest.CustomConsistentHashFactory.class,
      },
      schemaFileName = "test.core.distribution.rehash.proto",
      schemaFilePath = "proto/generated",
      schemaPackageName = "org.infinispan.test.core.distribution.rehash")
interface DistributionRehashSCI extends SerializationContextInitializer {
}
