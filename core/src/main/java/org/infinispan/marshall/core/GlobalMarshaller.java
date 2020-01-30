package org.infinispan.marshall.core;

import static org.infinispan.util.logging.Log.CONTAINER;

import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.factories.scopes.Scope;
import org.infinispan.factories.scopes.Scopes;
import org.infinispan.marshall.protostream.impl.AbstractInternalProtoStreamMarshaller;
import org.infinispan.marshall.protostream.impl.MarshallableLambda;
import org.infinispan.marshall.protostream.impl.MarshallableThrowable;
import org.infinispan.protostream.ImmutableSerializationContext;

/**
 * A globally-scoped marshaller for cluster communication.
 *
 * @author Ryan Emerson
 * @since 11.0
 */
// TODO add support for reusing instances similar to InstanceReusingAdvancedExternalizer?
@Scope(Scopes.GLOBAL)
public class GlobalMarshaller extends AbstractInternalProtoStreamMarshaller {

   @Inject GlobalComponentRegistry gcr;

   private ClassLoader classLoader;

   public GlobalMarshaller() {
      super(CONTAINER);
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
   public byte[] objectToByteBuffer(Object obj, int estimatedSize) {
      if (obj == null)
         return null;

      Class<?> clazz = obj.getClass();
      if (clazz.isSynthetic()) {
         obj = MarshallableLambda.create(obj);
      } else if (obj instanceof Throwable) {
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
}
