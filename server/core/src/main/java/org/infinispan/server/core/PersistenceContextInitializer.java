package org.infinispan.server.core;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.infinispan.server.core.backup.CacheBackupEntry;
import org.infinispan.server.core.backup.CounterBackupEntry;

@AutoProtoSchemaBuilder(
      dependsOn = {
            org.infinispan.commons.marshall.PersistenceContextInitializer.class,
            org.infinispan.marshall.persistence.impl.PersistenceContextInitializer.class
      },
      includeClasses = {
            CacheBackupEntry.class,
            CounterBackupEntry.class,
            CacheIgnoreManager.IgnoredCaches.class
      },
      schemaFileName = "persistence.server.core.proto",
      schemaFilePath = "proto/generated",
      schemaPackageName = "org.infinispan.persistence.server.core",
      service = false
)
interface PersistenceContextInitializer extends SerializationContextInitializer {
}
