package org.infinispan.util;

import java.util.Objects;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.marshall.protostream.impl.MarshallableObject;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 *
 * Holds logically related key-value pairs or binary tuples.
 *
 * @author Mircea Markus
 * @since 6.0
 */
@ProtoTypeId(ProtoStreamTypeIds.KEY_VALUE_PAIR)
public class KeyValuePair<K,V> {

   @ProtoField(number = 1)
   final MarshallableObject<K> key;

   @ProtoField(number = 2)
   final MarshallableObject<V> value;

   @ProtoFactory
   KeyValuePair(MarshallableObject<K> key, MarshallableObject<V> value) {
      this.key = key;
      this.value = value;
   }

   public KeyValuePair(K key, V value) {
      this(MarshallableObject.create(key), MarshallableObject.create(value));
   }

   public K getKey() {
      return MarshallableObject.unwrap(key);
   }

   public V getValue() {
      return MarshallableObject.unwrap(value);
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      KeyValuePair<?, ?> that = (KeyValuePair<?, ?>) o;
      return Objects.equals(key, that.key) &&
            Objects.equals(value, that.value);
   }

   @Override
   public int hashCode() {
      return Objects.hash(key, value);
   }

   @Override
   public String toString() {
      return "KeyValuePair{key=" + key + ", value=" + value + '}';
   }
}
