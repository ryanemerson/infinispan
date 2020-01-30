package org.infinispan.commands.write;

import org.infinispan.commands.remote.BaseRpcCommand;
import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.util.ByteString;
import org.infinispan.util.concurrent.CommandAckCollector;

/**
 * A command that represents an acknowledge sent by a backup owner to the originator.
 * <p>
 * The acknowledge signals a successful execution of the operation.
 *
 * @author Pedro Ruivo
 * @since 9.0
 */
@ProtoTypeId(ProtoStreamTypeIds.BACKUP_ACK_COMMAND)
public class BackupAckCommand extends BaseRpcCommand {

   public static final byte COMMAND_ID = 2;
   @ProtoField(number = 2, defaultValue = "-1")
   protected long id;

   @ProtoField(number = 3, defaultValue = "-1")
   protected int topologyId;

   @ProtoFactory
   public BackupAckCommand(ByteString cacheName, long id, int topologyId) {
      super(cacheName);
      this.id = id;
      this.topologyId = topologyId;
   }

   public void ack(CommandAckCollector ackCollector) {
      ackCollector.backupAck(id, getOrigin(), topologyId);
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
   public String toString() {
      return "BackupAckCommand{" +
            "id=" + id +
            ", topologyId=" + topologyId +
            '}';
   }
}
