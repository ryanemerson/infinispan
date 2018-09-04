package org.infinispan.persistence.marshallers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.protostream.MessageMarshaller;

public class MapMarshaller implements MessageMarshaller<Map> {

   private final Class<? extends Map> clazz;
   private final Marshaller marshaller;

   public MapMarshaller(Class<? extends Map> clazz, Marshaller marshaller) {
      this.clazz = clazz;
      this.marshaller = marshaller;
   }

   @Override
   public Map readFrom(ProtoStreamReader reader) throws IOException {
      try {
         String type = reader.readString("keyType");
         List<byte[]> keySetBytes = new ArrayList<>();
         List<byte[]> valueSetBytes = new ArrayList<>();
         if (!type.isEmpty()) {
            keySetBytes = reader.readCollection("keySet", new ArrayList<>(), byte[].class);
            valueSetBytes = reader.readCollection("valueSet", new ArrayList<>(), byte[].class);
         }

         String implementation = reader.readString("implementation");
         Map map = implementation == null ? new HashMap() : (Map) Class.forName(implementation).newInstance();
         if (!type.isEmpty()) {
            for (int i = 0; i < keySetBytes.size(); i++) {
               Object key = marshaller.objectFromByteBuffer(keySetBytes.get(i));
               Object value = marshaller.objectFromByteBuffer(valueSetBytes.get(i));
               map.put(key, value);
            }
         }
         return map;
      } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
         throw new IllegalStateException(e);
      }
   }

   @Override
   public void writeTo(ProtoStreamWriter writer, Map map) throws IOException {
      if (map.isEmpty()) {
         writer.writeString("keyType", "");
      } else {
         try {
            List<byte[]> keys = new ArrayList<>(map.size());
            List<byte[]> values = new ArrayList<>(map.size());
            for (Map.Entry entry : (Set<Map.Entry>) map.entrySet()) {
               keys.add(marshaller.objectToByteBuffer(entry.getKey()));
               values.add(marshaller.objectToByteBuffer(entry.getValue()));
            }

            Class<?> keyClazz = map.keySet().iterator().next().getClass();
            Class<?> valueClazz = map.values().iterator().next().getClass();
            writer.writeString("keyType", keyClazz.getName());
            writer.writeCollection("keySet", keys, byte[].class);

            writer.writeString("valueType", valueClazz.getName());
            writer.writeCollection("valueSet", values, byte[].class);
         } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
         }
      }

      if (!(map instanceof HashMap)) {
         writer.writeString("implementation", map.getClass().getName());
      }
   }

   @Override
   public Class<? extends Map> getJavaClass() {
      return clazz;
   }

   @Override
   public String getTypeName() {
      return "persistence.MapEntry";
   }
}
