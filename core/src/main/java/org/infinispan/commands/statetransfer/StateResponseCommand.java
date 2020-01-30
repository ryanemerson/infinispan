package org.infinispan.commands.statetransfer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletionStage;

import org.infinispan.commands.TopologyAffectedCommand;
import org.infinispan.commands.remote.BaseRpcCommand;
import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.conflict.impl.StateReceiver;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.statetransfer.StateChunk;
import org.infinispan.statetransfer.StateConsumer;
import org.infinispan.util.ByteString;
import org.infinispan.util.concurrent.CompletableFutures;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * This command is used by a StateProvider to push cache entries to a StateConsumer.
 *
 * @author anistor@redhat.com
 * @since 5.2
 */
@ProtoTypeId(ProtoStreamTypeIds.STATE_RESPONSE_COMMAND)
public class StateResponseCommand extends BaseRpcCommand implements TopologyAffectedCommand {

   private static final Log log = LogFactory.getLog(StateResponseCommand.class);

   public static final byte COMMAND_ID = 20;

   /**
    * The topology id of the sender at send time.
    */
   @ProtoField(number = 2, defaultValue = "-1")
   int topologyId;

   /**
    * A collections of state chunks to be transferred.
    */
   @ProtoField(number = 3, collectionImplementation = ArrayList.class)
   Collection<StateChunk> stateChunks;

   /**
    * Whether the returned state should be applied to the underlying cache upon delivery
    */
   @ProtoField(number = 4, defaultValue = "false")
   boolean applyState;

   /**
    * Traditional state transfer is pull based (node sends {@link org.infinispan.commands.statetransfer.StateTransferStartCommand}
    * and expects StateResponseCommand). This flags unsolicited StateResponseCommand that should be applied anyway. Used
    * by scattered cache.
    */
   @ProtoField(number = 5, defaultValue = "false")
   boolean pushTransfer;

   @ProtoFactory
   public StateResponseCommand(ByteString cacheName, int topologyId, Collection<StateChunk> stateChunks,
                               boolean applyState, boolean pushTransfer) {
      super(cacheName);
      this.topologyId = topologyId;
      this.stateChunks = stateChunks;
      this.applyState = applyState;
      this.pushTransfer = pushTransfer;
   }

   @Override
   public CompletionStage<?> invokeAsync(ComponentRegistry componentRegistry) throws Throwable {
      final boolean trace = log.isTraceEnabled();
      LogFactory.pushNDC(cacheName, trace);
      try {
         if (applyState) {
            StateConsumer stateConsumer = componentRegistry.getStateTransferManager().getStateConsumer();
            return stateConsumer.applyState(origin, topologyId, pushTransfer, stateChunks);
         } else {
            StateReceiver stateReceiver = componentRegistry.getConflictManager().running().getStateReceiver();
            stateReceiver.receiveState(origin, topologyId, stateChunks);
         }
         return CompletableFutures.completedNull();
      } finally {
         LogFactory.popNDC(trace);
      }
   }

   @Override
   public boolean isReturnValueExpected() {
      return false;
   }

   @Override
   public int getTopologyId() {
      return topologyId;
   }

   @Override
   public void setTopologyId(int topologyId) {
      this.topologyId = topologyId;
   }

   @Override
   public byte getCommandId() {
      return COMMAND_ID;
   }

   public Collection<StateChunk> getStateChunks() {
      return stateChunks;
   }

   @Override
   public String toString() {
      return "StateResponseCommand{" +
            "cache=" + cacheName +
            ", pushTransfer=" + pushTransfer +
            ", stateChunks=" + stateChunks +
            ", origin=" + origin +
            ", topologyId=" + topologyId +
            ", applyState=" + applyState +
            '}';
   }
}
