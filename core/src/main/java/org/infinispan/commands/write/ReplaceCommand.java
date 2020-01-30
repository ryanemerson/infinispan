package org.infinispan.commands.write;

import java.util.Objects;

import org.infinispan.commands.CommandInvocationId;
import org.infinispan.commands.MetadataAwareCommand;
import org.infinispan.commands.Visitor;
import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.context.InvocationContext;
import org.infinispan.marshall.protostream.impl.MarshallableObject;
import org.infinispan.marshall.protostream.impl.MarshallableUserObject;
import org.infinispan.metadata.Metadata;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * @author Mircea.Markus@jboss.com
 * @author Galder Zamarreño
 * @since 4.0
 */
@ProtoTypeId(ProtoStreamTypeIds.REPLACE_COMMAND)
public class ReplaceCommand extends AbstractDataWriteCommand implements MetadataAwareCommand {
   public static final byte COMMAND_ID = 11;

   @ProtoField(number = 6)
   MarshallableUserObject<?> oldValue;

   @ProtoField(number = 7)
   MarshallableUserObject<?> newValue;

   @ProtoField(number = 8)
   MarshallableObject<Metadata> metadata;

   @ProtoField(number = 9)
   ValueMatcher valueMatcher;

   private transient boolean successful = true;

   @ProtoFactory
   ReplaceCommand(MarshallableUserObject<?> wrappedKey, long flagsWithoutRemote, int topologyId, int segment,
                  CommandInvocationId commandInvocationId, MarshallableUserObject<?> oldValue,
                  MarshallableUserObject<?> newValue, MarshallableObject<Metadata> metadata, ValueMatcher valueMatcher) {
      super(wrappedKey, flagsWithoutRemote, topologyId, segment, commandInvocationId);
      this.oldValue = oldValue;
      this.newValue = newValue;
      this.metadata = metadata;
      this.valueMatcher = valueMatcher;
   }

   public ReplaceCommand(Object key, Object oldValue, Object newValue, Metadata metadata, int segment, long flagsBitSet,
                         CommandInvocationId commandInvocationId) {
      super(key, segment, flagsBitSet, commandInvocationId);
      setOldValue(oldValue);
      setNewValue(newValue);
      setMetadata(metadata);
      this.valueMatcher = oldValue != null ? ValueMatcher.MATCH_EXPECTED : ValueMatcher.MATCH_NON_NULL;
   }

   @Override
   public Object acceptVisitor(InvocationContext ctx, Visitor visitor) throws Throwable {
      return visitor.visitReplaceCommand(ctx, this);
   }

   @Override
   public LoadType loadType() {
      return LoadType.PRIMARY;
   }

   @Override
   public byte getCommandId() {
      return COMMAND_ID;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      if (!super.equals(o)) return false;
      ReplaceCommand that = (ReplaceCommand) o;
      return Objects.equals(oldValue, that.oldValue) &&
            Objects.equals(newValue, that.newValue) &&
            Objects.equals(metadata, that.metadata);
   }

   @Override
   public int hashCode() {
      return Objects.hash(super.hashCode(), oldValue, newValue, metadata);
   }

   @Override
   public boolean isSuccessful() {
      return successful;
   }

   @Override
   public boolean isConditional() {
      return true;
   }

   @Override
   public Metadata getMetadata() {
      return MarshallableObject.unwrap(metadata);
   }

   @Override
   public void setMetadata(Metadata metadata) {
      this.metadata = MarshallableObject.create(metadata);
   }

   public Object getOldValue() {
      return MarshallableUserObject.unwrap(oldValue);
   }

   public void setOldValue(Object oldValue) {
      this.oldValue = MarshallableUserObject.create(oldValue);
   }

   public Object getNewValue() {
      return MarshallableUserObject.unwrap(newValue);
   }

   public void setNewValue(Object newValue) {
      this.newValue = MarshallableUserObject.create(newValue);
   }

   @Override
   public ValueMatcher getValueMatcher() {
      return valueMatcher;
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
   public final boolean isReturnValueExpected() {
     return true;
   }

   @Override
   public String toString() {
      return "ReplaceCommand{" +
            "key=" + key +
            ", oldValue=" + oldValue +
            ", newValue=" + newValue +
            ", metadata=" + metadata +
            ", flags=" + printFlags() +
            ", commandInvocationId=" + CommandInvocationId.show(commandInvocationId) +
            ", successful=" + successful +
            ", valueMatcher=" + valueMatcher +
            ", topologyId=" + topologyId +
            '}';
   }
}
