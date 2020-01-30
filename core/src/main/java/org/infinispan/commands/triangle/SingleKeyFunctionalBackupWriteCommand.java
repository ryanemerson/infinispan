package org.infinispan.commands.triangle;

import static org.infinispan.commands.triangle.SingleKeyFunctionalBackupWriteCommand.Operation.READ_WRITE;
import static org.infinispan.commands.triangle.SingleKeyFunctionalBackupWriteCommand.Operation.READ_WRITE_KEY_VALUE;
import static org.infinispan.commands.triangle.SingleKeyFunctionalBackupWriteCommand.Operation.WRITE_ONLY;
import static org.infinispan.commands.triangle.SingleKeyFunctionalBackupWriteCommand.Operation.WRITE_ONLY_KEY_VALUE;
import static org.infinispan.commands.write.ValueMatcher.MATCH_ALWAYS;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import org.infinispan.commands.CommandInvocationId;
import org.infinispan.commands.functional.AbstractWriteKeyCommand;
import org.infinispan.commands.functional.ReadWriteKeyCommand;
import org.infinispan.commands.functional.ReadWriteKeyValueCommand;
import org.infinispan.commands.functional.WriteOnlyKeyCommand;
import org.infinispan.commands.functional.WriteOnlyKeyValueCommand;
import org.infinispan.commands.write.WriteCommand;
import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.encoding.DataConversion;
import org.infinispan.functional.impl.Params;
import org.infinispan.marshall.protostream.impl.MarshallableObject;
import org.infinispan.marshall.protostream.impl.MarshallableUserObject;
import org.infinispan.metadata.Metadata;
import org.infinispan.protostream.annotations.ProtoEnumValue;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoName;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.util.ByteString;

/**
 * A single key {@link BackupWriteCommand} for single key functional commands.
 *
 * @author Pedro Ruivo
 * @since 9.0
 */
@ProtoTypeId(ProtoStreamTypeIds.SINGLE_KEY_FUNCTIONAL_BACKUP_WRITE_COMMAND)
public class SingleKeyFunctionalBackupWriteCommand extends FunctionalBackupWriteCommand {

   public static final byte COMMAND_ID = 77;

   @ProtoField(number = 11)
   final Operation operation;

   @ProtoField(number = 12)
   final MarshallableUserObject<?> key;

   @ProtoField(number = 13)
   final MarshallableUserObject<?> value;

   @ProtoField(number = 14)
   final MarshallableUserObject<?> prevValue;

   @ProtoField(number = 15)
   final MarshallableObject<Metadata> prevMetadata;


   @ProtoFactory
   SingleKeyFunctionalBackupWriteCommand(ByteString cacheName, CommandInvocationId commandInvocationId, int topologyId,
                                         long flags, long sequence, int segmentId, MarshallableUserObject<?> function,
                                         Params params, DataConversion keyDataConversion, DataConversion valueDataConversion,
                                         Operation operation, MarshallableUserObject<?> key, MarshallableUserObject<?> value,
                                         MarshallableUserObject<?> prevValue, MarshallableObject<Metadata> prevMetadata) {
      super(cacheName, commandInvocationId, topologyId, flags, sequence, segmentId, function, params, keyDataConversion,
            valueDataConversion);
      this.operation = operation;
      this.key = key;
      this.value = value;
      this.prevValue = prevValue;
      this.prevMetadata = prevMetadata;
   }

   private SingleKeyFunctionalBackupWriteCommand(ByteString cacheName, AbstractWriteKeyCommand<?, ?> command, long sequence, int segmentId,
                                                 Operation operation, Object key, Object value, Object prevValue,
                                                 Metadata prevMetadata, Object function) {
      super(cacheName, command, sequence, segmentId, function);
      this.operation = operation;
      this.key = MarshallableUserObject.create(key);
      this.value = MarshallableUserObject.create(value);
      this.prevValue = MarshallableUserObject.create(prevValue);
      this.prevMetadata = MarshallableObject.create(prevMetadata);
   }

