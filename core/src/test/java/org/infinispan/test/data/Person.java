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
import org.infinispan.marshall.core.ExternalPojo;

@SerializeWith(Person.Externalizer.class)
public class Person implements Serializable, ExternalPojo {

   private static final long serialVersionUID = -885384294556845285L;

   String name = null;
   Address address;

   public Person() {
      // Needed for serialization
   }

   public Person(String name) {
      this.name = name;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public void setName(Object obj) {
      this.name = (String) obj;
   }

   public Address getAddress() {
      return address;
   }

   public void setAddress(Address address) {
      this.address = address;
   }

   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("name=").append(getName()).append(" Address= ").append(address);
      return sb.toString();
   }

   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      final Person person = (Person) o;

      if (address != null ? !address.equals(person.address) : person.address != null) return false;
      if (name != null ? !name.equals(person.name) : person.name != null) return false;

      return true;
   }

   public int hashCode() {
      int result;
      result = (name != null ? name.hashCode() : 0);
      result = 29 * result + (address != null ? address.hashCode() : 0);
      return result;
   }

   public static class Externalizer extends AbstractExternalizer<Person> {
      @Override
      public Set<Class<? extends Person>> getTypeClasses() {
         return Util.asSet(Person.class);
      }

      @Override
      public void writeObject(ObjectOutput output, Person object) throws IOException {
         MarshallUtil.marshallString(object.name, output);
         output.writeObject(object.address);
      }

      @Override
      public Person readObject(ObjectInput input) throws IOException, ClassNotFoundException {
         Person p = new Person();
         p.name = MarshallUtil.unmarshallString(input);
         p.address = (Address) input.readObject();
         return p;
      }
   }
}
