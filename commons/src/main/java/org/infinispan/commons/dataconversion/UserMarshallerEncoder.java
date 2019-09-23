package org.infinispan.commons.dataconversion;

import org.infinispan.commons.marshall.Marshaller;

/**
 * @author Ryan Emerson
 * @since 10.0
 */
public class UserMarshallerEncoder extends MarshallerEncoder {

   public UserMarshallerEncoder(Marshaller marshaller) {
      super(marshaller);
   }

   @Override
   public MediaType getStorageFormat() {
      return marshaller.mediaType();
   }

   @Override
   public short id() {
      return EncoderIds.USER_MARSHALLER;
   }
}
