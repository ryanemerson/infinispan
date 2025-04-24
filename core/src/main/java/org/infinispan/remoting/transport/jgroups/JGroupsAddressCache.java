package org.infinispan.remoting.transport.jgroups;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jgroups.Address;
import org.jgroups.util.ExtendedUUID;
import org.jgroups.util.NameCache;
import org.jgroups.util.UUID;

/**
 * Cache JGroupsTopologyAwareAddress instances
 *
 * @author Dan Berindei
 * @since 7.0
 */
public class JGroupsAddressCache {
   private static final ConcurrentMap<Address, JGroupsTopologyAwareAddress> addressCache =
         new ConcurrentHashMap<>();

   public static org.infinispan.remoting.transport.Address fromJGroupsTopologyAwareAddress(Address JGroupsTopologyAwareAddress) {
      // New entries are rarely added after startup, but computeIfAbsent synchronizes every time
      JGroupsTopologyAwareAddress ispnAddress = addressCache.get(JGroupsTopologyAwareAddress);
      if (ispnAddress != null) {
         return ispnAddress;
      }
      return addressCache.computeIfAbsent(JGroupsTopologyAwareAddress, ignore -> {
         if (JGroupsTopologyAwareAddress instanceof ExtendedUUID) {
            return new JGroupsTopologyAwareAddress((ExtendedUUID) JGroupsTopologyAwareAddress);
         } else if (JGroupsTopologyAwareAddress instanceof UUID uuid) {
            return new JGroupsTopologyAwareAddress(new ExtendedUUID(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits()));
         } else {
            throw new IllegalStateException("Unexpected address type: " + JGroupsTopologyAwareAddress.getClass().getName());
         }
      });
   }

   static void pruneAddressCache() {
      // Prune the JGroups addresses & LocalUUIDs no longer in the UUID cache from the our address cache
      addressCache.forEach((address, ignore) -> {
         if (NameCache.get(address) == null) {
            addressCache.remove(address);
         }
      });
   }
}
