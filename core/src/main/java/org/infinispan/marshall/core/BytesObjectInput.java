package org.infinispan.marshall.core;

import java.io.IOException;
import java.io.ObjectInput;

import org.infinispan.marshall.core.impl.AbstractBytesObjectInput;

/**
 * Array backed {@link ObjectInput} implementation that utilises the {@link GlobalMarshaller} to read objects.
 */
final class BytesObjectInput extends AbstractBytesObjectInput {

   final GlobalMarshaller marshaller;

   private BytesObjectInput(byte[] bytes, int offset, GlobalMarshaller marshaller) {
      super(bytes, offset);
      this.marshaller = marshaller;
   }

   static BytesObjectInput from(byte[] bytes, GlobalMarshaller marshaller) {
      return from(bytes, 0, marshaller);
   }

   static BytesObjectInput from(byte[] bytes, int offset, GlobalMarshaller marshaller) {
      return new BytesObjectInput(bytes, offset, marshaller);
   }

   @Override
   public Object readObject() throws ClassNotFoundException, IOException {
      return marshaller.readNullableObject(this);
   }
}
