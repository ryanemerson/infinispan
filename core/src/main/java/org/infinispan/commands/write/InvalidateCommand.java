package org.infinispan.commands.write;

import java.util.Collection;
import java.util.Objects;

import org.infinispan.commands.AbstractTopologyAffectedCommand;
import org.infinispan.commands.CommandInvocationId;
import org.infinispan.commands.Visitor;
import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.commons.util.Util;
import org.infinispan.context.InvocationContext;
import org.infinispan.context.impl.FlagBitSets;
import org.infinispan.marshall.protostream.impl.MarshallableUserCollection;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.util.concurrent.locks.RemoteLockCommand;


/**
 * Removes an entry from memory.
 *
 * @author Mircea.Markus@jboss.com
 * @since 4.0
 */
@ProtoTypeId(ProtoStreamTypeIds.INVALIDATE_COMMAND)
public class InvalidateCommand extends AbstractTopologyAffectedCommand implements WriteCommand, RemoteLockCommand {
   public static final int COMMAND_ID = 6;

   @ProtoField(number = 3)
   protected MarshallableUserCollection<Object> keys;

   @ProtoField(number = 4)
   protected CommandInvocationId commandInvocationId;

   @ProtoFactory
   InvalidateCommand(long flagsWithoutRemote, int topologyId, CommandInvocationId commandInvocationId,
                     MarshallableUserCollection<Object> keys) {
      super(flagsWithoutRemote, topologyId);
      this.keys = keys;
      this.commandInvocationId = commandInvocationId;
   }

   public InvalidateCommand(long flagsBitSet, CommandInvocationId commandInvocationId, Object... keys) {
      this(flagsBitSet, -1, commandInvocationId, MarshallableUserCollection.create(keys));
   }

   public InvalidateCommand(long flagsBitSet, Collection<Object> keys, CommandInvocationId commandInvocationId) {
      this(flagsBitSet, commandInvocationId, keys == null || keys.isEmpty() ? Util.EMPTY_OBJECT_ARRAY : keys.toArray(new Object[0]));
   }

   @Override
   public byte getCommandId() {
      return COMMAND_ID;
   }

   @Override
   public boolean isReturnValueExpected() {
      return false;
   }

   @Override
   public Object acceptVisitor(InvocationContext ctx, Visitor visitor) throws Throwable {
      return visitor.visitInvalidateCommand(ctx, this);
   }

   public Object[] getKeys() {
      return MarshallableUserCollection.unwrapAsArray(keys, Object[]::new);
   }

   @Override
   public boolean isSuccessful() {
      return true;
   }

   @Override
   public boolean isConditional() {
      return false;
   }

   @Override
   public ValueMatcher getValueMatcher() {
      return ValueMatcher.MATCH_ALWAYS;
   }

   @Override
   public void setValueMatcher(ValueMatcher valueMatcher) {
   }

   @Override
   public Collection<?> getAffectedKeys() {
      return MarshallableUserCollection.unwrap(keys);
   }

   @Override
   public void fail() {
      throw new UnsupportedOperationException();
   }

   @Override
   public CommandInvocationId getCommandInvocationId() {
      return commandInvocationId;
   }

   @Override
   public Collection<?> getKeysToLock() {
      return getAffectedKeys();
   }

   @Override
   public Object getKeyLockOwner() {
      return commandInvocationId;
   }

   @Override
   public boolean hasZeroLockAcquisition() {
      return hasAnyFlag(FlagBitSets.ZERO_LOCK_ACQUISITION_TIMEOUT);
   }

   @Override
   public boolean hasSkipLocking() {
      return hasAnyFlag(FlagBitSets.SKIP_LOCKING);
   }

   @Override
   public LoadType loadType() {
      return LoadType.DONT_LOAD;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      InvalidateCommand that = (InvalidateCommand) o;
      return Objects.equals(keys, that.keys);
   }

   @Override
   public int hashCode() {
      return Objects.hash(keys);
   }

   @Override
   public String toString() {
      return "InvalidateCommand{keys=" + keys + '}';
   }
}
