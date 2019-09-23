package org.infinispan.commons.dataconversion;

import org.infinispan.commons.marshall.Marshaller;

/**
 * @since 9.1
 */
public class GenericJbossMarshallerEncoder extends MarshallerEncoder {

   public GenericJbossMarshallerEncoder(Marshaller marshaller) {
      super(marshaller);
   }

   @Override
   public MediaType getStorageFormat() {
      return MediaType.APPLICATION_JBOSS_MARSHALLING;
   }

   @Override
   public short id() {
      return EncoderIds.GENERIC_MARSHALLER;
   }
}
