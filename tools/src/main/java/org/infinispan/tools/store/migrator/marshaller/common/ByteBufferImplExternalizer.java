package org.infinispan.tools.store.migrator.marshaller.common;

import java.io.IOException;
import java.io.ObjectInput;
import java.util.Collections;
import java.util.Set;

import org.infinispan.commons.io.ByteBufferImpl;
import org.infinispan.commons.io.UnsignedNumeric;
import org.infinispan.commons.marshall.Ids;

public class ByteBufferImplExternalizer extends AbstractMigratorExternalizer<ByteBufferImpl> {
   @Override
   public ByteBufferImpl readObject(ObjectInput input) throws IOException, ClassNotFoundException {
      int length = UnsignedNumeric.readUnsignedInt(input);
      byte[] data = new byte[length];
      input.readFully(data, 0, length);
      return new ByteBufferImpl(data, 0, length);
   }

   @Override
   public Integer getId() {
      return Ids.BYTE_BUFFER;
   }

   @Override
   public Set<Class<? extends ByteBufferImpl>> getTypeClasses() {
      return Collections.singleton(ByteBufferImpl.class);
   }
}
