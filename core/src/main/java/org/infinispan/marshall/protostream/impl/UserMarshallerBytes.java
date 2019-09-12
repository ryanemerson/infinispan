package org.infinispan.marshall.protostream.impl;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * A wrapper message used by ProtoStream Marshallers to wrap bytes generated by a non-protostream user marshaller.
 *
 * @author Ryan Emerson
 * @since 10.0
 */
@ProtoTypeId(ProtoStreamTypeIds.USER_MARSHALLER_BYTES)
public class UserMarshallerBytes {

   @ProtoField(number = 1)
   final byte[] bytes;

   @ProtoFactory
   public UserMarshallerBytes(byte[] bytes) {
      this.bytes = bytes;
   }

   public byte[] getBytes() {
      return bytes;
   }
}
