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
@ProtoTypeId(ProtoStreamTypeIds.MARSHALLABLE_USER_MAP)
public class MarshallableUserMap<K, V> extends AbstractMarshallableMapWrapper<K, V> {

   /**
    * @param map the {@link Map} to be wrapped.
    * @return a new {@link MarshallableUserMap} instance containing the passed object if the object is not null,
    * otherwise null.
    */
   public static <K, V> MarshallableUserMap<K, V> create(Map<K, V> map) {
      return map == null ? null : new MarshallableUserMap<>(map);
   }

   /**
    * @param wrapper the {@link MarshallableUserMap} instance to unwrap.
    * @return the wrapped {@link Map} or null if the provided wrapper does not exist.
    */
   public static <K, V> Map<K, V> unwrap(MarshallableUserMap<K, V> wrapper) {
      return wrapper == null ? null : wrapper.get();
   }

   @ProtoFactory
   MarshallableUserMap(List<KeyValuePair<K, V>> entries) {
      super(entries);
   }

   private MarshallableUserMap(Map<K, V> map) {
      super(map);
   }

   public static class Marshaller extends AbstractMarshallableMapWrapper.Marshaller {

      public Marshaller(String typeName) {
         super(typeName);
      }

      @Override
      AbstractMarshallableMapWrapper<Object, Object> newWrapperInstance(Map<Object, Object> map) {
         return new MarshallableUserMap<>(map);
      }

      @Override
      public Class getJavaClass() {
         return MarshallableUserMap.class;
      }
   }
}
