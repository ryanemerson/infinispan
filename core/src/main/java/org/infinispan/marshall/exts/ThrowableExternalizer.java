package org.infinispan.marshall.exts;


import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.infinispan.commons.CacheException;
import org.infinispan.commons.marshall.AdvancedExternalizer;
import org.infinispan.commons.util.Util;
import org.infinispan.marshall.core.Ids;
import org.infinispan.marshall.core.MarshallingException;
import org.infinispan.statetransfer.OutdatedTopologyException;

public class ThrowableExternalizer implements AdvancedExternalizer<Throwable> {

   // TODO update to support all known internal exceptions
   public static ThrowableExternalizer INSTANCE = new ThrowableExternalizer();

   private static final short UNKNOWN = -1;
   // Infinispan Exceptions
   private static final short ALL_OWNERS_LOST = 0;
   private static final short AVAILABILITY = 1;
   private static final short CACHE_CONFIGURATION = 2;
   private static final short CACHE_EXCEPTION = 3;
   private static final short CACHE_LISTENER = 4;
   private static final short CACHE_UNREACHABLE = 5;
   private static final short CACHE_JOIN = 6;
   private static final short CONCURRENT_CHANGE = 7;
   private static final short CONTAINER_FULL = 8;
   private static final short DEADLOCK_DETECTED = 9;
   private static final short EMBEDDED_CACHE_STARTUP = 10;
   private static final short ENCODING = 11;
   private static final short INCORRECT_LISTENER = 12;
   private static final short ILLEGAL_LIFECYLE = 13;
   private static final short INVALID_CACHE_USAGE = 14;
   private static final short INVALID_TX = 15;
   private static final short MARSHALLING = 16;
   private static final short NOT_SERIALIZABLE_CONFIGURATION = 17; // Probably not needed, deprecate?
   private static final short OUTDATED_TOPOLOGY = 18;
   private static final short PERSISTENCE = 19;
   private static final short REMOTE = 20;
   private static final short RPC = 21;
   private static final short SUSPECT = 22;
   private static final short TIMEOUT = 23;
   private static final short WRITE_SKEW = 24;
   private final Map<Class<?>, Short> numbers = new HashMap<>(24);

   public ThrowableExternalizer() {
      numbers.put(CacheException.class, CACHE_EXCEPTION);
      numbers.put(CacheException.class, OUTDATED_TOPOLOGY);
   }

   @Override
   public Set<Class<? extends Throwable>> getTypeClasses() {
      return Util.asSet(Throwable.class);
   }

   @Override
   public Integer getId() {
      return Ids.EXCEPTIONS;
   }

   @Override
   public void writeObject(ObjectOutput out, Throwable t) throws IOException {
      short id = numbers.getOrDefault(t.getClass(), UNKNOWN);
      out.writeShort(id);

      switch (id) {
         case OUTDATED_TOPOLOGY:
            OutdatedTopologyException e = (OutdatedTopologyException) t;
            out.writeBoolean(e.topologyIdDelta == 0);
            break;
         default:
            writeGenericThrowable(out, t);
            break;
      }
   }

   @Override
   public Throwable readObject(ObjectInput in) throws IOException, ClassNotFoundException {
      short id = in.readShort();
      switch (id) {
         case OUTDATED_TOPOLOGY:
            boolean retryNextTopology = in.readBoolean();
            return retryNextTopology ? OutdatedTopologyException.RETRY_NEXT_TOPOLOGY : OutdatedTopologyException.RETRY_SAME_TOPOLOGY;
         default:
            return readGenericThrowable(in);
      }
   }


   private void writeGenericThrowable(ObjectOutput out, Throwable t) throws IOException {
      out.writeUTF(t.getClass().getName());
      out.writeUTF(t.getMessage());
      out.writeObject(t.getCause());
   }

   private Throwable readGenericThrowable(ObjectInput in) throws IOException, ClassNotFoundException{
      String impl = in.readUTF();
      String msg = in.readUTF();
      Throwable t = (Throwable) in.readObject();
      try {
         Constructor<?> ctor = Class.forName(impl).getConstructor(String.class, Throwable.class);
         return (Throwable) ctor.newInstance(new Object[]{msg, t});
      } catch (IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
         throw new MarshallingException(e);
      }
   }
}
