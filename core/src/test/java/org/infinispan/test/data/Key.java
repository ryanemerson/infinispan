package org.infinispan.test.data;

import java.util.Objects;

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

      return Objects.equals(value, ((Key) o).value);
   }

   @Override
   public int hashCode() {
      return value != null ? value.hashCode() : 0;
   }
}
