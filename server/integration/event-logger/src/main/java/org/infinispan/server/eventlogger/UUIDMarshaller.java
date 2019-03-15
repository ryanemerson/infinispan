package org.infinispan.server.eventlogger;

import java.io.IOException;
import java.util.UUID;

import org.infinispan.marshall.persistence.impl.PersistenceContextInitializer;
import org.infinispan.protostream.MessageMarshaller;

class UUIDMarshaller implements MessageMarshaller<UUID> {
   @Override
   public UUID readFrom(ProtoStreamReader reader) throws IOException {
      long msb = reader.readLong("msb");
      long lsb = reader.readLong("lsb");
      return new UUID(msb, lsb);
   }

   @Override
   public void writeTo(ProtoStreamWriter writer, UUID uuid) throws IOException {
      writer.writeLong("msb", uuid.getMostSignificantBits());
      writer.writeLong("lsb", uuid.getLeastSignificantBits());
   }

   @Override
   public Class<? extends UUID> getJavaClass() {
      return UUID.class;
   }

   @Override
   public String getTypeName() {
      return PersistenceContextInitializer.PACKAGE_NAME + ".UUID";
   }
}
