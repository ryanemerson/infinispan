package org.infinispan.commands.triangle;

import java.util.Collection;

import org.infinispan.commands.CommandInvocationId;
import org.infinispan.commands.write.PutMapCommand;
import org.infinispan.commands.write.WriteCommand;
import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.marshall.protostream.impl.MarshallableObject;
import org.infinispan.marshall.protostream.impl.MarshallableUserMap;
import org.infinispan.metadata.Metadata;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.util.ByteString;
import org.infinispan.util.TriangleFunctionsUtil;

/**
 * A {@link BackupWriteCommand} implementation for {@link PutMapCommand}.
 *
 * @author Pedro Ruivo
 * @since 9.2
 */
@ProtoTypeId(ProtoStreamTypeIds.PUT_MAP_BACKUP_WRITE_COMMAND)
public class PutMapBackupWriteCommand extends BackupWriteCommand {

   public static final byte COMMAND_ID = 78;

   @ProtoField(number = 7)
   final MarshallableUserMap<Object, Object> map;

   @ProtoField(number = 8)
   final MarshallableObject<Metadata> metadata;

   @ProtoFactory
   PutMapBackupWriteCommand(ByteString cacheName, CommandInvocationId commandInvocationId, int topologyId,
                            long flags, long sequence, int segmentId, MarshallableUserMap<Object, Object> map,
                            MarshallableObject<Metadata> metadata) {
      super(cacheName, commandInvocationId, topologyId, flags, sequence, segmentId);
      this.map = map;
      this.metadata = metadata;
   }

   public PutMapBackupWriteCommand(ByteString cacheName, PutMapCommand command, long sequence, int segmentId,
                                   Collection<Object> keys) {
      super(cacheName, command, sequence, segmentId);
      this.map = MarshallableUserMap.create(TriangleFunctionsUtil.filterEntries(command.getMap(), keys));
      this.metadata = MarshallableObject.create(command.getMetadata());
   }

   @Override
   public byte getCommandId() {
      return COMMAND_ID;
   }

   @Override
   public String toString() {
      return "PutMapBackupWriteCommand{" + toStringFields() + '}';
   }

   @Override
   WriteCommand createWriteCommand() {
      PutMapCommand cmd = new PutMapCommand(map.get(), metadata.get(), getFlags(), getCommandInvocationId());
      cmd.setForwarded(true);
      return cmd;
   }

   @Override
   String toStringFields() {
      return super.toStringFields() +
            ", map=" + map +
            ", metadata=" + metadata;
   }
}
