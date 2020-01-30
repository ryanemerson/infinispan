package org.infinispan.commands.write;

import java.util.Objects;

import org.infinispan.commands.CommandInvocationId;
import org.infinispan.commands.MetadataAwareCommand;
import org.infinispan.commands.Visitor;
import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.context.InvocationContext;
import org.infinispan.context.impl.FlagBitSets;
import org.infinispan.marshall.protostream.impl.MarshallableObject;
import org.infinispan.marshall.protostream.impl.MarshallableUserObject;
import org.infinispan.metadata.Metadata;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;


/**
 * @author Mircea.Markus@jboss.com
 * @author <a href="mailto:galder.zamarreno@jboss.com">Galder Zamarreno</a>
 * @since 4.0
 */
@ProtoTypeId(ProtoStreamTypeIds.REMOVE_COMMAND)
public class RemoveCommand extends AbstractDataWriteCommand implements MetadataAwareCommand {
   public static final byte COMMAND_ID = 10;

   private transient boolean successful = true;
   private transient boolean nonExistent = false;

   /**
    * When not null, value indicates that the entry should only be removed if the key is mapped to this value.
    * When null, the entry should be removed regardless of what value it is mapped to.
    */
   @ProtoField(number = 6)
   protected MarshallableUserObject<?> value;

   @ProtoField(number = 7)
   protected MarshallableObject<Metadata> metadata;

   @ProtoField(number = 8)
   protected ValueMatcher valueMatcher;

   @ProtoFactory
   RemoveCommand(MarshallableUserObject<?> wrappedKey, long flagsWithoutRemote, int topologyId, int segment,
                 CommandInvocationId commandInvocationId, MarshallableUserObject<?> value,
                 MarshallableObject<Metadata> metadata, ValueMatcher valueMatcher) {
      super(wrappedKey, flagsWithoutRemote, topologyId, segment, commandInvocationId);
      this.value = value;
      this.metadata = metadata;
      this.valueMatcher = valueMatcher;
   }

   public RemoveCommand(Object key, Object value, int segment, long flagsBitSet,
                        CommandInvocationId commandInvocationId) {
      super(key, segment, flagsBitSet, commandInvocationId);
      setValue(value);
      this.valueMatcher = value != null ? ValueMatcher.MATCH_EXPECTED : ValueMatcher.MATCH_ALWAYS;
   }

   @Override
   public Object acceptVisitor(InvocationContext ctx, Visitor visitor) throws Throwable {
      return visitor.visitRemoveCommand(ctx, this);
   }

   @Override
   public byte getCommandId() {
      return COMMAND_ID;
   }

   @Override
   public void setMetadata(Metadata metadata) {
      this.metadata = MarshallableObject.create(metadata);
   }

   @Override
   public Metadata getMetadata() {
      return MarshallableObject.unwrap(metadata);
   }

   @Override
   public boolean equals(Object o) {
      if (!super.equals(o)) {
         return false;
      }

      RemoveCommand that = (RemoveCommand) o;

      return Objects.equals(value, that.value);
   }

   @Override
   public int hashCode() {
      int result = super.hashCode();
      result = 31 * result + (value != null ? value.hashCode() : 0);
      return result;
   }


   @Override
   public String toString() {
      return "RemoveCommand{key=" + key +
            ", value=" + value +
            ", metadata=" + metadata +
            ", flags=" + printFlags() +
            ", commandInvocationId=" + CommandInvocationId.show(commandInvocationId) +
            ", valueMatcher=" + valueMatcher +
            ", topologyId=" + getTopologyId() +
            "}";
   }

   @Override
   public boolean isSuccessful() {
      return successful;
   }

   @Override
   public boolean isConditional() {
      return value != null;
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
   public LoadType loadType() {
      return isConditional() || !hasAnyFlag(FlagBitSets.IGNORE_RETURN_VALUES) ? LoadType.PRIMARY : LoadType.DONT_LOAD;
   }

   public Object getValue() {
      return value;
   }

   public void setValue(Object value) {
      this.value = MarshallableUserObject.create(value);
   }

   @Override
   public final boolean isReturnValueExpected() {
      // IGNORE_RETURN_VALUES ignored for conditional remove
      return isConditional() || super.isReturnValueExpected();
   }
}
