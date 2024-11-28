package org.infinispan.marshall.protostream.impl.adapters;

import org.infinispan.commons.marshall.MarshallingException;
import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

// TODO ProtoTypeId to reduce payload?
@ProtoAdapter(Class.class)
public class ClassAdapter {

   @ProtoFactory
   Class<?> create(String clazz) {
      try {
         return Class.forName(clazz);
      } catch (ClassNotFoundException e) {
         throw new MarshallingException(e);
      }
   }

   @ProtoField(1)
   String getClazz(Class<?> clazz) {
      return clazz.getName();
   }
}
