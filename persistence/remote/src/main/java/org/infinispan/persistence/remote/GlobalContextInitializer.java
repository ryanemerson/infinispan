package org.infinispan.persistence.remote;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.ProtoSchema;

@ProtoSchema(
      dependsOn = org.infinispan.marshall.core.next.impl.GlobalContextInitializer.class,
      includeClasses = {
            org.infinispan.persistence.remote.upgrade.AddSourceRemoteStoreTask.class,
            org.infinispan.persistence.remote.upgrade.CheckRemoteStoreTask.class,
            org.infinispan.persistence.remote.upgrade.DisconnectRemoteStoreTask.class,
            org.infinispan.persistence.remote.upgrade.MigrationTask.class,
            org.infinispan.persistence.remote.upgrade.MigrationTask.EntryWriter.class,
            org.infinispan.persistence.remote.upgrade.RemovedFilter.class,
      },
      schemaFileName = "global.remote.store.proto",
      schemaFilePath = "proto/generated",
      schemaPackageName = "org.infinispan.global.remote.store",
      service = false
)
interface GlobalContextInitializer extends SerializationContextInitializer {
}