   public static SingleKeyFunctionalBackupWriteCommand create(ByteString cacheName, ReadWriteKeyCommand<?, ?, ?> command, long sequence, int segmentId) {
      return new SingleKeyFunctionalBackupWriteCommand(cacheName, command, sequence, segmentId, READ_WRITE, command.getKey(), null, null,
            null, command.getFunction());
   }

   public static SingleKeyFunctionalBackupWriteCommand create(ByteString cacheName, ReadWriteKeyValueCommand<?, ?, ?, ?> command, long sequence, int segmentId) {
      return new SingleKeyFunctionalBackupWriteCommand(cacheName, command, sequence, segmentId, READ_WRITE_KEY_VALUE, command.getKey(), command.getArgument(),
            command.getPrevValue(), command.getPrevMetadata(), command.getBiFunction());
   }


   public static SingleKeyFunctionalBackupWriteCommand create(ByteString cacheName, WriteOnlyKeyValueCommand<?, ?, ?> command, long sequence, int segmentId) {
      return new SingleKeyFunctionalBackupWriteCommand(cacheName, command, sequence, segmentId, WRITE_ONLY_KEY_VALUE, command.getKey(), command.getArgument(),
            null, null, command.getBiConsumer());
   }

   public static SingleKeyFunctionalBackupWriteCommand create(ByteString cacheName, WriteOnlyKeyCommand<?, ?> command, long sequence, int segmentId) {
      return new SingleKeyFunctionalBackupWriteCommand(cacheName, command, sequence, segmentId, WRITE_ONLY, command.getKey(), null, null,
            null, command.getConsumer());
   }

   @Override
   public byte getCommandId() {
      return COMMAND_ID;
   }

   @Override
   WriteCommand createWriteCommand() {
      // TODO can we remove unwrapping once commands have been updated to have a ProtoFactory
      Object key = MarshallableUserObject.unwrap(this.key);
      Object value = MarshallableUserObject.unwrap(this.value);
      Object function = MarshallableUserObject.unwrap(this.function);
      Object prevValue = MarshallableUserObject.unwrap(this.prevValue);
      Metadata prevMetadata = MarshallableObject.unwrap(this.prevMetadata);
      switch (operation) {
         case READ_WRITE:
            //noinspection unchecked
            return new ReadWriteKeyCommand(key, (Function) function, segmentId, getCommandInvocationId(), MATCH_ALWAYS,
                  params, keyDataConversion, valueDataConversion);
         case READ_WRITE_KEY_VALUE:
            //noinspection unchecked
            ReadWriteKeyValueCommand cmd = new ReadWriteKeyValueCommand(key, value, (BiFunction) function, segmentId,
                  getCommandInvocationId(), MATCH_ALWAYS, params, keyDataConversion, valueDataConversion);
            cmd.setPrevValueAndMetadata(prevValue, prevMetadata);
            return cmd;
         case WRITE_ONLY:
            //noinspection unchecked
            return new WriteOnlyKeyCommand(key, (Consumer) function, segmentId, getCommandInvocationId(), MATCH_ALWAYS,
                  params, keyDataConversion, valueDataConversion);
         case WRITE_ONLY_KEY_VALUE:
            //noinspection unchecked
            return new WriteOnlyKeyValueCommand(key, value, (BiConsumer) function, segmentId, getCommandInvocationId(),
                  MATCH_ALWAYS, params, keyDataConversion, valueDataConversion);
         default:
            throw new IllegalStateException("Unknown operation " + operation);
      }
   }

   @ProtoName(value = "SingleKeyFunctionBackupOperation")
   @ProtoTypeId(ProtoStreamTypeIds.SINGLE_KEY_FUNCTIONAL_BACKUP_WRITE_COMMAND_OPERATION)
   public enum Operation {
      @ProtoEnumValue(number = 1)
      READ_WRITE_KEY_VALUE,
      @ProtoEnumValue(number = 2)
      READ_WRITE,
      @ProtoEnumValue(number = 3)
      WRITE_ONLY_KEY_VALUE,
      @ProtoEnumValue(number = 4)
      WRITE_ONLY
   }
}
