package org.infinispan.tools.store.migrator.marshaller.common;

import java.io.IOException;
import java.io.ObjectInput;
import java.util.Collections;
import java.util.Set;

import org.infinispan.container.versioning.SimpleClusteredVersion;
import org.infinispan.marshall.core.Ids;

public class SimpleClusteredVersionExternalizer extends AbstractMigratorExternalizer<SimpleClusteredVersion> {

   @Override
   public SimpleClusteredVersion readObject(ObjectInput unmarshaller) throws IOException, ClassNotFoundException {
      int topologyId = unmarshaller.readInt();
      long version = unmarshaller.readLong();
      return new SimpleClusteredVersion(topologyId, version);
   }

   @Override
   public Integer getId() {
      return Ids.SIMPLE_CLUSTERED_VERSION;
   }

   @Override
   public Set<Class<? extends SimpleClusteredVersion>> getTypeClasses() {
      return Collections.singleton(SimpleClusteredVersion.class);
   }
}
