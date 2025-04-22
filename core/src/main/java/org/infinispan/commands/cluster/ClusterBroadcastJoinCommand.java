package org.infinispan.commands.cluster;

import java.util.concurrent.CompletionStage;

import org.infinispan.commands.topology.AbstractCacheControlCommand;
import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.marshall.protostream.impl.WrappedMessages;
import org.infinispan.protostream.WrappedMessage;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.remoting.transport.Address;
import org.infinispan.upgrade.ManagerVersion;

/**
 * Broadcast metadata of a node that is attempting to join the cluster.
 *
 * @author Ryan Emerson
 * @since 16.0
 */
@ProtoTypeId(ProtoStreamTypeIds.CLUSTER_BROADCAST_JOIN_COMMAND)
public class ClusterBroadcastJoinCommand extends AbstractCacheControlCommand {

   @ProtoField(1)
   final WrappedMessage address;

   @ProtoField(2)
   final ManagerVersion version;

   public ClusterBroadcastJoinCommand(Address address, ManagerVersion version) {
      this(new WrappedMessage(address), version);
   }

   @ProtoFactory
   ClusterBroadcastJoinCommand(WrappedMessage address, ManagerVersion version) {
      this.address = address;
      this.version = version;
   }

   @Override
   public CompletionStage<Void> invokeAsync(GlobalComponentRegistry gcr) throws Throwable {
      return gcr.getClusterTopologyManager().handleNodeJoin(WrappedMessages.unwrap(address), version);
   }
}
