package org.infinispan.test;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.distribution.MagicKey;
import org.infinispan.distribution.groups.BaseUtilGroupTest;
import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.infinispan.remoting.FailureType;
import org.infinispan.statetransfer.BigObject;
import org.infinispan.test.data.Address;
import org.infinispan.test.data.BrokenMarshallingPojo;
import org.infinispan.test.data.CountMarshallingPojo;
import org.infinispan.test.data.DelayedMarshallingPojo;
import org.infinispan.test.data.Key;
import org.infinispan.test.data.Person;
import org.infinispan.xsite.BringSiteOnlineResponse;

@AutoProtoSchemaBuilder(
      includeClasses = {
            Address.class,
            BaseUtilGroupTest.GroupKey.class,
            BigObject.class,
            BringSiteOnlineResponse.class, // TODO remove?
            BrokenMarshallingPojo.class,
            CacheMode.class,  // TODO remove?
            DelayedMarshallingPojo.class,
            FailureType.class,
            Key.class,
            MagicKey.class,
            CountMarshallingPojo.class,
            Person.class,
//            Requires https://issues.jboss.org/browse/IPROTO-101
//            TakeSiteOfflineResponse.class,
//            Xsite.* test failures caused by this
      },
      schemaFileName = "test.proto",
      schemaFilePath = "proto/generated",
      schemaPackageName = "org.infinispan.test.core")
public interface TestDataSerializationContextInitializer extends SerializationContextInitializer {
}
