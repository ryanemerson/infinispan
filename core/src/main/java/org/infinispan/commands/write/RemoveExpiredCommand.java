package org.infinispan.commands.write;

import java.util.Objects;

import org.infinispan.commands.CommandInvocationId;
import org.infinispan.commands.Visitor;
import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.context.InvocationContext;
import org.infinispan.marshall.protostream.impl.MarshallableObject;
import org.infinispan.metadata.Metadata;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;


/**
 * Removes an entry that is expired from memory
 *
 * @author William Burns
 * @since 8.0
 */
@ProtoTypeId(ProtoStreamTypeIds.REMOVE_EXPIRED_COMMAND)
public class RemoveExpiredCommand extends RemoveCommand {
   public static final int COMMAND_ID = 58;

   @ProtoField(number = 9, defaultValue = "-1")
   Long lifespan;

   @ProtoFactory
   RemoveExpiredCommand(MarshallableObject<?> wrappedKey, long flagsWithoutRemote, int topologyId, int segment,
                        CommandInvocationId commandInvocationId, MarshallableObject<?> wrappedValue,
                        MarshallableObject<Metadata> wrappedMetadata, ValueMatcher valueMatcher, Long lifespan) {
      super(wrappedKey, flagsWithoutRemote, topologyId, segment, commandInvocationId, wrappedValue, null, valueMatcher);
      this.lifespan = lifespan;
   }

   public RemoveExpiredCommand(Object key, Object value, Long lifespan, int segment, long flagBitSet,
                               CommandInvocationId commandInvocationId) {
      //valueEquivalence can be null because this command never compares values.
      super(key, value, segment, flagBitSet, commandInvocationId);
      this.lifespan = lifespan;
      this.valueMatcher = ValueMatcher.MATCH_EXPECTED_OR_NULL;
   }

   @Override
   public Object acceptVisitor(InvocationContext ctx, Visitor visitor) throws Throwable {
      return visitor.visitRemoveExpiredCommand(ctx, this);
   }

   @Override
   public boolean isConditional() {
      return true;
   }

   @Override
   public byte getCommandId() {
      return COMMAND_ID;
   }

   @Override
   public String toString() {
      return "RemoveExpiredCommand{" +
              "key=" + key +
              ", value=" + value +
              ", lifespan=" + lifespan +
              '}';
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      if (!super.equals(o)) return false;
      RemoveExpiredCommand that = (RemoveExpiredCommand) o;
      return Objects.equals(lifespan, that.lifespan);
   }

   @Override
   public int hashCode() {
      return Objects.hash(super.hashCode(), lifespan);
   }

   public Long getLifespan() {
      return lifespan;
   }
}
