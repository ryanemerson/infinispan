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
import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.infinispan.remoting.FailureType;
import org.infinispan.remoting.TransportSenderExceptionHandlingTest;
import org.infinispan.statetransfer.BigObject;
import org.infinispan.statetransfer.ReadAfterLosingOwnershipTest;
import org.infinispan.statetransfer.RemoteGetDuringStateTransferTest;
import org.infinispan.statetransfer.StateTransferCacheLoaderFunctionalTest;
import org.infinispan.statetransfer.StateTransferFunctionalTest;
import org.infinispan.statetransfer.WriteSkewDuringStateTransferTest;
import org.infinispan.stream.BaseSetupStreamIteratorTest;
import org.infinispan.stream.BaseStreamTest;
import org.infinispan.test.data.Address;
import org.infinispan.test.data.Key;
import org.infinispan.test.data.Person;

@AutoProtoSchemaBuilder(
      includeClasses = {
            Address.class,
            AffinityPartitionerTest.AffinityKey.class,
            BaseSetupStreamIteratorTest.StringTruncator.class,
            BaseSetupStreamIteratorTest.TestDefaultConsistentHashFactory.class,
            BaseStreamTest.ForEachInjected.class,
            BaseStreamTest.ForEachDoubleInjected.class,
            BaseStreamTest.ForEachIntInjected.class,
            BaseStreamTest.ForEachLongInjected.class,
            BaseUtilGroupTest.GroupKey.class,
            BigObject.class,
            CacheMode.class,
            CustomClass.class,
            EvictionWithConcurrentOperationsTest.SameHashCodeKey.class,
            FailureType.class,
            Key.class,
            MagicKey.class,
            NonTxBackupOwnerBecomingPrimaryOwnerTest.CustomConsistentHashFactory.class,
            NonTxPrimaryOwnerBecomingNonOwnerTest.CustomConsistentHashFactory.class,
            Person.class,
            ReadAfterLosingOwnershipTest.SingleKeyConsistentHashFactory.class,
            RemoteGetDuringStateTransferTest.SingleKeyConsistentHashFactory.class,
            StateTransferCacheLoaderFunctionalTest.DelayedUnmarshal.class,
            StateTransferFunctionalTest.DelayTransfer.class,
            StateTransferGetGroupKeysTest.CustomConsistentHashFactory.class,
            TransportSenderExceptionHandlingTest.BrokenMarshallingPojo.class,
            WriteSkewDuringStateTransferTest.ConsistentHashFactoryImpl.class
      },
      schemaFileName = "test.proto",
      schemaFilePath = "proto/generated",
      schemaPackageName = "org.infinispan.test")
public interface TestSerializationContextInitializer extends SerializationContextInitializer {
}
