package org.infinispan.marshaller.kryo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.infinispan.commons.io.ByteBuffer;
import org.infinispan.commons.io.ByteBufferImpl;
import org.infinispan.commons.marshall.AbstractMarshaller;
import org.infinispan.commons.marshall.Marshaller;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * @author Ryan Emerson
 * @since 9.0
 */
public class KryoMarshaller extends AbstractMarshaller {

   private final Kryo kryo = new Kryo();

   public Kryo getKryo() {
      return kryo;
   }

   @Override
   public Object objectFromByteBuffer(byte[] bytes, int offset, int length) throws IOException, ClassNotFoundException {
      try (Input input = new Input(bytes, offset, length)) {
         return kryo.readClassAndObject(input);
      }
   }

   @Override
   protected ByteBuffer objectToBuffer(Object obj, int estimatedSize) throws IOException, InterruptedException {
      if (obj instanceof byte[]) {
         byte[] bytes = (byte[]) obj;
         return new ByteBufferImpl(bytes, 0, bytes.length);
      }

      try (Output output = new Output(new ByteArrayOutputStream(estimatedSize), estimatedSize)) {
         kryo.writeClassAndObject(output, obj);
         byte[] bytes = output.toBytes();
         return new ByteBufferImpl(bytes, 0, bytes.length);
      }
   }

   @Override
   public boolean isMarshallable(Object obj) throws Exception {
      try {
         objectToBuffer(obj);
         return true;
      } catch (Throwable t) {
         return false;
      }
   }
}
