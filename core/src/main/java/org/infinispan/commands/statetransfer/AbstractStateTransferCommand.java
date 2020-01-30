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

   @ProtoField(number = 2, defaultValue = "-1")
   protected int topologyId;

//   @ProtoField(number = 3, collectionImplementation = SmallIntSet.class)
   protected IntSet segments;

   // TODO remove once IPROTO-131 issue fixed
   @ProtoField(number = 3, collectionImplementation = HashSet.class)
   protected Set<Integer> segmentsWorkaround;

   AbstractStateTransferCommand(byte commandId, ByteString cacheName) {
      super(cacheName);
      this.commandId = commandId;
   }

   AbstractStateTransferCommand(byte commandId, ByteString cacheName, int topologyId, IntSet segments) {
      this(commandId, cacheName);
      this.topologyId = topologyId;
      this.segments = segments;
      this.segmentsWorkaround = segments == null ? null : new HashSet<>(segments);
   }

   @Override
   public boolean isReturnValueExpected() {
      return true;
   }

   @Override
   public int getTopologyId() {
      return topologyId;
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
