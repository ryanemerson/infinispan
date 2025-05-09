package org.infinispan.remoting.transport.jgroups;

import org.infinispan.remoting.transport.Address;
import org.jgroups.util.ExtendedUUID;

public class JGroupsXSiteAddress implements Address {
   private final ExtendedUUID address;

   public JGroupsXSiteAddress(ExtendedUUID address) {
      this.address = address;
   }

   @Override
   public int compareTo(org.jgroups.Address o) {
      return address.compareTo(o);
   }

   public org.jgroups.Address getJGroupsAddress() {
      return address;
   }
}
