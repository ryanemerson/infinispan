package org.infinispan.xsite.statetransfer;

import java.util.concurrent.CompletionStage;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.util.ByteString;
import org.infinispan.util.concurrent.CompletableFutures;
import org.infinispan.xsite.BackupReceiver;
import org.infinispan.xsite.XSiteReplicateCommand;

/**
 * Wraps the state to be sent to another site
 *
 * @author Pedro Ruivo
 * @since 7.0
 */
@ProtoTypeId(ProtoStreamTypeIds.XSITE_STATE_PUSH_COMMAND)
public class XSiteStatePushCommand extends XSiteReplicateCommand {

   public static final byte COMMAND_ID = 33;
   private XSiteState[] chunk;
   private long timeoutMillis;

   @ProtoFactory
   public XSiteStatePushCommand(ByteString cacheName, XSiteState[] chunk, long timeout) {
      super(COMMAND_ID, cacheName);
      this.chunk = chunk;
      this.timeoutMillis = timeout;
   }

   @ProtoField(number = 2)
   public XSiteState[] getChunk() {
      return chunk;
   }

   @ProtoField(number = 3, name = "timeout", defaultValue = "-1")
   public long getTimeout() {
      return timeoutMillis;
   }

   @Override
   public CompletionStage<Void> performInLocalSite(BackupReceiver receiver, boolean preserveOrder) {
      assert !preserveOrder;
      return receiver.handleStateTransferState(this);
   }

   @Override
   public CompletionStage<?> invokeAsync(ComponentRegistry componentRegistry) throws Throwable {
      XSiteStateConsumer stateConsumer = componentRegistry.getXSiteStateTransferManager().running().getStateConsumer();
      stateConsumer.applyState(chunk);
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
   public boolean canBlock() {
      return true;
   }

   @Override
   public String toString() {
      return "XSiteStatePushCommand{" +
            "cacheName=" + cacheName +
            ", timeout=" + timeoutMillis +
            " (" + chunk.length + " keys)" +
            '}';
   }
}
