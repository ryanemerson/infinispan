package org.infinispan.commands.topology;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.topology.ManagerStatusResponse;

/**
 * Retrieve status information from the coordinator on initial startup
 *
 * @author Ryan Emerson
 * @since 16.0
 */
@ProtoTypeId(ProtoStreamTypeIds.MANAGER_STATUS_COMMAND)
public class ManagerStatusCommand extends AbstractCacheControlCommand {

   @ProtoFactory
   public ManagerStatusCommand() {}

   @Override
   public CompletionStage<ManagerStatusResponse> invokeAsync(GlobalComponentRegistry gcr) throws Throwable {
      var ctm = gcr.getClusterTopologyManager();
      return CompletableFuture.completedFuture(
            new ManagerStatusResponse(null, ctm.isRebalancingEnabled(), ctm.isMixedCluster(), ctm.getOldestMember())
      );
   }
}
