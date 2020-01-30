package org.infinispan.xsite.commands;

import java.util.concurrent.CompletionStage;

import org.infinispan.commands.remote.BaseRpcCommand;
import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.util.ByteString;
import org.infinispan.util.concurrent.CompletableFutures;
import org.infinispan.xsite.statetransfer.XSiteStateProvider;
import org.infinispan.xsite.statetransfer.XSiteStateTransferManager;

/**
 * Start send XSite state.
 *
 * @author Ryan Emerson
 * @since 11.0
 */
@ProtoTypeId(ProtoStreamTypeIds.XSITE_STATE_TRANSFER_START_SEND_COMMAND)
public class XSiteStateTransferStartSendCommand extends BaseRpcCommand {

   public static final byte COMMAND_ID = 104;

   @ProtoField(number = 2)
   final String siteName;

   @ProtoField(number = 3, defaultValue = "-1")
   final int topologyId;

   @ProtoFactory
   public XSiteStateTransferStartSendCommand(ByteString cacheName, String siteName, int topologyId) {
      super(cacheName);
      this.siteName = siteName;
      this.topologyId = topologyId;
   }

   @Override
   public CompletionStage<?> invokeAsync(ComponentRegistry registry) {
      XSiteStateTransferManager stateTransferManager = registry.getXSiteStateTransferManager().running();
      XSiteStateProvider provider = stateTransferManager.getStateProvider();
      provider.startStateTransfer(siteName, getOrigin(), topologyId);
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
      return "XSiteStateTransferStartSendCommand{" +
            "siteName='" + siteName + '\'' +
            ", topologyId=" + topologyId +
            ", cacheName=" + cacheName +
            '}';
   }
}
