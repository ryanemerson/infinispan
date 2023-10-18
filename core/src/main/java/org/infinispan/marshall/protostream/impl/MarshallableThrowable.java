package org.infinispan.marshall.protostream.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.infinispan.commons.marshall.MarshallingException;
import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoName;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.statetransfer.AllOwnersLostException;

/**
 * // TODO: Document this
 *
 * @author Ryan Emerson
 * @since 11.0
 */
@ProtoName("Throwable")
@ProtoTypeId(ProtoStreamTypeIds.MARSHALLABLE_THROWABLE)
public class MarshallableThrowable {

   private static final Map<Class<?>, Type> KNOWN_THROWABLES = new HashMap<>(24);

   static {
      KNOWN_THROWABLES.put(AllOwnersLostException.class, Type.ALL_OWNERS_LOST);
   }

   private volatile Throwable throwable;

//   @ProtoField(number = 1, defaultValue = "UNKNOWN")
//   final Type type;

   @ProtoField(number = 2, name = "implementation")
   final String impl;

   @ProtoField(number = 3, name = "message")
   final String msg;

   @ProtoField(number = 4)
   final MarshallableThrowable cause;

   @ProtoFactory
   MarshallableThrowable(String impl, String msg, MarshallableThrowable cause) {
//   WrappedThrowable(Type type, String impl, String msg, WrappedThrowable cause) {
//      this.type = type;
      this.impl = impl;
      this.msg = msg;
      this.cause = cause;
   }

   // TODO only create if not a known internal exception class
   public static MarshallableThrowable create(Throwable t) {
      if (t == null)
         return null;

      Type type = KNOWN_THROWABLES.getOrDefault(t.getClass(), null);
      if (type == null)
         return new MarshallableThrowable(t.getClass().getName(), t.getMessage(), create(t.getCause()));

      // TODO switch
      return null;
   }

   public Throwable get() {
      if (throwable == null) {
//         switch (type) {
//            default:
//               throwable = recreateGenericThrowable(impl, msg, cause);
//         }
         throwable = recreateGenericThrowable(impl, msg, cause);
      }
      return throwable;
   }

   private Throwable recreateGenericThrowable(String impl, String msg, MarshallableThrowable t) {
      Throwable cause = t == null ? null : t.get();
      try {
         Class<?> clazz = Class.forName(impl);

         Object retVal;
         if (cause == null && msg == null) {
            retVal = create(clazz, c -> getInstance(c));
         } else if (cause == null) {
            retVal = create(clazz, c -> getInstance(c, msg), String.class);
         } else if (msg == null) {
            retVal = create(clazz, c -> getInstance(c, cause), Throwable.class);
         } else {
            retVal = create(clazz, c -> getInstance(c, msg, cause), String.class, Throwable.class);
            if (retVal == null) {
               retVal = create(clazz, c -> getInstance(c, cause), Throwable.class);
            }
         }
         return (Throwable) retVal;
      } catch (ClassNotFoundException e) {
         throw new MarshallingException(e);
      }
   }

   private Object create(Class<?> clazz, Function<Constructor<?>, ?> builder, Class<?>... args) {
      try {
         Constructor<?> ctor = clazz.getConstructor(args);
         return builder.apply(ctor);
      } catch (NoSuchMethodException e) {
         return null;
      }
   }

   private Object getInstance(Constructor<?> constructor, Object... args) {
      try {
         return constructor.newInstance(args);
      } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
         throw new MarshallingException(e);
      }
   }

   @Override
   public String toString() {
      return get().toString();
   }

   // TODO add known types
   enum Type {
      ALL_OWNERS_LOST,
      CACHE_EXCEPTION,
      CACHE_LISTENER,
      UNKNOWN
   }
}
