package org.infinispan.marshall.protostream.impl;

import java.util.List;
import java.util.Map;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.util.KeyValuePair;

/**
 * A wrapper for Maps of user objects whose key/value type is unknown until runtime.
 *
 * @author Ryan Emerson
 * @since 11.0
 */
@ProtoTypeId(ProtoStreamTypeIds.MARSHALLABLE_MAP)
public class MarshallableMap<K, V> extends AbstractMarshallableMapWrapper<K, V> {

   /**
    * @param map the {@link Map} to be wrapped.
    * @return a new {@link MarshallableMap} instance containing the passed object if the object is not null,
    * otherwise null.
    */
   public static <K, V> MarshallableMap<K, V> create(Map<K, V> map) {
      return map == null ? null : new MarshallableMap<>(map);
   }

   /**
    * @param wrapper the {@link MarshallableMap} instance to unwrap.
    * @return the wrapped {@link Map} or null if the provided wrapper does not exist.
    */
   public static <K, V> Map<K, V> unwrap(MarshallableMap<K, V> wrapper) {
      return wrapper == null ? null : wrapper.get();
   }

   @ProtoFactory
   MarshallableMap(List<KeyValuePair<K, V>> entries) {
      super(entries);
   }

   private MarshallableMap(Map<K, V> map) {
      super(map);
   }

   public static class Marshaller extends AbstractMarshallableMapWrapper.Marshaller {

      public Marshaller(String typeName) {
         super(typeName);
      }

      @Override
      AbstractMarshallableMapWrapper<Object, Object> newWrapperInstance(Map<Object, Object> map) {
         return new MarshallableMap<>(map);
      }

      @Override
      public Class getJavaClass() {
         return MarshallableMap.class;
      }
   }
}
