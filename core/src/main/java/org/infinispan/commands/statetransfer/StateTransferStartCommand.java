package org.infinispan.commands.statetransfer;

import java.util.Set;
import java.util.concurrent.CompletionStage;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.commons.util.IntSet;
import org.infinispan.commons.util.IntSets;
import org.infinispan.commons.util.concurrent.CompletableFutures;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.statetransfer.StateProvider;
import org.infinispan.util.ByteString;

/**
 * Start state transfer.
 *
 * @author Ryan Emerson
 * @since 11.0
 */
@ProtoTypeId(ProtoStreamTypeIds.STATE_TRANSFER_START_COMMAND)
public class StateTransferStartCommand extends AbstractStateTransferCommand {

   public static final byte COMMAND_ID = 116;

   @ProtoFactory
   StateTransferStartCommand(ByteString cacheName, int topologyId, Set<Integer> segmentsSet) {
      this(cacheName, topologyId, segmentsSet == null ? null : IntSets.from(segmentsSet));
   }

   public StateTransferStartCommand(ByteString cacheName, int topologyId, IntSet segments) {
      super(COMMAND_ID, cacheName, topologyId, segments);
   }

   @Override
   public CompletionStage<?> invokeAsync(ComponentRegistry registry) throws Throwable {
      StateProvider stateProvider = registry.getStateTransferManager().getStateProvider();
      stateProvider.startOutboundTransfer(origin, topologyId, segments, true);
      return CompletableFutures.completedNull();
   }

   @Override
   public String toString() {
      return "StateTransferStartCommand{" +
            "topologyId=" + topologyId +
            ", segments=" + segments +
            ", cacheName=" + cacheName +
            '}';
   }
}
