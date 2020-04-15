package org.infinispan.commands.triangle;

import static org.infinispan.commands.triangle.SingleKeyBackupWriteCommand.Operation.COMPUTE;
import static org.infinispan.commands.triangle.SingleKeyBackupWriteCommand.Operation.COMPUTE_IF_ABSENT;
import static org.infinispan.commands.triangle.SingleKeyBackupWriteCommand.Operation.COMPUTE_IF_PRESENT;
import static org.infinispan.commands.triangle.SingleKeyBackupWriteCommand.Operation.REMOVE;
import static org.infinispan.commands.triangle.SingleKeyBackupWriteCommand.Operation.REMOVE_EXPIRED;
import static org.infinispan.commands.triangle.SingleKeyBackupWriteCommand.Operation.REPLACE;
import static org.infinispan.commands.triangle.SingleKeyBackupWriteCommand.Operation.WRITE;

import java.util.function.BiFunction;
import java.util.function.Function;

import org.infinispan.commands.CommandInvocationId;
import org.infinispan.commands.write.ComputeCommand;
import org.infinispan.commands.write.ComputeIfAbsentCommand;
import org.infinispan.commands.write.PutKeyValueCommand;
import org.infinispan.commands.write.RemoveCommand;
import org.infinispan.commands.write.RemoveExpiredCommand;
import org.infinispan.commands.write.ReplaceCommand;
import org.infinispan.commands.write.WriteCommand;
import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.marshall.protostream.impl.MarshallableObject;
import org.infinispan.metadata.Metadata;
import org.infinispan.protostream.annotations.ProtoEnumValue;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoName;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.util.ByteString;

/**
 * A single key {@link BackupWriteCommand} for single key non-functional commands.
 *
 * @author Pedro Ruivo
 * @since 9.2
 */
@ProtoTypeId(ProtoStreamTypeIds.SINGLE_KEY_BACKUP_WRITE_COMMAND)
public class SingleKeyBackupWriteCommand extends BackupWriteCommand {

   public static final byte COMMAND_ID = 76;

   @ProtoField(number = 7)
   final Operation operation;

   @ProtoField(number = 8)
   final MarshallableObject<?> key;

   @ProtoField(number = 9)
   final MarshallableObject<?> valueOrFunction;

   @ProtoField(number = 10)
   final MarshallableObject<Metadata> metadata;

   @ProtoFactory
   SingleKeyBackupWriteCommand(ByteString cacheName, CommandInvocationId commandInvocationId, int topologyId,
                               long flags, long sequence, int segmentId, Operation operation, MarshallableObject<?> key,
                               MarshallableObject<?> valueOrFunction, MarshallableObject<Metadata> metadata) {
      super(cacheName, commandInvocationId, topologyId, flags, sequence, segmentId);
      this.operation = operation;
      this.key = key;
      this.valueOrFunction = valueOrFunction;
      this.metadata = metadata;
   }

   public SingleKeyBackupWriteCommand(ByteString cacheName, WriteCommand command, long sequence, int segmentId,
                                      Operation operation, Object key, Object valueOrFunction, Metadata metadata) {
      super(cacheName, command, sequence, segmentId);
      this.operation = operation;
      this.key = MarshallableObject.create(key);
      this.valueOrFunction = MarshallableObject.create(valueOrFunction);
      this.metadata = MarshallableObject.create(metadata);
   }

   public static SingleKeyBackupWriteCommand create(ByteString cacheName, PutKeyValueCommand command, long sequence, int segmentId) {
      return new SingleKeyBackupWriteCommand(cacheName, command, sequence, segmentId, WRITE,
            command.getKey(), command.getValue(), command.getMetadata());
   }

   public static SingleKeyBackupWriteCommand create(ByteString cacheName, RemoveCommand command, long sequence, int segmentId) {
      boolean removeExpired = command instanceof RemoveExpiredCommand;
      Operation operation = removeExpired ? REMOVE_EXPIRED : REMOVE;
      Object value = removeExpired ? command.getValue() : null;
      return new SingleKeyBackupWriteCommand(cacheName, command, sequence, segmentId, operation, command.getKey(), value, null);
   }

