package org.infinispan.server.resp;

import org.infinispan.marshall.protostream.impl.adapters.collections.EmptyListAdapter;
import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.ProtoSchema;

@ProtoSchema(
      dependsOn = {
            PersistenceContextInitializer.class,
            org.infinispan.marshall.core.next.impl.GlobalContextInitializer.class,
            org.infinispan.server.core.GlobalContextInitializer.class,
      },
      includeClasses = {
            EmptyListAdapter.class,
            org.infinispan.server.resp.commands.tx.WATCH.class,
            org.infinispan.server.resp.filter.ComposedFilterConverter.class,
            org.infinispan.server.resp.filter.EventListenerConverter.class,
            org.infinispan.server.resp.filter.EventListenerKeysFilter.class,
            org.infinispan.server.resp.filter.GlobMatchFilterConverter.class,
            org.infinispan.server.resp.filter.RespTypeFilterConverter.class,
      },
      schemaFileName = "global.resp.proto",
      schemaFilePath = "proto/generated",
      schemaPackageName = "org.infinispan.global.resp",
      service = false
)
public interface GlobalContextInitializer extends SerializationContextInitializer { }
