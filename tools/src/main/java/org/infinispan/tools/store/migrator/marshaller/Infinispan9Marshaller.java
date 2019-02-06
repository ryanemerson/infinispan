package org.infinispan.tools.store.migrator.marshaller;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.util.Map;

import org.infinispan.commons.marshall.AdvancedExternalizer;
import org.infinispan.tools.store.migrator.marshaller.externalizers.infinispan9.ExternalizerTable;

/**
 * Legacy marshaller for reading from Infinispan 9.x stores.
 *
 * @author Ryan Emerson
 * @since 10.0
 */
public class Infinispan9Marshaller extends AbstractLegacyMarshaller {

   Infinispan9Marshaller(Map<Integer, ? extends AdvancedExternalizer<?>> userExts) {
      super(new ExternalizerTable(userExts));
   }

   @Override
   public ObjectInput startObjectInput(InputStream is, boolean isReentrant) throws IOException {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public Object objectFromByteBuffer(byte[] buf, int offset, int length) throws IOException, ClassNotFoundException {
      return null;  // TODO: Customise this generated block
   }
}
