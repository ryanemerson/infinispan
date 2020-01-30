package org.infinispan.commands.statetransfer;

import java.util.Set;
import java.util.concurrent.CompletionStage;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.commons.util.IntSet;
import org.infinispan.commons.util.IntSets;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.scattered.BiasManager;
import org.infinispan.scattered.ScatteredStateProvider;
import org.infinispan.util.ByteString;

/**
 * Invoke {@link ScatteredStateProvider#confirmRevokedSegments(int)}.
 *
 * @author Ryan Emerson
 * @since 11.0
 */
@ProtoTypeId(ProtoStreamTypeIds.SCATTERED_STATE_CONFIRM_REVOKE_COMMAND)
public class ScatteredStateConfirmRevokedCommand extends AbstractStateTransferCommand {

   public static final byte COMMAND_ID = 115;

   @ProtoFactory
   ScatteredStateConfirmRevokedCommand(ByteString cacheName, int topologyId, Set<Integer> segmentsWorkaround) {
      this(cacheName, topologyId, segmentsWorkaround == null ? null : IntSets.from(segmentsWorkaround));
   }

//   @ProtoFactory
   public ScatteredStateConfirmRevokedCommand(ByteString cacheName, int topologyId, IntSet segments) {
      super(COMMAND_ID, cacheName, topologyId, segments);
   }

   @Override
   public CompletionStage<?> invokeAsync(ComponentRegistry registry) throws Throwable {
      ScatteredStateProvider stateProvider = (ScatteredStateProvider) registry.getStateTransferManager().getStateProvider();
      BiasManager biasManager = registry.getBiasManager().running();
      return stateProvider.confirmRevokedSegments(topologyId)
            .thenApply(nil -> {
               if (biasManager != null) {
                  biasManager.revokeLocalBiasForSegments(segments);
               }
               return null;
            });
   }

   @Override
   public String toString() {
      return "ScatteredStateConfirmRevokedCommand{" +
            "topologyId=" + topologyId +
            ", segments=" + segments +
            ", cacheName=" + cacheName +
            '}';
   }
}
