package org.infinispan.test.data;

import org.infinispan.protostream.annotations.ProtoField;

public class Key {

   @ProtoField(number = 1)
   String value;

   Key() {}

   public Key(String value) {
      this.value = value;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Key k1 = (Key) o;

      if (value != null ? !value.equals(k1.value) : k1.value != null) return false;

      return true;
   }

   @Override
   public int hashCode() {
      return value != null ? value.hashCode() : 0;
   }
}
