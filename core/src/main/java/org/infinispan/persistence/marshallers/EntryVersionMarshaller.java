package org.infinispan.persistence.marshallers;

import java.io.IOException;

import org.infinispan.container.versioning.EntryVersion;
import org.infinispan.container.versioning.NumericVersion;
import org.infinispan.container.versioning.SimpleClusteredVersion;
import org.infinispan.protostream.MessageMarshaller;

public class EntryVersionMarshaller implements MessageMarshaller<EntryVersion> {
   @Override
   public EntryVersion readFrom(ProtoStreamReader reader) throws IOException {
      EntryVersion version = reader.readObject("numeric", NumericVersion.class);
      return version != null ? version : reader.readObject("clustered", SimpleClusteredVersion.class);
   }

   @Override
   public void writeTo(ProtoStreamWriter writer, EntryVersion version) throws IOException {
      if (version instanceof NumericVersion)
         writer.writeObject("numeric", version, NumericVersion.class);
      else if (version instanceof SimpleClusteredVersion)
         writer.writeObject("clustered", version, SimpleClusteredVersion.class);
   }

   @Override
   public Class<? extends EntryVersion> getJavaClass() {
      return EntryVersion.class;
   }

   @Override
   public String getTypeName() {
      return "persistence.EntryVersion";
   }
}
