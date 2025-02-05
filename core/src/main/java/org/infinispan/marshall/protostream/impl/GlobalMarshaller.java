package org.infinispan.marshall.protostream.impl;

import static org.infinispan.util.logging.Log.CONTAINER;

import java.io.IOException;

import org.infinispan.commons.marshall.ImmutableProtoStreamMarshaller;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.factories.annotations.Stop;
import org.infinispan.marshall.core.impl.DelegatingUserMarshaller;
import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.impl.LazyByteArrayOutputStream;

/**
 * A globally-scoped marshaller for cluster communication.
 *
 * @author Ryan Emerson
 * @since 16.0
 */
// TODO add support for reusing instances similar to InstanceReusingAdvancedExternalizer?
public class GlobalMarshaller extends AbstractInternalProtoStreamMarshaller {

   @Inject
   GlobalComponentRegistry gcr;

   private ClassLoader classLoader;

   public GlobalMarshaller() {
      super(CONTAINER);
   }

   @Override
   public void start() {
      super.start();
      classLoader = gcr.getGlobalConfiguration().classLoader();
      skipUserMarshaller = ((DelegatingUserMarshaller) userMarshaller).getDelegate() instanceof ImmutableProtoStreamMarshaller;
   }

   @Override
   @Stop() // Stop after transport to avoid send/receive and marshaller not being ready
   public void stop() {
      userMarshaller.stop();
   }

   @Override
   public ImmutableSerializationContext getSerializationContext() {
      return ctxRegistry.getGlobalCtx();
   }

   @Override
   public boolean isMarshallableWithProtoStream(Object o) {
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

   protected LazyByteArrayOutputStream objectToOutputStream(Object obj, int estimatedSize) {
      Class<?> clazz = obj.getClass();
      if (clazz.isSynthetic()) {
         obj = MarshallableLambda.create(obj);
      } else if (obj instanceof Throwable && !isMarshallable(obj)) {
         obj = MarshallableThrowable.create((Throwable) obj);
      }
      return super.objectToOutputStream(obj, estimatedSize);
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
      return super.objectFromByteBuffer(buf, offset, length);
   }
}
