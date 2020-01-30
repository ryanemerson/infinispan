package org.infinispan.marshall.core;

import static org.infinispan.util.logging.Log.CONTAINER;

import org.infinispan.commons.io.ByteBuffer;
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

   public GlobalMarshaller() {
      super(CONTAINER);
   }

   @Override
   public ImmutableSerializationContext getSerializationContext() {
      return ctxRegistry.running().getGlobalCtx();
   }

   @Override
   protected ByteBuffer objectToBuffer(Object o, int estimatedSize) {
      if (o == null)
         return null;

      Class<?> clazz = o.getClass();
      if (clazz.isSynthetic()) {
         o = MarshallableLambda.create(o);
      } else if (o instanceof Throwable) {
         o = MarshallableThrowable.create((Throwable) o);
      }

      return super.objectToBuffer(o, estimatedSize);
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
