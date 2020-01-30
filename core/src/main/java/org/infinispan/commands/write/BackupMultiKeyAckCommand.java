package org.infinispan.commands.write;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.util.ByteString;
import org.infinispan.util.concurrent.CommandAckCollector;

/**
 * A command that represents an acknowledge sent by a backup owner to the originator.
 * <p>
 * The acknowledge signals a successful execution of a multi-key command, like {@link PutMapCommand}. It contains the
 * segments ids of the updated keys.
 *
 * @author Pedro Ruivo
 * @since 9.0
 */
@ProtoTypeId(ProtoStreamTypeIds.BACKUP_MULTI_KEY_ACK_COMMAND)
public class BackupMultiKeyAckCommand extends BackupAckCommand {

   public static final byte COMMAND_ID = 41;

   @ProtoField(number = 4, defaultValue = "-1")
   final int segment;

   @ProtoFactory
   public BackupMultiKeyAckCommand(ByteString cacheName, long id, int segment, int topologyId) {
      super(cacheName, id, topologyId);
      this.segment = segment;
   }

   @Override
   public void ack(CommandAckCollector ackCollector) {
      ackCollector.multiKeyBackupAck(id, getOrigin(), segment, topologyId);
   }

   @Override
   public byte getCommandId() {
      return COMMAND_ID;
   }

   @Override
   public String toString() {
      return "BackupMultiKeyAckCommand{" +
            "id=" + id +
            ", segment=" + segment +
            ", topologyId=" + topologyId +
            '}';
   }
}
