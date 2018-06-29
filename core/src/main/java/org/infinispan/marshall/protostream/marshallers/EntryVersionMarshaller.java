package org.infinispan.marshall.protostream.marshallers;

import java.io.IOException;

import org.infinispan.container.versioning.EntryVersion;
import org.infinispan.container.versioning.NumericVersion;
import org.infinispan.container.versioning.SimpleClusteredVersion;
import org.infinispan.protostream.MessageMarshaller;
import org.infinispan.protostream.annotations.ProtoEnum;
import org.infinispan.protostream.annotations.ProtoEnumValue;

public class EntryVersionMarshaller implements MessageMarshaller<EntryVersion> {
   @ProtoEnum(name = "VersionType")
   public enum VersionType {
      @ProtoEnumValue(number = 0)
      NUMERIC,
      @ProtoEnumValue(number = 1)
      CLUSTERED
   }

   @Override
   public EntryVersion readFrom(ProtoStreamReader reader) throws IOException {
      VersionType type = reader.readEnum("type", VersionType.class);
      long version = reader.readLong("version");
      if (type == VersionType.NUMERIC) {
         return new NumericVersion(version);
      } else {
         int topology = reader.readInt("topology");
         return new SimpleClusteredVersion(topology, version);
      }
   }

   @Override
   public void writeTo(ProtoStreamWriter writer, EntryVersion version) throws IOException {
      if (version instanceof NumericVersion) {
         writer.writeEnum("type", VersionType.NUMERIC, VersionType.class);
         writer.writeLong("version", ((NumericVersion)version).getVersion());
      } else {
         SimpleClusteredVersion v = (SimpleClusteredVersion) version;
         writer.writeEnum("type", VersionType.CLUSTERED, VersionType.class);
         writer.writeLong("version", v.version);
         writer.writeLong("topology", v.topologyId);
      }
   }

   @Override
   public Class<? extends EntryVersion> getJavaClass() {
      return EntryVersion.class;
   }

   @Override
   public String getTypeName() {
      return "core.EntryVersion";
   }
}
