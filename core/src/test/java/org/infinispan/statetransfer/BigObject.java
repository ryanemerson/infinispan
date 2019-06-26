package org.infinispan.statetransfer;

import org.infinispan.protostream.annotations.ProtoField;

public class BigObject {

    @ProtoField(number = 1)
    String name;

    @ProtoField(number = 2)
    String value;

    BigObject() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      BigObject bigObject = (BigObject) o;

      if (name != null ? !name.equals(bigObject.name) : bigObject.name != null) return false;
      if (value != null ? !value.equals(bigObject.value) : bigObject.value != null) return false;
      return true;
   }

   public int hashCode() {
        int result = getName() != null ? getName().hashCode() : 0;
        return result;
    }
}
