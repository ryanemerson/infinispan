package org.infinispan.test;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.distribution.MagicKey;
import org.infinispan.distribution.ch.AffinityPartitionerTest;
import org.infinispan.distribution.groups.BaseUtilGroupTest;
import org.infinispan.distribution.groups.StateTransferGetGroupKeysTest;
import org.infinispan.distribution.rehash.NonTxBackupOwnerBecomingPrimaryOwnerTest;
import org.infinispan.distribution.rehash.NonTxPrimaryOwnerBecomingNonOwnerTest;
import org.infinispan.eviction.impl.EvictionWithConcurrentOperationsTest;
import org.infinispan.marshall.CustomClass;
import org.infinispan.marshall.VersionAwareMarshallerTest;
import org.infinispan.marshall.core.StoreAsBinaryTest;
import org.infinispan.notifications.cachelistener.cluster.AbstractClusterListenerUtilTest;
import org.infinispan.notifications.cachelistener.cluster.NoOpCacheEventFilterConverterWithDependencies;
import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.infinispan.remoting.FailureType;
import org.infinispan.statetransfer.BigObject;
import org.infinispan.statetransfer.ReadAfterLosingOwnershipTest;
import org.infinispan.statetransfer.RemoteGetDuringStateTransferTest;
import org.infinispan.statetransfer.StateTransferFunctionalTest;
import org.infinispan.statetransfer.WriteSkewDuringStateTransferTest;
import org.infinispan.stream.BaseSetupStreamIteratorTest;
import org.infinispan.test.data.Address;
import org.infinispan.test.data.BrokenMarshallingPojo;
import org.infinispan.test.data.CountMarshallingPojo;
import org.infinispan.test.data.DelayedMarshallingPojo;
import org.infinispan.test.data.Key;
import org.infinispan.test.data.Person;
import org.infinispan.xsite.BringSiteOnlineResponse;

@AutoProtoSchemaBuilder(
//      TODO replace with `org.infinispan.test.*` filter when implemented
      includeClasses = {
            AbstractClusterListenerUtilTest.FilterConverter.class,
            AbstractClusterListenerUtilTest.LifespanConverter.class,
            AbstractClusterListenerUtilTest.LifespanFilter.class,
            AbstractClusterListenerUtilTest.NewLifespanLargerFilter.class,
            AbstractClusterListenerUtilTest.StringAppender.class,
            AbstractClusterListenerUtilTest.StringTruncator.class,
            Address.class,
            AffinityPartitionerTest.AffinityKey.class,
            BaseSetupStreamIteratorTest.StringTruncator.class,
            BaseSetupStreamIteratorTest.TestDefaultConsistentHashFactory.class,
            BaseSetupStreamIteratorTest.TestScatteredConsistentHashFactory.class,
            BaseUtilGroupTest.GroupKey.class,
            BigObject.class,
            BringSiteOnlineResponse.class,
            BrokenMarshallingPojo.class,
            CacheMode.class,
            CustomClass.class,
            DelayedMarshallingPojo.class,
//            Requires https://issues.jboss.org/browse/IPROTO-100
//            DistributedStreamIteratorWithStoreAsBinaryTest.MagicKeyStringFilter.class,
//            DistributedStreamIteratorWithStoreAsBinaryTest.MapPair.class,
            EvictionWithConcurrentOperationsTest.SameHashCodeKey.class,
            FailureType.class,
            Key.class,
            MagicKey.class,
            CountMarshallingPojo.class,
            NonTxBackupOwnerBecomingPrimaryOwnerTest.CustomConsistentHashFactory.class,
            NonTxPrimaryOwnerBecomingNonOwnerTest.CustomConsistentHashFactory.class,
            NoOpCacheEventFilterConverterWithDependencies.class,
            Person.class,
            ReadAfterLosingOwnershipTest.SingleKeyConsistentHashFactory.class,
            RemoteGetDuringStateTransferTest.SingleKeyConsistentHashFactory.class,
            StateTransferFunctionalTest.DelayTransfer.class,
            StateTransferGetGroupKeysTest.CustomConsistentHashFactory.class,
            StoreAsBinaryTest.CustomReadObjectMethod.class,
            StoreAsBinaryTest.ObjectThatContainsACustomReadObjectMethod.class,
//            Requires https://issues.jboss.org/browse/IPROTO-101
//            TakeSiteOfflineResponse.class,
//            Xsite.* test failures caused by this
            VersionAwareMarshallerTest.Human.class,
            VersionAwareMarshallerTest.Pojo.class,
            VersionAwareMarshallerTest.PojoExtended.class,
            VersionAwareMarshallerTest.PojoWithExternalAndInternal.class,
            WriteSkewDuringStateTransferTest.ConsistentHashFactoryImpl.class
      },
      schemaFileName = "test.proto",
      schemaFilePath = "proto/generated",
      schemaPackageName = "org.infinispan.test.core")
public interface TestSerializationContextInitializer extends SerializationContextInitializer {
}
