package org.infinispan.commands.statetransfer;

import java.util.Set;
import java.util.concurrent.CompletionStage;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.commons.util.IntSet;
import org.infinispan.commons.util.IntSets;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.statetransfer.StateProvider;
import org.infinispan.util.ByteString;
import org.infinispan.util.concurrent.CompletableFutures;

/**
 * Start conflict resolution.
 *
 * @author Ryan Emerson
 * @since 11.0
 */
@ProtoTypeId(ProtoStreamTypeIds.CONFLICT_RESOLUTION_START_COMMAND)
public class ConflictResolutionStartCommand extends AbstractStateTransferCommand {

   public static final byte COMMAND_ID = 112;

   @ProtoFactory
   ConflictResolutionStartCommand(ByteString cacheName, int topologyId, Set<Integer> segmentsWorkaround) {
      this(cacheName, topologyId, segmentsWorkaround == null ? null : IntSets.from(segmentsWorkaround));
   }

   public ConflictResolutionStartCommand(ByteString cacheName, int topologyId, IntSet segments) {
//   @ProtoFactory
      super(COMMAND_ID, cacheName, topologyId, segments);
   }

   @Override
   public CompletionStage<?> invokeAsync(ComponentRegistry registry) throws Throwable {
      StateProvider stateProvider = registry.getStateTransferManager().getStateProvider();
      stateProvider.startOutboundTransfer(origin, topologyId, segments, false);
      return CompletableFutures.completedNull();
   }

   @Override
   public String toString() {
      return "ConflictResolutionStartCommand{" +
            "topologyId=" + topologyId +
            ", segments=" + segments +
            ", cacheName=" + cacheName +
            '}';
   }
}
