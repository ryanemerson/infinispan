package org.infinispan.commands.remote;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import org.infinispan.commands.TopologyAffectedCommand;
import org.infinispan.commands.read.GetAllCommand;
import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.commons.util.EnumUtil;
import org.infinispan.container.entries.CacheEntry;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.context.Flag;
import org.infinispan.context.InvocationContext;
import org.infinispan.context.impl.FlagBitSets;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.marshall.protostream.impl.MarshallableObject;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.remoting.responses.Response;
import org.infinispan.transaction.xa.GlobalTransaction;
import org.infinispan.util.ByteString;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * Issues a remote getAll call.  This is not a {@link org.infinispan.commands.VisitableCommand} and hence not passed up
 * the interceptor chain.
 *
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
@ProtoTypeId(ProtoStreamTypeIds.CLUSTERED_GET_ALL_COMMAND)
public class ClusteredGetAllCommand<K, V> extends BaseRpcCommand implements TopologyAffectedCommand {
   public static final byte COMMAND_ID = 46;
   private static final Log log = LogFactory.getLog(ClusteredGetAllCommand.class);
   private static final boolean trace = log.isTraceEnabled();

   @ProtoField(number = 2, defaultValue = "-1")
   int topologyId;

   @ProtoField(number = 3, collectionImplementation = ArrayList.class)
   final List<MarshallableObject<?>> keys;

   @ProtoField(number = 4, name = "globalTransaction")
   final GlobalTransaction gtx;

   final long flags;

   @ProtoFactory
   ClusteredGetAllCommand(ByteString cacheName, int topologyId, List<MarshallableObject<?>> keys,
                          GlobalTransaction gtx, long flagsWithoutRemote) {
      super(cacheName);
      this.topologyId = topologyId;
      this.keys = keys;
      this.gtx = gtx;
      this.flags = flagsWithoutRemote;
   }

   public ClusteredGetAllCommand(ByteString cacheName, List<?> keys, long flags, GlobalTransaction gtx) {
      super(cacheName);
      this.keys = keys.stream().map(MarshallableObject::new).collect(Collectors.toList());
      this.gtx = gtx;
      this.flags = flags;
   }

   @ProtoField(number = 5, name = "flags", defaultValue = "0")
   long getFlagsWithoutRemote() {
      return FlagBitSets.copyWithoutRemotableFlags(flags);
   }

   @Override
   public CompletionStage<?> invokeAsync(ComponentRegistry componentRegistry) throws Throwable {
      if (!EnumUtil.containsAny(flags, FlagBitSets.FORCE_WRITE_LOCK)) {
         return invokeGetAll(componentRegistry);
      } else {
         return componentRegistry.getCommandsFactory()
               .buildLockControlCommand(keys, flags, gtx)
               .invokeAsync(componentRegistry)
               .thenCompose(o -> invokeGetAll(componentRegistry));
      }
   }

   private CompletionStage<Object> invokeGetAll(ComponentRegistry cr) {
      // make sure the get command doesn't perform a remote call
      // as our caller is already calling the ClusteredGetCommand on all the relevant nodes
      GetAllCommand command = cr.getCommandsFactory().buildGetAllCommand(keys, flags, true);
      command.setTopologyId(topologyId);
      InvocationContext invocationContext = cr.getInvocationContextFactory().running().createRemoteInvocationContextForCommand(command, getOrigin());
      CompletionStage<Object> future = cr.getInterceptorChain().running().invokeAsync(invocationContext, command);
      return future.thenApply(rv -> {
         if (trace) log.trace("Found: " + rv);
         if (rv == null || rv instanceof Response) {
            return rv;
         }

         Map<K, CacheEntry<K, V>> map = (Map<K, CacheEntry<K, V>>) rv;
         return keys.stream()
               .map(MarshallableObject::unwrap)
               .map(map::get)
               .map(entry -> {
                  if (entry == null) {
                     return null;
                  } else if (entry instanceof InternalCacheEntry) {
                     return ((InternalCacheEntry<K, V>) entry).toInternalCacheValue();
                  } else {
                     return cr.getInternalEntryFactory().running().createValue(entry);
                  }
               })
               .collect(Collectors.toList());
      });
   }

   public List<?> getKeys() {
      return keys;
   }

   @Override
   public byte getCommandId() {
      return COMMAND_ID;
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

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ClusteredGetAllCommand<?, ?> that = (ClusteredGetAllCommand<?, ?>) o;
      return Objects.equals(keys, that.keys) &&
            Objects.equals(gtx, that.gtx);
   }

   @Override
   public int hashCode() {
      return Objects.hash(keys, gtx);
   }

   @Override
   public String toString() {
      return "ClusteredGetAllCommand{" + "keys=" + keys +
            ", flags=" + EnumUtil.prettyPrintBitSet(flags, Flag.class) +
            ", topologyId=" + topologyId +
            '}';
   }
}
