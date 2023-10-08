package org.infinispan.commands.irac;

import java.util.Collection;
import java.util.concurrent.CompletionStage;

import org.infinispan.commands.remote.BaseRpcCommand;
import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.commons.util.concurrent.CompletableFutures;
import org.infinispan.container.versioning.irac.IracTombstoneInfo;
import org.infinispan.container.versioning.irac.IracTombstoneManager;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.remoting.transport.Address;
import org.infinispan.util.ByteString;

/**
 * Response for a state request with the tombstones stored in the local node.
 *
 * @since 14.0
 */
@ProtoTypeId(ProtoStreamTypeIds.IRAC_TOMBSTONE_STATE_RESPONSE_COMMAND)
public class IracTombstoneStateResponseCommand extends BaseRpcCommand {

   public static final byte COMMAND_ID = 39;

   @ProtoField(number = 2)
   final Collection<IracTombstoneInfo> state;

   @ProtoFactory
   public IracTombstoneStateResponseCommand(ByteString cacheName, Collection<IracTombstoneInfo> state) {
      super(cacheName);
      this.state = state;
   }

   @Override
   public CompletionStage<Void> invokeAsync(ComponentRegistry registry) {
      IracTombstoneManager tombstoneManager = registry.getIracTombstoneManager().running();
      for (IracTombstoneInfo data : state) {
         tombstoneManager.storeTombstoneIfAbsent(data);
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
   public Address getOrigin() {
      //no-op
      return null;
   }

   @Override
   public void setOrigin(Address origin) {
      //no-op
   }

   @Override
   public String toString() {
      return "IracTombstoneStateResponseCommand{" +
            "cacheName=" + cacheName +
            ", state=" + state +
            '}';
   }
}
