package org.infinispan.commands.statetransfer;

import java.util.HashSet;
import java.util.Set;

import org.infinispan.commands.TopologyAffectedCommand;
import org.infinispan.commands.remote.BaseRpcCommand;
import org.infinispan.commons.util.IntSet;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.util.ByteString;

/**
 * Base class for commands related to state transfer.
 *
 * @author Ryan Emerson
 * @since 11.0
 */
abstract class AbstractStateTransferCommand extends BaseRpcCommand implements TopologyAffectedCommand {

   private final byte commandId;
   protected int topologyId;
   protected IntSet segments;

   AbstractStateTransferCommand(byte commandId, ByteString cacheName) {
      super(cacheName);
      this.commandId = commandId;
   }

   AbstractStateTransferCommand(byte commandId, ByteString cacheName, int topologyId, IntSet segments) {
      this(commandId, cacheName);
      this.topologyId = topologyId;
      this.segments = segments;
   }

   @Override
   @ProtoField(value = 2, defaultValue = "-1")
   public int getTopologyId() {
      return topologyId;
   }

   @ProtoField(number = 3, collectionImplementation = HashSet.class)
   Set<Integer> getSegmentsSet() {
      return segments;
   }

   @Override
   public boolean isReturnValueExpected() {
      return true;
   }

   @Override
   public void setTopologyId(int topologyId) {
      this.topologyId = topologyId;
   }

   public IntSet getSegments() {
      return segments;
   }

   @Override
   public byte getCommandId() {
      return commandId;
   }
}
