package org.infinispan.commands.triangle;

import java.util.Collection;
import java.util.Map;

import org.infinispan.commands.CommandInvocationId;
import org.infinispan.commands.write.PutMapCommand;
import org.infinispan.commands.write.WriteCommand;
import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.marshall.protostream.impl.MarshallableMap;
import org.infinispan.marshall.protostream.impl.MarshallableObject;
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

   private Map<Object, Object> map;
   private Metadata metadata;

   public PutMapBackupWriteCommand(ByteString cacheName, PutMapCommand command, long sequence, int segmentId,
                                   Collection<Object> keys) {
      super(cacheName, command, sequence, segmentId);
      this.map = TriangleFunctionsUtil.filterEntries(command.getMap(), keys);
      this.metadata = command.getMetadata();
   }

   @ProtoFactory
   PutMapBackupWriteCommand(ByteString cacheName, CommandInvocationId commandInvocationId, int topologyId,
                            long flags, long sequence, int segmentId, MarshallableMap<Object, Object> map,
                            MarshallableObject<Metadata> metadata) {
      super(cacheName, commandInvocationId, topologyId, flags, sequence, segmentId);
      this.map = MarshallableMap.unwrap(map);
      this.metadata = MarshallableObject.unwrap(metadata);
   }

   @ProtoField(number = 7)
   MarshallableMap<Object, Object> getMap() {
      return MarshallableMap.create(map);
   }

   @ProtoField(number = 8)
   MarshallableObject<Metadata> getMetadata() {
      return MarshallableObject.create(metadata);
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
      PutMapCommand cmd = new PutMapCommand(map, metadata, getFlags(), getCommandInvocationId());
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