   public static SingleKeyBackupWriteCommand create(ByteString cacheName, ReplaceCommand command, long sequence, int segmentId) {
      return new SingleKeyBackupWriteCommand(cacheName, command, sequence, segmentId, REPLACE, command.getKey(),
            command.getNewValue(), command.getMetadata());
   }

   public static SingleKeyBackupWriteCommand create(ByteString cacheName, ComputeIfAbsentCommand command, long sequence, int segmentId) {
      return new SingleKeyBackupWriteCommand(cacheName, command, sequence, segmentId, COMPUTE_IF_ABSENT,
            command.getKey(), command.getMappingFunction(), command.getMetadata());
   }

   public static SingleKeyBackupWriteCommand create(ByteString cacheName, ComputeCommand command, long sequence, int segmentId) {
      Operation operation = command.isComputeIfPresent() ? COMPUTE_IF_PRESENT : COMPUTE;
      return new SingleKeyBackupWriteCommand(cacheName, command, sequence, segmentId, operation, command.getKey(),
            command.getRemappingBiFunction(), command.getMetadata());
   }

   @Override
   public byte getCommandId() {
      return COMMAND_ID;
   }

   @Override
   public String toString() {
      return "SingleKeyBackupWriteCommand{" + toStringFields() + '}';
   }

   @Override
   WriteCommand createWriteCommand() {
      // TODO can we remove unwrapping once commands have been updated to have a ProtoFactory
      Object key = MarshallableObject.unwrap(this.key);
      Object valueOrFunction = MarshallableObject.unwrap(this.valueOrFunction);
      Metadata metadata = MarshallableObject.unwrap(this.metadata);
      switch (operation) {
         case REMOVE:
            return new RemoveCommand(key, null, segmentId, getFlags(), getCommandInvocationId());
         case WRITE:
            return new PutKeyValueCommand(key, valueOrFunction, false, metadata, segmentId, getTopologyId(),
                  getCommandInvocationId());
         case COMPUTE:
            return new ComputeCommand(key, (BiFunction<?, ?, ?>) valueOrFunction, false, segmentId, getFlags(),
                  getCommandInvocationId(), metadata);
         case REPLACE:
            return new ReplaceCommand(key, null, valueOrFunction, metadata, segmentId, getFlags(),
                  getCommandInvocationId());
         case REMOVE_EXPIRED:
            // Doesn't matter if it is max idle or not - important thing is that it raises expired event
            return new RemoveExpiredCommand(key, valueOrFunction, null, segmentId, getFlags(),
                  getCommandInvocationId());
         case COMPUTE_IF_PRESENT:
            return new ComputeCommand(key, (BiFunction<?, ?, ?>) valueOrFunction, true, segmentId, getFlags(),
                  getCommandInvocationId(), metadata);
         case COMPUTE_IF_ABSENT:
            return new ComputeIfAbsentCommand(key, (Function<?, ?>) valueOrFunction, segmentId, getFlags(),
                  getCommandInvocationId(), metadata);
         default:
            throw new IllegalStateException("Unknown operation " + operation);
      }
   }

   @Override
   String toStringFields() {
      return super.toStringFields() +
            ", operation=" + operation +
            ", key=" + key +
            ", valueOrFunction=" + valueOrFunction +
            ", metadata=" + metadata;
   }

   @ProtoName(value = "SingleKeyBackupOperation")
   @ProtoTypeId(ProtoStreamTypeIds.SINGLE_KEY_BACKUP_WRITE_COMMAND_OPERATION)
   public enum Operation {
      @ProtoEnumValue(number = 1)
      WRITE,
      @ProtoEnumValue(number = 2)
      REMOVE,
      @ProtoEnumValue(number = 3)
      REMOVE_EXPIRED,
      @ProtoEnumValue(number = 4)
      REPLACE,
      @ProtoEnumValue(number = 5)
      COMPUTE,
      @ProtoEnumValue(number = 6)
      COMPUTE_IF_PRESENT,
      @ProtoEnumValue(number = 7)
      COMPUTE_IF_ABSENT
   }
}
