package org.infinispan.commands;

import org.infinispan.commands.remote.CacheRpcCommand;
import org.infinispan.commons.util.EnumUtil;
import org.infinispan.context.Flag;
import org.infinispan.context.impl.FlagBitSets;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.remoting.transport.Address;
import org.infinispan.util.ByteString;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * Base class for those commands that can carry flags.
 *
 * @author Galder Zamarreño
 * @since 5.1
 */
public abstract class AbstractFlagAffectedCommand implements CacheRpcCommand, FlagAffectedCommand {

   private static final Log log = LogFactory.getLog(AbstractFlagAffectedCommand.class);

   protected ByteString cacheName;
   protected Address origin;
   protected long flags;

   protected AbstractFlagAffectedCommand(long flags) {
      this.flags = flags;
   }

   public AbstractFlagAffectedCommand(ByteString cacheName, long flags) {
      this.cacheName = cacheName;
      this.flags = flags;
   }

   @Override
   @ProtoField(1)
   public ByteString getCacheName() {
      return cacheName;
   }

   @ProtoField(number = 2, name = "flags")
   public long getFlagsWithoutRemote() {
      return FlagBitSets.copyWithoutRemotableFlags(flags);
   }

//   @Override
//   public CompletionStage<?> invokeAsync(ComponentRegistry componentRegistry) throws Throwable {
//      init(componentRegistry);
//      InvocationContextFactory icf = componentRegistry.getInvocationContextFactory().running();
//      InvocationContext ctx = icf.createRemoteInvocationContextForCommand(this, getOrigin());
//      if (log.isTraceEnabled())
//         log.tracef("Invoking command %s, with originLocal flag set to %b", this, ctx.isOriginLocal());
//      return componentRegistry.getInterceptorChain().running().invokeAsync(ctx, this);
//   }

   @Override
   public long getFlagsBitSet() {
      return flags;
   }

   @Override
   public void setFlagsBitSet(long bitSet) {
      this.flags = bitSet;
   }

   protected final boolean hasSameFlags(FlagAffectedCommand other) {
      return this.flags == other.getFlagsBitSet();
   }

   protected final String printFlags() {
      return EnumUtil.prettyPrintBitSet(flags, Flag.class);
   }

   @Override
   public void setCacheName(ByteString cacheName) {
      this.cacheName = cacheName;
   }

   @Override
   public Address getOrigin() {
      return origin;
   }

   @Override
   public void setOrigin(Address origin) {
      this.origin = origin;
   }
}
