package org.infinispan.commands.triangle;

import org.infinispan.commands.write.WriteCommand;
import org.infinispan.util.ByteString;

/**
 * A command that tell a backup owner to ignore a sequence id after the primary failed to send a regular write command.
 *
 * @author Dan Berindei
 * @since 12.1
 */
public class BackupNoopCommand extends BackupWriteCommand {

   public static final byte COMMAND_ID = 81;

   public BackupNoopCommand(ByteString cacheName, WriteCommand command, long sequence, int segmentId) {
      super(cacheName, command, sequence, segmentId);
   }

   @Override
   public byte getCommandId() {
      return COMMAND_ID;
   }

   @Override
   public String toString() {
      return "BackupNoopCommand{" + toStringFields() + '}';
   }

   @Override
   WriteCommand createWriteCommand() {
      return null;
   }

   @Override
   String toStringFields() {
      return super.toStringFields();
   }
}
