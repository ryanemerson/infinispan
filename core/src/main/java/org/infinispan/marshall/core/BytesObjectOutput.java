package org.infinispan.marshall.core;

import java.io.IOException;
import java.io.ObjectOutput;

import org.infinispan.marshall.core.impl.AbstractBytesObjectOutput;

/**
 * Array backed {@link ObjectOutput} implementation that utilises the {@link GlobalMarshaller} to write objects.
 */
final class BytesObjectOutput extends AbstractBytesObjectOutput {

   private final GlobalMarshaller marshaller;

   BytesObjectOutput(int size, GlobalMarshaller marshaller) {
      super(size);
      this.marshaller = marshaller;
   }

   @Override
   public void writeObject(Object obj) throws IOException {
      marshaller.writeNullableObject(obj, this);
   }

   int size() {
      return pos;
   }
}
