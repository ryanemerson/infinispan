package org.infinispan.marshall.protostream.impl.marshallers;

import org.infinispan.protostream.MessageMarshaller;

abstract class AbstractMessageMarshaller<T> implements MessageMarshaller<T> {

   protected final String typeName;
   protected final Class<? extends T> clazz;

   protected AbstractMessageMarshaller(String typeName, Class<? extends T> clazz) {
      this.typeName = typeName;
      this.clazz = clazz;
   }

   @Override
   public Class<? extends T> getJavaClass() {
      return clazz;
   }

   @Override
   public String getTypeName() {
      return typeName;
   }
}
