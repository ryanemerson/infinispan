package org.infinispan.remoting.transport.jgroups;

import org.jgroups.Address;
import org.jgroups.protocols.relay.SiteUUID;

public class JGroupsXSiteAddress implements org.infinispan.remoting.transport.Address {

   private final SiteUUID siteUUID;

   JGroupsXSiteAddress(SiteUUID siteUUID) {
      this.siteUUID = siteUUID;
   }

   @Override
   public int compareTo(Address o) {
      return siteUUID.compareTo(o);
   }
}
