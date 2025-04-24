package org.infinispan.container.entries;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.container.versioning.EntryVersion;
import org.infinispan.container.versioning.SimpleClusteredVersion;
import org.infinispan.metadata.Metadata;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.remoting.transport.Address;
import org.infinispan.remoting.transport.jgroups.JGroupsTopologyAwareAddress;
import org.jgroups.util.ExtendedUUID;
import org.jgroups.util.UUID;

/**
 * This is a metadata type used by scattered cache during state transfer. The address points to node which has last
 * known version of given entry: During key transfer RemoteMetadata is created and overwritten if another response
 * with higher version comes. During value transfer the address is already final and we request the value + metadata
 * only from this node.
 *
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
@ProtoTypeId(ProtoStreamTypeIds.REMOTE_METADATA)
public class RemoteMetadata implements Metadata {
   private final JGroupsAddress address;
   private final SimpleClusteredVersion version;
   private final JGroupsTopologyAwareAddress topologyAwareAddress;

   public RemoteMetadata(Address address, EntryVersion version) {
      this(null, (SimpleClusteredVersion) version, (JGroupsTopologyAwareAddress) address);
   }

   @ProtoFactory
   RemoteMetadata(JGroupsAddress address, SimpleClusteredVersion version, JGroupsTopologyAwareAddress topologyAwareAddress) {
      this.address = null;
      this.version = version;
      assert address != null || topologyAwareAddress != null;
      if (address != null) {
         // Address should always be UUID based as Anchored keys only supported with Replicated cache modes
         var uuid = (UUID) address.address;
         this.topologyAwareAddress = new JGroupsTopologyAwareAddress(new ExtendedUUID(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits()));
      } else {
         this.topologyAwareAddress = topologyAwareAddress;
      }
   }

   @ProtoField(number = 1, javaType = JGroupsAddress.class)
   public Address getAddress() {
      assert address == null;
      return null;
   }

   @Override
   public long lifespan() {
      return -1;
   }

   @Override
   public long maxIdle() {
      return -1;
   }

   @Override
   @ProtoField(number = 2, javaType = SimpleClusteredVersion.class)
   public EntryVersion version() {
      return version;
   }

   @ProtoField(number = 3, javaType = JGroupsTopologyAwareAddress.class)
   public Address getTopologyAwareAddress() {
      return topologyAwareAddress;
   }

   @Override
   public Builder builder() {
      throw new UnsupportedOperationException();
   }

   @Override
   public String toString() {
      final StringBuilder sb = new StringBuilder("RemoteMetadata{");
      sb.append("address=").append(address);
      sb.append(", version=").append(version);
      sb.append('}');
      return sb.toString();
   }
}
