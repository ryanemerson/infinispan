package org.infinispan.tools.jdbc.migrator.marshaller;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;

import org.infinispan.commons.io.ByteBuffer;
import org.infinispan.commons.io.ByteBufferImpl;
import org.infinispan.commons.io.ExposedByteArrayOutputStream;
import org.infinispan.commons.marshall.AbstractMarshaller;
import org.infinispan.commons.marshall.NotSerializableException;
import org.infinispan.commons.marshall.StreamingMarshaller;
import org.infinispan.commons.marshall.jboss.DefaultContextClassResolver;
import org.infinispan.commons.marshall.jboss.SerializeWithExtFactory;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.marshall.core.JBossMarshaller;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * Legacy VersionAwareMarshaller that reads and ignores the short previously writte
 * A delegate to various other marshallers like {@link JBossMarshaller}. This delegating marshaller adds versioning
 * information to the stream when marshalling objects and is able to pick the appropriate marshaller to delegate to
 * based on the versioning information when unmarshalling objects.
 *
 * @author Manik Surtani
 * @author Galder Zamarreño
 * @since 4.0
 */
public class VersionAwareMarshaller extends AbstractMarshaller implements StreamingMarshaller {
   private final LegacyJBossMarshaller defaultMarshaller = new LegacyJBossMarshaller();

   @Override
   public void stop() {
   }

   @Override
   public void start() {
   }

   @Override
   protected ByteBuffer objectToBuffer(Object obj, int estimatedSize) throws IOException, InterruptedException {
      throw new UnsupportedOperationException();
   }

   @Override
   public Object objectFromByteBuffer(byte[] bytes, int offset, int len) throws IOException, ClassNotFoundException {
      ByteArrayInputStream is = new ByteArrayInputStream(bytes, offset, len);
      ObjectInput in = startObjectInput(is, false);
      Object o = null;
      try {
         o = defaultMarshaller.objectFromObjectStream(in);
      } finally {
         finishObjectInput(in);
      }
      return o;
   }

   @Override
   public ObjectInput startObjectInput(InputStream is, boolean isReentrant) throws IOException {
      ObjectInput in = defaultMarshaller.startObjectInput(is, isReentrant);
      try {
         in.readShort();
      }
      catch (Exception e) {
         finishObjectInput(in);
         throw new IOException("Unable to read version id from first two bytes of stream: " + e.getMessage());
      }
      return in;
   }

   @Override
   public void finishObjectInput(ObjectInput oi) {
      defaultMarshaller.finishObjectInput(oi);
   }

   @Override
   public boolean isMarshallable(Object o) throws Exception {
      return defaultMarshaller.isMarshallable(o);
   }

   @Override
   public Object objectFromObjectStream(ObjectInput in) throws IOException, ClassNotFoundException, InterruptedException {
      throw new UnsupportedOperationException();
   }

   @Override
   public ObjectOutput startObjectOutput(OutputStream os, boolean isReentrant, final int estimatedSize) throws IOException {
      throw new UnsupportedOperationException();
   }

   @Override
   public void finishObjectOutput(ObjectOutput oo) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void objectToObjectStream(Object obj, ObjectOutput out) throws IOException {
      throw new UnsupportedOperationException();
   }
}
