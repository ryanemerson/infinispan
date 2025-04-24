package org.infinispan.container.entries;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import org.infinispan.commons.marshall.MarshallingException;
import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.remoting.transport.Address;

/**
 * Legacy Address implementation required for backwards-compatibility only.
 * <p>
 * Class cannot be created as RemoteMetdata.JGroupsAddress due to https://github.com/infinispan/protostream/issues/457
 */
@ProtoTypeId(ProtoStreamTypeIds.JGROUPS_ADDRESS)
public class JGroupsAddress implements Address {

   final org.jgroups.Address address;

   JGroupsAddress(final org.jgroups.Address address) {
      if (address == null)
         throw new IllegalArgumentException("Address shall not be null");
      this.address = address;
   }

   @ProtoFactory
   static JGroupsAddress protoFactory(byte[] bytes) throws IOException {
      try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
         org.jgroups.Address address = org.jgroups.util.Util.readAddress(in);
         return new JGroupsAddress(address);
      } catch (ClassNotFoundException e) {
         throw new MarshallingException(e);
      }
   }

   @ProtoField(1)
   byte[] getBytes() {
      throw new IllegalStateException("Class should never be marshalled as it exists for backwards-compatibility only");
   }

   @Override
   public int compareTo(Address o) {
      JGroupsAddress oa = (JGroupsAddress) o;
      return address.compareTo(oa.address);
   }
}
