package org.infinispan.commands.read;

import static org.infinispan.commons.util.EnumUtil.prettyPrintBitSet;

import java.util.Objects;

import org.infinispan.commands.DataCommand;
import org.infinispan.commands.SegmentSpecificCommand;
import org.infinispan.context.Flag;
import org.infinispan.context.impl.FlagBitSets;
import org.infinispan.marshall.protostream.impl.MarshallableObject;
import org.infinispan.protostream.annotations.ProtoField;

/**
 * @author Mircea.Markus@jboss.com
 * @author Sanne Grinovero &lt;sanne@hibernate.org&gt; (C) 2011 Red Hat Inc.
 * @since 4.0
 */
public abstract class AbstractDataCommand implements DataCommand, SegmentSpecificCommand {

   protected MarshallableObject<?> key;
   private long flags;
   // These 2 ints have to stay next to each other to ensure they are aligned together
   protected int topologyId = -1;
   protected int segment;

   // For ProtoFactory implementations
   protected AbstractDataCommand(MarshallableObject<?> wrappedKey, long flagsWithoutRemote, int topologyId, int segment) {
      this.key = wrappedKey;
      this.flags = flagsWithoutRemote;
      this.topologyId = topologyId;
      this.segment = segment;

      if (segment < 0)
         throw new IllegalArgumentException("Segment must be 0 or greater");
   }

   protected AbstractDataCommand(Object key, int segment, long flagsBitSet) {
      this(MarshallableObject.create(key), flagsBitSet, 0, segment);
   }

   // TODO remove
   protected AbstractDataCommand() {
      this.segment = -1;
   }

   @ProtoField(number = 1, name = "key")
   public MarshallableObject<?> getWrappedKey() {
      return key;
   }

   @Override
   @ProtoField(number = 2, defaultValue = "-1")
   public int getSegment() {
      return segment;
   }

   @Override
   @ProtoField(number = 3, defaultValue = "-1")
   public int getTopologyId() {
      return topologyId;
   }

   @Override
   public void setTopologyId(int topologyId) {
      this.topologyId = topologyId;
   }

   @ProtoField(number = 4, name = "flags", defaultValue = "0")
   public long getFlagsWithoutRemote() {
      return FlagBitSets.copyWithoutRemotableFlags(flags);
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
   public Object getKey() {
      return MarshallableObject.unwrap(key);
   }

   public void setKey(Object key) {
      this.key = MarshallableObject.create(key);
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
            " {key=" + key +
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
