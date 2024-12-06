package org.infinispan.server.hotrod;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.ProtoSchema;

@ProtoSchema(
      dependsOn = {
            org.infinispan.commons.GlobalContextInitializer.class,
            org.infinispan.marshall.persistence.impl.PersistenceContextInitializer.class,
            org.infinispan.marshall.core.next.impl.GlobalContextInitializer.class,
            org.infinispan.query.remote.impl.persistence.PersistenceContextInitializer.class
      },
      includeClasses = {
            CheckAddressTask.class,
            org.infinispan.server.hotrod.HotRodServer.ToEmptyBytesKeyValueFilterConverter.class,
            org.infinispan.server.hotrod.KeyValueVersionConverter.class,
            org.infinispan.server.hotrod.MultiHomedServerAddress.class,
            org.infinispan.server.hotrod.MultiHomedServerAddress.InetAddressWithNetMask.class,
            org.infinispan.server.hotrod.command.tx.ForwardCommitCommand.class,
            org.infinispan.server.hotrod.command.tx.ForwardRollbackCommand.class,
            org.infinispan.server.hotrod.event.KeyValueWithPreviousEventConverter.class,
            org.infinispan.server.hotrod.SingleHomedServerAddress.class,
            org.infinispan.server.hotrod.tx.table.CacheXid.class,
            org.infinispan.server.hotrod.tx.table.ClientAddress.class,
            org.infinispan.server.hotrod.tx.table.Status.class,
            org.infinispan.server.hotrod.tx.table.TxState.class,
            org.infinispan.server.hotrod.tx.table.functions.ConditionalMarkAsRollbackFunction.class,
            org.infinispan.server.hotrod.tx.table.functions.CreateStateFunction.class,
            org.infinispan.server.hotrod.tx.table.functions.PreparingDecisionFunction.class,
            org.infinispan.server.hotrod.tx.table.functions.SetCompletedTransactionFunction.class,
            org.infinispan.server.hotrod.tx.table.functions.SetDecisionFunction.class,
            org.infinispan.server.hotrod.tx.table.functions.SetPreparedFunction.class,
            org.infinispan.server.hotrod.tx.table.functions.XidPredicate.class
      },
      schemaFileName = "global.server.hotrod.proto",
      schemaFilePath = "proto/generated",
      schemaPackageName = "org.infinispan.global.server.hotrod",
      service = false

)
public interface GlobalContextInitializer extends SerializationContextInitializer {
}
