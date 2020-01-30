package org.infinispan.xsite.commands;

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
import org.infinispan.xsite.statetransfer.XSiteStateConsumer;
import org.infinispan.xsite.statetransfer.XSiteStateTransferManager;

/**
 * Start receiving XSite state.
 *
 * @author Ryan Emerson
 * @since 11.0
 */
@ProtoTypeId(ProtoStreamTypeIds.XSITE_STATE_TRANSFER_START_RECEIVE_COMMAND)
public class XSiteStateTransferStartReceiveCommand extends XSiteReplicateCommand {

   public static final byte COMMAND_ID = 106;

   @ProtoField(number = 2)
   final String siteName;

   @ProtoFactory
   public XSiteStateTransferStartReceiveCommand(ByteString cacheName, String siteName) {
      super(COMMAND_ID, cacheName);
      this.siteName = siteName;
   }

   @Override
   public CompletionStage<?> invokeAsync(ComponentRegistry registry) {
      XSiteStateTransferManager stateTransferManager = registry.getXSiteStateTransferManager().running();
      XSiteStateConsumer consumer = stateTransferManager.getStateConsumer();
      consumer.startStateTransfer(siteName);
      return CompletableFutures.completedNull();
   }

   @Override
   public CompletionStage<Void> performInLocalSite(BackupReceiver receiver, boolean preserveOrder) {
      assert !preserveOrder;
      return receiver.handleStartReceivingStateTransfer(this);
   }

   public static XSiteStateTransferStartReceiveCommand copyForCache(XSiteStateTransferStartReceiveCommand command, ByteString cacheName) {
      return new XSiteStateTransferStartReceiveCommand(cacheName, command.originSite);
   }

   @Override
   public String toString() {
      return "XSiteStateTransferStartReceiveCommand{" +
            "siteName='" + siteName + '\'' +
            ", cacheName=" + cacheName +
            '}';
   }
}
