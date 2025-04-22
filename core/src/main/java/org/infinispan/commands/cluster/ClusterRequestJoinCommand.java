package org.infinispan.commands.cluster;

import java.util.concurrent.CompletionStage;

import org.infinispan.commands.topology.AbstractCacheControlCommand;
import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.topology.ClusterStatusResponse;
import org.infinispan.upgrade.ManagerVersion;

/**
 * Command to initiate the cluster handshake procedure with the coordinator.
 *
 * @author Ryan Emerson
 * @since 16.0
 */
@ProtoTypeId(ProtoStreamTypeIds.CLUSTER_REQUEST_JOIN_COMMAND)
public class ClusterRequestJoinCommand extends AbstractCacheControlCommand {

   @ProtoField(1)
   final ManagerVersion version;

   @ProtoFactory
   public ClusterRequestJoinCommand(ManagerVersion version) {
      this.version = version;
   }

   @Override
   public CompletionStage<ClusterStatusResponse> invokeAsync(GlobalComponentRegistry gcr) throws Throwable {
      return gcr.getClusterTopologyManager().initiateNodeJoin(origin, version);
   }
}
