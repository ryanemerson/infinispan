package org.infinispan.test.data;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.Set;

import org.infinispan.commons.marshall.AbstractExternalizer;
import org.infinispan.commons.marshall.MarshallUtil;
import org.infinispan.commons.marshall.SerializeWith;
import org.infinispan.commons.util.Util;

@SerializeWith(Address.Externalizer.class)
public class Address implements Serializable {
   private static final long serialVersionUID = 5943073369866339615L;

   String street = null;
   String city = "San Jose";
   int zip = 0;

   public String getStreet() {
      return street;
   }

   public void setStreet(String street) {
      this.street = street;
   }

   public String getCity() {
      return city;
   }

   public void setCity(String city) {
      this.city = city;
   }

   public int getZip() {
      return zip;
   }

   public void setZip(int zip) {
      this.zip = zip;
   }

   public String toString() {
      return "street=" + getStreet() + ", city=" + getCity() + ", zip=" + getZip();
   }

   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      final Address address = (Address) o;

      if (zip != address.zip) return false;
      if (city != null ? !city.equals(address.city) : address.city != null) return false;
      if (street != null ? !street.equals(address.street) : address.street != null) return false;

      return true;
   }

   public int hashCode() {
      int result;
      result = (street != null ? street.hashCode() : 0);
      result = 29 * result + (city != null ? city.hashCode() : 0);
      result = 29 * result + zip;
      return result;
   }

   // TODO replace with user protostream obj
   public static class Externalizer extends AbstractExternalizer<Address> {
      @Override
      public Set<Class<? extends Address>> getTypeClasses() {
         return Util.asSet(Address.class);
      }

      @Override
      public void writeObject(ObjectOutput output, Address object) throws IOException {
         MarshallUtil.marshallString(object.street, output);
         MarshallUtil.marshallString(object.city, output);
         output.writeInt(object.zip);
      }

      @Override
      public Address readObject(ObjectInput input) throws IOException, ClassNotFoundException {
         Address addr = new Address();
         addr.street = MarshallUtil.unmarshallString(input);
         addr.city = MarshallUtil.unmarshallString(input);
         addr.zip = input.readInt();
         return addr;
      }
   }
}
