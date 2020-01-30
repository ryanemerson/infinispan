package org.infinispan.commands.remote;

import java.util.Collection;
import java.util.concurrent.CompletionStage;

import org.infinispan.commands.write.BackupAckCommand;
import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.marshall.protostream.impl.MarshallableUserCollection;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.remoting.inboundhandler.DeliverOrder;
import org.infinispan.remoting.rpc.RpcManager;
import org.infinispan.remoting.transport.Address;
import org.infinispan.remoting.transport.jgroups.JGroupsAddress;
import org.infinispan.scattered.BiasManager;
import org.infinispan.util.ByteString;
import org.infinispan.util.concurrent.CompletableFutures;

/**
 * Informs node that it is not allowed to serve reads from the local record anymore.
 * After local bias is revoked a {@link BackupAckCommand} is sent to the originator, and this confirms all keys.
 */
//TODO: consolidate this with InvalidateVersionsCommand
@ProtoTypeId(ProtoStreamTypeIds.REVOKE_BIAS_COMMAND)
public class RevokeBiasCommand extends BaseRpcCommand {
   public static final byte COMMAND_ID = 74;

   @ProtoField(number = 2, javaType = JGroupsAddress.class)
   final Address ackTarget;

   @ProtoField(number = 3, defaultValue = "-1")
   final long id;

   @ProtoField(number = 4, defaultValue = "-1")
   final int topologyId;

   final Collection<?> keys;

   public RevokeBiasCommand(ByteString cacheName, Address ackTarget, long id, int topologyId, Collection<?> keys) {
      super(cacheName);
      this.ackTarget = ackTarget;
      this.id = id;
      this.topologyId = topologyId;
      this.keys = keys;
   }

   @ProtoFactory
   RevokeBiasCommand(ByteString cacheName, JGroupsAddress ackTarget, long id, int topologyId, MarshallableUserCollection<?> keys) {
      this(cacheName, ackTarget, id, topologyId, MarshallableUserCollection.unwrap(keys));
   }

   @ProtoField(number = 5)
   MarshallableUserCollection<?> getKeys() {
      return MarshallableUserCollection.create(keys);
   }

   @Override
   public CompletionStage<?> invokeAsync(ComponentRegistry componentRegistry) throws Throwable {
      BiasManager biasManager = componentRegistry.getBiasManager().running();
      for (Object key : keys) {
         biasManager.revokeLocalBias(key);
      }
      // ackTarget null means that this message is sent synchronously by primary owner == originator
      if (ackTarget != null) {
         RpcManager rpcManager = componentRegistry.getRpcManager().running();
         rpcManager.sendTo(ackTarget, new BackupAckCommand(cacheName, id, topologyId), DeliverOrder.NONE);
      }
      return CompletableFutures.completedNull();
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
      return "RevokeBiasCommand{" + "ackTarget=" + ackTarget +
            ", id=" + id +
            ", topologyId=" + topologyId +
            ", keys=" + keys +
            '}';
   }
}
