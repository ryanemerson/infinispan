package org.infinispan.commands.triangle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.infinispan.commands.CommandInvocationId;
import org.infinispan.commands.functional.AbstractWriteManyCommand;
import org.infinispan.commands.functional.ReadWriteManyCommand;
import org.infinispan.commands.functional.WriteOnlyManyCommand;
import org.infinispan.commands.write.WriteCommand;
import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.encoding.DataConversion;
import org.infinispan.functional.impl.Params;
import org.infinispan.marshall.protostream.impl.MarshallableObject;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.util.ByteString;

/**
 * A multi-key {@link BackupWriteCommand} for {@link WriteOnlyManyCommand} and {@link ReadWriteManyCommand}.
 *
 * @author Pedro Ruivo
 * @since 9.2
 */
@ProtoTypeId(ProtoStreamTypeIds.MULTI_KEY_FUNCTIONAL_BACKUP_WRITE_COMMAND)
public class MultiKeyFunctionalBackupWriteCommand extends FunctionalBackupWriteCommand {

   public static final byte COMMAND_ID = 80;

   @ProtoField(number = 11, defaultValue = "false")
   final boolean writeOnly;

   @ProtoField(number = 12, collectionImplementation = ArrayList.class)
   final Collection<MarshallableObject<?>> keys;

   @ProtoFactory
   MultiKeyFunctionalBackupWriteCommand(ByteString cacheName, CommandInvocationId commandInvocationId, int topologyId,
                                        long flags, long sequence, int segmentId, MarshallableObject<?> function,
                                        Params params, DataConversion keyDataConversion, DataConversion valueDataConversion,
                                        boolean writeOnly, Collection<MarshallableObject<?>> keys) {
      super(cacheName, commandInvocationId, topologyId, flags, sequence, segmentId, function, params, keyDataConversion, valueDataConversion);
      this.writeOnly = writeOnly;
      this.keys = keys;
   }

   private MultiKeyFunctionalBackupWriteCommand(ByteString cacheName, AbstractWriteManyCommand<?, ?> command, long sequence,
                                                int segmentId, Object function, boolean writeOnly, Collection<?> keys) {
      super(cacheName, command, sequence, segmentId, function);
      this.writeOnly = writeOnly;
      this.keys = keys.stream().map(MarshallableObject::new).collect(Collectors.toList());
   }

   public static <K, V> MultiKeyFunctionalBackupWriteCommand create(ByteString cacheName, WriteOnlyManyCommand<K, V> command,
                                                                    Collection<?> keys, long sequence, int segmentId) {
      return new MultiKeyFunctionalBackupWriteCommand(cacheName, command, sequence, segmentId, command.getConsumer(), true, keys);
   }

   public static <K, V, R> MultiKeyFunctionalBackupWriteCommand create(ByteString cacheName, ReadWriteManyCommand<K, V, R> command,
                                                                       Collection<?> keys, long sequence, int segmentId) {
      return new MultiKeyFunctionalBackupWriteCommand(cacheName, command, sequence, segmentId, command.getFunction(), false, keys);
   }

   @Override
   public byte getCommandId() {
      return COMMAND_ID;
   }

   @Override
   WriteCommand createWriteCommand() {
      //noinspection unchecked
      AbstractWriteManyCommand cmd = writeOnly ?
            new WriteOnlyManyCommand(keys, (Consumer) function, params, getCommandInvocationId(),
                  keyDataConversion, valueDataConversion) :
            new ReadWriteManyCommand(keys, (Function) function, params, getCommandInvocationId(),
                  keyDataConversion, valueDataConversion);
      cmd.setForwarded(true);
      return cmd;
   }
}
