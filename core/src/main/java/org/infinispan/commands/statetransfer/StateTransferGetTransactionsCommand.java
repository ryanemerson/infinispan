package org.infinispan.commands.statetransfer;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.commons.util.IntSet;
import org.infinispan.commons.util.IntSets;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.statetransfer.StateProvider;
import org.infinispan.statetransfer.TransactionInfo;
import org.infinispan.util.ByteString;

/**
 * Get transactions for the specified segments.
 *
 * @author Ryan Emerson
 * @since 11.0
 */
@ProtoTypeId(ProtoStreamTypeIds.STATE_TRANSFER_GET_TRANSACTIONS_COMMAND)
public class StateTransferGetTransactionsCommand extends AbstractStateTransferCommand {

   public static final byte COMMAND_ID = 119;

   @ProtoFactory
   StateTransferGetTransactionsCommand(ByteString cacheName, int topologyId, Set<Integer> segmentsWorkaround) {
      this(cacheName, topologyId, segmentsWorkaround == null ? null : IntSets.from(segmentsWorkaround));
   }

//   @ProtoFactory
   public StateTransferGetTransactionsCommand(ByteString cacheName, int topologyId, IntSet segments) {
      super(COMMAND_ID, cacheName, topologyId, segments);
   }

   @Override
   public CompletionStage<List<TransactionInfo>> invokeAsync(ComponentRegistry registry) throws Throwable {
      // TODO needs to handle collection return
      StateProvider stateProvider = registry.getStateTransferManager().getStateProvider();
      return stateProvider.getTransactionsForSegments(origin, topologyId, segments);
   }

   @Override
   public String toString() {
      return "StateTransferGetTransactionsCommand{" +
            "topologyId=" + topologyId +
            ", segments=" + segments +
            ", cacheName=" + cacheName +
            '}';
   }
}
