package org.infinispan.remoting.transport.jgroups;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jgroups.Address;
import org.jgroups.util.ExtendedUUID;
import org.jgroups.util.NameCache;
import org.jgroups.util.UUID;

/**
 * Cache JGroupsAddress instances
 *
 * @author Dan Berindei
 * @since 7.0
 */
public class JGroupsAddressCache {
   private static final ConcurrentMap<UUID, JGroupsAddress> uuidAddressCache = new ConcurrentHashMap<>();
   private static final ConcurrentMap<Address, JGroupsAddress> addressCache =
         new ConcurrentHashMap<>();

   public static org.infinispan.remoting.transport.Address fromJGroupsAddress(Address jgroupsAddress) {
      if (!(jgroupsAddress instanceof UUID uuid))
         throw new IllegalStateException("JGroups Address must be UUID based");

      // New entries are rarely added after startup, but computeIfAbsent synchronizes every time
      JGroupsAddress ispnAddress = addressCache.get(jgroupsAddress);
      if (ispnAddress != null) {
         return ispnAddress;
      }

      JGroupsAddress addr = addressCache.computeIfAbsent(jgroupsAddress, ignore ->
            jgroupsAddress instanceof ExtendedUUID ?
                  new JGroupsTopologyAwareAddress((ExtendedUUID) jgroupsAddress) :
                  new JGroupsAddress(jgroupsAddress));

      uuidAddressCache.putIfAbsent(uuid, addr);
      return addr;
   }

   public static JGroupsAddress fromUUID(long mostSignificantBytes, long leastSignificantBytes) {
      return uuidAddressCache.get(new UUID(mostSignificantBytes, leastSignificantBytes));
   }

   static void pruneAddressCache() {
      // Prune the JGroups addresses & LocalUUIDs no longer in the UUID cache from the address cache
      addressCache.forEach((address, ignore) -> {
         if (NameCache.get(address) == null) {
            addressCache.remove(address);
            uuidAddressCache.remove((UUID) address);
         }
      });
   }
}
