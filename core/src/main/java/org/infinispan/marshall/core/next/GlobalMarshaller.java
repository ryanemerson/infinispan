package org.infinispan.marshall.core.next;

import static org.infinispan.util.logging.Log.CONTAINER;

import java.io.IOException;

import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.marshall.protostream.impl.AbstractInternalProtoStreamMarshaller;
import org.infinispan.marshall.protostream.impl.MarshallableLambda;
import org.infinispan.marshall.protostream.impl.MarshallableThrowable;
import org.infinispan.marshall.protostream.impl.SerializationContextRegistry;
import org.infinispan.protostream.ImmutableSerializationContext;

/**
 * A globally-scoped marshaller for cluster communication.
 *
 * @author Ryan Emerson
 * @since 11.0
 */
// TODO add support for reusing instances similar to InstanceReusingAdvancedExternalizer?
public class GlobalMarshaller extends AbstractInternalProtoStreamMarshaller {

   GlobalComponentRegistry gcr;

   private ClassLoader classLoader;

   public GlobalMarshaller() {
      super(CONTAINER);
   }

   // TODO remove when DelegatingGlobalMarshaller no longer required
   public void init(GlobalComponentRegistry gcr, SerializationContextRegistry ctxRegistry, Marshaller userMarshaller) {
      this.gcr = gcr;
      this.ctxRegistry = ctxRegistry;
      this.userMarshaller = userMarshaller;
      this.classLoader = gcr.getGlobalConfiguration().classLoader();
   }

   @Override
   public void start() {
      super.start();
      classLoader = gcr.getGlobalConfiguration().classLoader();
   }

   @Override
   public ImmutableSerializationContext getSerializationContext() {
      return ctxRegistry.getGlobalCtx();
   }

   @Override
   protected boolean isMarshallableWithProtoStream(Object o) {
      return o instanceof String ||
            o instanceof Long ||
            o instanceof Integer ||
            o instanceof Double ||
            o instanceof Float ||
            o instanceof Boolean ||
            o instanceof byte[] ||
            o instanceof Byte ||
            o instanceof Short ||
            o instanceof Character ||
            o instanceof java.util.Date ||
            o instanceof java.time.Instant ||
            super.isMarshallableWithProtoStream(o);
   }

   @Override
   public byte[] objectToByteBuffer(Object obj, int estimatedSize) {
      if (obj == null)
         return null;

      Class<?> clazz = obj.getClass();
      if (clazz.isSynthetic()) {
         obj = MarshallableLambda.create(obj);
      } else if (obj instanceof Throwable && !isMarshallable(obj)) {
         obj = MarshallableThrowable.create((Throwable) obj);
      }

      return super.objectToByteBuffer(obj, estimatedSize);
   }

   @Override
   protected Object unwrapAndInit(Object o) {
      if (o instanceof MarshallableLambda) {
         return ((MarshallableLambda) o).unwrap(classLoader);
      } else if (o instanceof MarshallableThrowable) {
         return ((MarshallableThrowable) o).get();
      }

      return super.unwrapAndInit(o);
   }

   @Override
   public Object objectFromByteBuffer(byte[] buf, int offset, int length) throws IOException {
      var obj = super.objectFromByteBuffer(buf, offset, length);
      if (obj.getClass().getPackage().getName().startsWith("org.infinispan.query") || obj.getClass().getPackage().getName().startsWith("org.apache.lucene"))
         System.err.println("Unmarshall: " + obj);
      return obj;
   }
}
