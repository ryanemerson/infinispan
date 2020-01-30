package org.infinispan.commands.statetransfer;

import java.util.Set;
import java.util.concurrent.CompletionStage;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.commons.util.IntSet;
import org.infinispan.commons.util.IntSets;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.scattered.ScatteredStateProvider;
import org.infinispan.util.ByteString;
import org.infinispan.util.concurrent.CompletableFutures;

/**
 * Start transferring keys and remote metadata for the given segments.
 *
 * @author Ryan Emerson
 * @since 11.0
 */
@ProtoTypeId(ProtoStreamTypeIds.SCATTERED_STATE_GET_KEYS_COMMAND)
public class ScatteredStateGetKeysCommand extends AbstractStateTransferCommand {

   public static final byte COMMAND_ID = 114;

   @ProtoFactory
   ScatteredStateGetKeysCommand(ByteString cacheName, int topologyId, Set<Integer> segmentsWorkaround) {
      this(cacheName, topologyId, segmentsWorkaround == null ? null : IntSets.from(segmentsWorkaround));
   }

//   @ProtoFactory
   public ScatteredStateGetKeysCommand(ByteString cacheName, int topologyId, IntSet segments) {
      super(COMMAND_ID, cacheName, topologyId, segments);
   }

   @Override
   public CompletionStage<?> invokeAsync(ComponentRegistry registry) throws Throwable {
      ScatteredStateProvider stateProvider = (ScatteredStateProvider) registry.getStateTransferManager().getStateProvider();
      stateProvider.startKeysTransfer(segments, origin);
      return CompletableFutures.completedNull();
   }

   @Override
   public String toString() {
      return "ScatteredStateGetKeysCommand{" +
            "topologyId=" + topologyId +
            ", segments=" + segments +
            ", cacheName=" + cacheName +
            '}';
   }
}
