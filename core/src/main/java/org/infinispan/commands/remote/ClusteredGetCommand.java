package org.infinispan.commands.remote;

import java.util.Objects;
import java.util.concurrent.CompletionStage;

import org.infinispan.commands.SegmentSpecificCommand;
import org.infinispan.commands.TopologyAffectedCommand;
import org.infinispan.commands.read.GetCacheEntryCommand;
import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.commons.util.EnumUtil;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.container.entries.MVCCEntry;
import org.infinispan.context.Flag;
import org.infinispan.context.InvocationContext;
import org.infinispan.context.InvocationContextFactory;
import org.infinispan.context.impl.FlagBitSets;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.interceptors.AsyncInterceptorChain;
import org.infinispan.marshall.protostream.impl.MarshallableObject;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.util.ByteString;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * Issues a remote get call.  This is not a {@link org.infinispan.commands.VisitableCommand} and hence not passed up the
 * interceptor chain.
 * <p/>
 *
 * @author Mircea.Markus@jboss.com
 * @since 4.0
 */
@ProtoTypeId(ProtoStreamTypeIds.CLUSTERED_GET_COMMAND)
public class ClusteredGetCommand extends BaseRpcCommand implements SegmentSpecificCommand, TopologyAffectedCommand {

   public static final byte COMMAND_ID = 16;
   private static final Log log = LogFactory.getLog(ClusteredGetCommand.class);
   private static final boolean trace = log.isTraceEnabled();

   private Object key;

   //only used by extended statistics. this boolean is local.
   private boolean isWrite;
   private int segment;
   private int topologyId = -1;
   private long flags;

   public ClusteredGetCommand(Object key, ByteString cacheName, int segment, long flags) {
      super(cacheName);
      this.key = key;
      this.isWrite = false;
      if (segment < 0) {
         throw new IllegalArgumentException("Segment must 0 or greater!");
      }
      this.segment = segment;
      this.flags = flags;
   }

   @ProtoFactory
   ClusteredGetCommand(ByteString cacheName, int topologyId, MarshallableObject<?> wrappedKey, int segment, long flagsWithoutRemote) {
      this(MarshallableObject.unwrap(wrappedKey), cacheName, segment, flagsWithoutRemote);
      this.topologyId = topologyId;
   }

   @Override
   @ProtoField(number = 2, defaultValue = "-1")
   public int getTopologyId() {
      return topologyId;
   }

   @ProtoField(number = 3, name = "key")
   MarshallableObject<?> getWrappedKey() {
      return MarshallableObject.create(key);
   }

   @Override
   @ProtoField(number = 4, defaultValue = "-1")
   public int getSegment() {
      return segment;
   }

   @ProtoField(number = 5, name = "flags", defaultValue = "0")
   long getFlagsWithoutRemote() {
      return FlagBitSets.copyWithoutRemotableFlags(flags);
   }

   /**
    * Invokes a logical "get(key)" on a remote cache and returns results.
    * @return
    */
   @Override
   public CompletionStage<?> invokeAsync(ComponentRegistry componentRegistry) throws Throwable {
      // make sure the get command doesn't perform a remote call
      // as our caller is already calling the ClusteredGetCommand on all the relevant nodes
      // CACHE_MODE_LOCAL is not used as it can be used when we want to ignore the ownership with respect to reads
      long flagBitSet = EnumUtil.bitSetOf(Flag.SKIP_REMOTE_LOOKUP);
      GetCacheEntryCommand command = componentRegistry.getCommandsFactory().buildGetCacheEntryCommand(key, segment,
            EnumUtil.mergeBitSets(flagBitSet, flags));
      command.setTopologyId(topologyId);
      InvocationContextFactory icf = componentRegistry.getInvocationContextFactory().running();
      InvocationContext invocationContext = icf.createRemoteInvocationContextForCommand(command, getOrigin());
      AsyncInterceptorChain invoker = componentRegistry.getInterceptorChain().running();
      return invoker.invokeAsync(invocationContext, command)
            .thenApply(rv -> {
               if (trace) log.tracef("Return value for key=%s is %s", key, rv);
               //this might happen if the value was fetched from a cache loader
               if (rv instanceof MVCCEntry) {
                  MVCCEntry mvccEntry = (MVCCEntry) rv;
                  return componentRegistry.getInternalEntryFactory().wired().createValue(mvccEntry);
               } else if (rv instanceof InternalCacheEntry) {
                  InternalCacheEntry internalCacheEntry = (InternalCacheEntry) rv;
                  return internalCacheEntry.toInternalCacheValue();
               } else { // null or Response
                  return rv;
               }
            });
   }

   @Override
   public byte getCommandId() {
      return COMMAND_ID;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      ClusteredGetCommand that = (ClusteredGetCommand) o;
      return Objects.equals(key, that.key);
   }

   @Override
   public int hashCode() {
      return Objects.hashCode(key);
   }

   @Override
   public String toString() {
      return "ClusteredGetCommand{key=" + key +
            ", flags=" + EnumUtil.prettyPrintBitSet(flags, Flag.class) +
            ", topologyId=" + topologyId +
            "}";
   }

   public boolean isWrite() {
      return isWrite;
   }

   public void setWrite(boolean write) {
      isWrite = write;
   }

   public Object getKey() {
      return key;
   }

   @Override
   public boolean isReturnValueExpected() {
      return true;
   }

   @Override
   public void setTopologyId(int topologyId) {
      this.topologyId = topologyId;
   }
}
