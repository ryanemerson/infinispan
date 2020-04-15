package org.infinispan.commands.write;

import static org.infinispan.commons.util.Util.toStr;

import java.util.Objects;

import org.infinispan.commands.CommandInvocationId;
import org.infinispan.commands.MetadataAwareCommand;
import org.infinispan.commands.Visitor;
import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.context.InvocationContext;
import org.infinispan.context.impl.FlagBitSets;
import org.infinispan.marshall.protostream.impl.MarshallableObject;
import org.infinispan.metadata.Metadata;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * Implements functionality defined by {@link org.infinispan.Cache#put(Object, Object)}
 *
 * <p>Note: Since 9.4, when the flag {@link org.infinispan.context.Flag#PUT_FOR_STATE_TRANSFER} is set,
 * the metadata is actually an {@code InternalMetadata} that includes the timestamps of the entry
 * from the source node.</p>
 *
 * @author Mircea.Markus@jboss.com
 * @since 4.0
 */
@ProtoTypeId(ProtoStreamTypeIds.PUT_KEY_VALUE_COMMAND)
public class PutKeyValueCommand extends AbstractDataWriteCommand implements MetadataAwareCommand {

   public static final byte COMMAND_ID = 8;

   private Object value;
   private boolean putIfAbsent;
   private ValueMatcher valueMatcher;
   private Metadata metadata;
   private transient boolean successful = true;

   public PutKeyValueCommand(Object key, Object value, boolean putIfAbsent, Metadata metadata, int segment,
                             long flagsBitSet, CommandInvocationId commandInvocationId) {
      super(key, segment, flagsBitSet, commandInvocationId);
      setValue(value);
      setMetadata(metadata);
      this.putIfAbsent = putIfAbsent;
      this.valueMatcher = putIfAbsent ? ValueMatcher.MATCH_EXPECTED : ValueMatcher.MATCH_ALWAYS;
   }

   @ProtoFactory
   PutKeyValueCommand(MarshallableObject<?> wrappedKey, long flagsWithoutRemote, int topologyId, int segment,
                      CommandInvocationId commandInvocationId, MarshallableObject<?> wrappedValue,
                      MarshallableObject<Metadata> wrappedMetadata, ValueMatcher valueMatcher, boolean putIfAbsent) {
      super(wrappedKey, flagsWithoutRemote, topologyId, segment, commandInvocationId);
      this.value = MarshallableObject.unwrap(wrappedValue);
      this.metadata = MarshallableObject.unwrap(wrappedMetadata);
      this.valueMatcher = valueMatcher;
      this.putIfAbsent = putIfAbsent;
   }

   @ProtoField(number = 6, name = "value")
   MarshallableObject<?> getWrappedValue() {
      return MarshallableObject.create(value);
   }

   @ProtoField(number = 7, name = "metadata")
   MarshallableObject<Metadata> getWrappedMetadata() {
      return MarshallableObject.create(metadata);
   }

   @Override
   @ProtoField(number = 8)
   public ValueMatcher getValueMatcher() {
      return valueMatcher;
   }

   @ProtoField(number = 9, defaultValue = "false")
   public boolean isPutIfAbsent() {
      return putIfAbsent;
   }

   public Object getValue() {
      return value;
   }

   public void setValue(Object value) {
      this.value = MarshallableObject.create(value);
   }

   @Override
   public Object acceptVisitor(InvocationContext ctx, Visitor visitor) throws Throwable {
      return visitor.visitPutKeyValueCommand(ctx, this);
   }

   @Override
   public LoadType loadType() {
      if (isConditional() || !hasAnyFlag(FlagBitSets.IGNORE_RETURN_VALUES)) {
         return LoadType.PRIMARY;
      } else {
         return LoadType.DONT_LOAD;
      }
   }

   @Override
   public byte getCommandId() {
      return COMMAND_ID;
   }

   @Override
   public Metadata getMetadata() {
      return metadata;
   }

   @Override
   public void setMetadata(Metadata metadata) {
      this.metadata = metadata;
   }

   public void setPutIfAbsent(boolean putIfAbsent) {
      this.putIfAbsent = putIfAbsent;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      if (!super.equals(o)) return false;
      PutKeyValueCommand that = (PutKeyValueCommand) o;
      return putIfAbsent == that.putIfAbsent &&
            Objects.equals(value, that.value) &&
            Objects.equals(metadata, that.metadata);
   }

   @Override
   public int hashCode() {
      return Objects.hash(super.hashCode(), value, metadata, putIfAbsent);
   }

   @Override
   public String toString() {
      return "PutKeyValueCommand{key=" + toStr(key) +
            ", value=" + toStr(value) +
            ", flags=" + printFlags() +
            ", commandInvocationId=" + CommandInvocationId.show(commandInvocationId) +
            ", putIfAbsent=" + putIfAbsent +
            ", valueMatcher=" + valueMatcher +
            ", metadata=" + metadata +
            ", successful=" + successful +
            ", topologyId=" + topologyId +
            "}";
   }

   @Override
   public boolean isSuccessful() {
      return successful;
   }

   @Override
   public boolean isConditional() {
      return putIfAbsent;
   }

   @Override
   public void setValueMatcher(ValueMatcher valueMatcher) {
      this.valueMatcher = valueMatcher;
   }

   @Override
   public void fail() {
      successful = false;
   }

   @Override
   public boolean isReturnValueExpected() {
      return isConditional() || super.isReturnValueExpected();
   }
}
