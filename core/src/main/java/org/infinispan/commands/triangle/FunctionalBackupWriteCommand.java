package org.infinispan.commands.triangle;

import org.infinispan.commands.CommandInvocationId;
import org.infinispan.commands.functional.AbstractWriteKeyCommand;
import org.infinispan.commands.functional.AbstractWriteManyCommand;
import org.infinispan.commands.functional.FunctionalCommand;
import org.infinispan.encoding.DataConversion;
import org.infinispan.functional.impl.Params;
import org.infinispan.marshall.protostream.impl.MarshallableUserObject;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.util.ByteString;

/**
 * A base {@link BackupWriteCommand} used by {@link FunctionalCommand}.
 *
 * @author Pedro Ruivo
 * @since 9.2
 */
abstract class FunctionalBackupWriteCommand extends BackupWriteCommand {

   @ProtoField(number = 7)
   final MarshallableUserObject<?> function;

   @ProtoField(number = 8)
   final Params params;

   @ProtoField(number = 9)
   final DataConversion keyDataConversion;

   @ProtoField(number = 10)
   final DataConversion valueDataConversion;

   // Used by ProtoFactory implementations
   protected FunctionalBackupWriteCommand(ByteString cacheName, CommandInvocationId commandInvocationId, int topologyId,
                                          long flags, long sequence, int segmentId, MarshallableUserObject<?> function,
                                          Params params, DataConversion keyDataConversion, DataConversion valueDataConversion) {
      super(cacheName, commandInvocationId, topologyId, flags, sequence, segmentId);
      this.function = function;
      this.params = params;
      this.keyDataConversion = keyDataConversion;
      this.valueDataConversion = valueDataConversion;
   }

   protected FunctionalBackupWriteCommand(ByteString cacheName, AbstractWriteKeyCommand<?, ?> command, long sequence,
                                          int segmentId, Object function) {
      super(cacheName, command, sequence, segmentId);
      this.params = command.getParams();
      this.keyDataConversion = command.getKeyDataConversion();
      this.valueDataConversion = command.getValueDataConversion();
      this.function = MarshallableUserObject.create(function);
   }

   protected FunctionalBackupWriteCommand(ByteString cacheName, AbstractWriteManyCommand<?, ?> command, long sequence,
                                          int segmentId, Object function) {
      super(cacheName, command, sequence, segmentId);
      this.params = command.getParams();
      this.keyDataConversion = command.getKeyDataConversion();
      this.valueDataConversion = command.getValueDataConversion();
      this.function = MarshallableUserObject.create(function);
   }
}
