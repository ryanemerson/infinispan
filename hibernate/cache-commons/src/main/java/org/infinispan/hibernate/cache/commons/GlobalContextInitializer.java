package org.infinispan.hibernate.cache.commons;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.ProtoSchema;

@ProtoSchema(
      dependsOn = {
            org.infinispan.protostream.types.java.CommonTypes.class,
            org.infinispan.marshall.core.next.impl.GlobalContextInitializer.class,
            org.infinispan.marshall.persistence.impl.PersistenceContextInitializer.class
      },
      includeClasses = {
            org.infinispan.hibernate.cache.commons.util.BeginInvalidationCommand.class,
            org.infinispan.hibernate.cache.commons.util.EndInvalidationCommand.class,
            org.infinispan.hibernate.cache.commons.util.EvictAllCommand.class,
            org.infinispan.hibernate.cache.commons.util.FutureUpdate.class,
            org.infinispan.hibernate.cache.commons.util.Tombstone.class,
            org.infinispan.hibernate.cache.commons.util.Tombstone.ExcludeTombstonesFilter.class,
            org.infinispan.hibernate.cache.commons.util.TombstoneUpdate.class,
            org.infinispan.hibernate.cache.commons.util.VersionedEntry.class,
            org.infinispan.hibernate.cache.commons.util.VersionedEntry.ExcludeEmptyFilter.class
      },
      schemaFileName = "global.hibernate.commons.proto",
      schemaFilePath = "proto/generated",
      schemaPackageName = "org.infinispan.global.hibernate.commons",
      service = false
)
interface GlobalContextInitializer extends SerializationContextInitializer {
}
