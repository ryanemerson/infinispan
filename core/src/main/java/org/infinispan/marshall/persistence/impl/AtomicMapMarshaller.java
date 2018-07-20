package org.infinispan.marshall.persistence.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.infinispan.commons.util.FastCopyHashMap;
import org.infinispan.protostream.MessageMarshaller;
import org.infinispan.protostream.annotations.ProtoField;

final class AtomicMapMarshaller implements MessageMarshaller<FastCopyHashMap> {

   private final PersistenceMarshallerImpl marshaller;

   AtomicMapMarshaller(PersistenceMarshallerImpl marshaller) {
      this.marshaller = marshaller;
   }

   @Override
   public FastCopyHashMap readFrom(ProtoStreamReader reader) throws IOException {
      Collection<AtomicMapEntry> entries = reader.readCollection("entry", new ArrayList<>(), AtomicMapEntry.class);
      int capacity = entries.size();
      return entries.stream()
            .collect(Collectors.toMap(e -> marshaller.unmarshallUserBytes(e.key), e -> marshaller.unmarshallUserBytes(e.value), (o, n) -> n, () -> new FastCopyHashMap<>(capacity)));
   }

   @Override
   public void writeTo(ProtoStreamWriter writer, FastCopyHashMap map) throws IOException {
      if (!map.isEmpty()) {
         List<AtomicMapEntry> entries = ((Set<Map.Entry>) map.entrySet()).stream()
               .map(e -> new AtomicMapEntry(marshaller.marshallUserObject(e.getKey()), marshaller.marshallUserObject(e.getValue())))
               .collect(Collectors.toList());
         writer.writeCollection("entry", entries, AtomicMapEntry.class);
      }
   }

   @Override
   public Class<? extends FastCopyHashMap> getJavaClass() {
      return FastCopyHashMap.class;
   }

   @Override
   public String getTypeName() {
      return PersistenceContextInitializer.PACKAGE_NAME + ".AtomicMap";
   }

   static class AtomicMapEntry {
      @ProtoField(number = 1)
      byte[] key;

      @ProtoField(number = 2)
      byte[] value;

      AtomicMapEntry() {}

      AtomicMapEntry(byte[] key, byte[] value) {
         this.key = key;
         this.value = value;
      }
   }
}
