package org.infinispan.commands.read;

import static org.infinispan.commons.util.EnumUtil.prettyPrintBitSet;
import static org.infinispan.commons.util.Util.toStr;

import java.util.Objects;

import org.infinispan.commands.AbstractFlagAffectedCommand;
import org.infinispan.commands.DataCommand;
import org.infinispan.commands.SegmentSpecificCommand;
import org.infinispan.commands.remote.CacheRpcCommand;
import org.infinispan.context.Flag;
import org.infinispan.context.impl.FlagBitSets;
import org.infinispan.marshall.protostream.impl.MarshallableObject;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.remoting.transport.Address;
import org.infinispan.util.ByteString;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * @author Mircea.Markus@jboss.com
 * @author Sanne Grinovero &lt;sanne@hibernate.org&gt; (C) 2011 Red Hat Inc.
 * @since 4.0
 */
public abstract class AbstractDataCommand implements CacheRpcCommand, DataCommand, SegmentSpecificCommand {

   private static final Log log = LogFactory.getLog(AbstractFlagAffectedCommand.class);

   protected ByteString cacheName;
   protected Address origin;
   protected Object key;
   private long flags;
   // These 2 ints have to stay next to each other to ensure they are aligned together
   protected int topologyId = -1;
   protected int segment;

   // For ProtoFactory implementations
   protected AbstractDataCommand(ByteString cacheName, MarshallableObject<?> wrappedKey, long flagsWithoutRemote, int topologyId, int segment) {
      this(MarshallableObject.unwrap(wrappedKey), segment, flagsWithoutRemote);
      this.cacheName = cacheName;
      this.topologyId = topologyId;
   }

   protected AbstractDataCommand(Object key, int segment, long flagsBitSet) {
      this.key = key;
      if (segment < 0) {
         throw new IllegalArgumentException("Segment must be 0 or greater");
      }
      this.segment = segment;
      this.flags = flagsBitSet;
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
   @ProtoField(1)
   public ByteString getCacheName() {
      return cacheName;
   }

   @ProtoField(number = 2, name = "key")
   public MarshallableObject<?> getWrappedKey() {
      return MarshallableObject.create(key);
   }

   @Override
   @ProtoField(3)
   public int getSegment() {
      return segment;
   }

   @Override
   @ProtoField(4)
   public int getTopologyId() {
      return topologyId;
   }

   @Override
   public void setTopologyId(int topologyId) {
      this.topologyId = topologyId;
   }

   @ProtoField(number = 5, name = "flags")
   public long getFlagsWithoutRemote() {
      return FlagBitSets.copyWithoutRemotableFlags(getFlagsBitSet());
   }

   @Override
   public long getFlagsBitSet() {
      return flags;
   }

   @Override
   public void setFlagsBitSet(long bitSet) {
      this.flags = bitSet;
   }

   @Override
   public Address getOrigin() {
      return origin;
   }

   @Override
   public void setOrigin(Address origin) {
      this.origin = origin;
   }

   @Override
   public Object getKey() {
      return key;
   }

   public void setKey(Object key) {
      this.key = key;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      }
      if (obj == null) {
         return false;
      }
      if (getClass() != obj.getClass()) {
         return false;
      }
      AbstractDataCommand other = (AbstractDataCommand) obj;
      return flags == other.flags && Objects.equals(key, other.key);
   }

   @Override
   public int hashCode() {
      return (key != null ? key.hashCode() : 0);
   }

   @Override
   public String toString() {
      return getClass().getSimpleName() +
            " {key=" + toStr(key) +
            ", flags=" + printFlags() +
            "}";
   }

   @Override
   public boolean isReturnValueExpected() {
      return true;
   }

   protected final String printFlags() {
      return prettyPrintBitSet(flags, Flag.class);
   }
}
