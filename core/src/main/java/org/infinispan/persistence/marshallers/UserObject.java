package org.infinispan.persistence.marshallers;

import java.io.IOException;

import org.infinispan.protostream.MessageMarshaller;

public class UserObject {
   final Object object;

   public UserObject(Object object) {
      this.object = object;
   }

   public Object get() {
      return object;
   }

   public static class Marshaller implements MessageMarshaller<UserObject> {

      private final org.infinispan.commons.marshall.Marshaller userMarshaller;

      public Marshaller(org.infinispan.commons.marshall.Marshaller userMarshaller) {
         this.userMarshaller = userMarshaller;
      }

      @Override
      public Class<? extends UserObject> getJavaClass() {
         return UserObject.class;
      }

      @Override
      public String getTypeName() {
         return "persistence.UserObject";
      }

      @Override
      public UserObject readFrom(ProtoStreamReader reader) throws IOException {
         byte[] bytes = reader.readBytes("wrappedObject");
         try {
            return new UserObject(userMarshaller.objectFromByteBuffer(bytes, 0, bytes.length));
         } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
         }
      }

      @Override
      public void writeTo(ProtoStreamWriter writer, UserObject wrapper) throws IOException {
         try {
            writer.writeBytes("wrappedObject", userMarshaller.objectToByteBuffer(wrapper.object));
         } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
         }
      }
   }
}
