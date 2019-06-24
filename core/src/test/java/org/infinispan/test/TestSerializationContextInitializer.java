package org.infinispan.test;

import org.infinispan.distribution.MagicKey;
import org.infinispan.eviction.impl.EvictionWithConcurrentOperationsTest;
import org.infinispan.marshall.CustomClass;
import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.infinispan.test.data.Address;
import org.infinispan.test.data.Person;

@AutoProtoSchemaBuilder(
      includeClasses = {
            Address.class,
            CustomClass.class,
            MagicKey.class,
            Person.class,
            EvictionWithConcurrentOperationsTest.SameHashCodeKey.class
      },
      schemaFileName = "test.proto",
      schemaFilePath = "proto/generated",
      schemaPackageName = "org.infinispan.test")
public interface TestSerializationContextInitializer extends SerializationContextInitializer {
}
