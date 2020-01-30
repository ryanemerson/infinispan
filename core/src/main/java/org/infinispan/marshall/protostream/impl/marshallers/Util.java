package org.infinispan.marshall.protostream.impl.marshallers;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

final class Util {
   private static Field getDeclaredField(Class<?> c, String fieldName) {
      try {
         return c.getDeclaredField(fieldName);
      } catch (NoSuchFieldException e) {
         return null;
      }
   }

   static Field getField(Class<?> c, String fieldName) {
      Field field = getDeclaredField(c, fieldName);
      if (field != null) {
         field.setAccessible(true);
      }
      return field;
   }

   static <T> Constructor<T> getConstructor(Class<T> c, Class<?>... parameterTypes) {
      try {
         return c.getConstructor(parameterTypes);
      } catch (NoSuchMethodException e) {
         return null;
      }
   }
}
