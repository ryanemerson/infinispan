package org.infinispan.tools.store.migrator.marshaller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.util.Map;

import org.infinispan.commons.marshall.AdvancedExternalizer;
import org.infinispan.commons.marshall.StreamingMarshaller;
import org.infinispan.tools.store.migrator.marshaller.externalizers.infinispan8.ExternalizerTable;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * LegacyVersionAwareMarshaller that is used to read bytes marshalled using Infinispan 8.x. This is useful for providing
 * a migration path from 8.x stores.
 */
public class Infinispan8Marshaller extends AbstractLegacyMarshaller implements StreamingMarshaller {
   private static final Log log = LogFactory.getLog(Infinispan8Marshaller.class);

   Infinispan8Marshaller(Map<Integer, ? extends AdvancedExternalizer<?>> userExts) {
      super(new ExternalizerTable(userExts));
   }

   @Override
   public Object objectFromByteBuffer(byte[] bytes, int offset, int len) throws IOException, ClassNotFoundException {
      ByteArrayInputStream is = new ByteArrayInputStream(bytes, offset, len);
      ObjectInput in = startObjectInput(is, false);
      Object o;
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
      } catch (Exception e) {
         finishObjectInput(in);
         log.unableToReadVersionId();
         throw new IOException("Unable to read version id from first two bytes of stream: " + e.getMessage());
      }
      return in;
   }
}
