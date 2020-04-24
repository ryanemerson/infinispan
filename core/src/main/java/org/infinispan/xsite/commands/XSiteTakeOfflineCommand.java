package org.infinispan.xsite.commands;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.infinispan.commands.remote.BaseRpcCommand;
import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.util.ByteString;
import org.infinispan.xsite.status.TakeOfflineManager;
import org.infinispan.xsite.status.TakeSiteOfflineResponse;

/**
 * Take a site offline.
 *
 * @author Ryan Emerson
 * @since 11.0
 */
@ProtoTypeId(ProtoStreamTypeIds.XSITE_TAKE_OFFLINE_COMMAND)
public class XSiteTakeOfflineCommand extends BaseRpcCommand {

   public static final int COMMAND_ID = 101;

   @ProtoField(number = 2)
   final String siteName;

   @ProtoFactory
   public XSiteTakeOfflineCommand(ByteString cacheName, String siteName) {
      super(cacheName);
      this.siteName = siteName;
   }

   @Override
   public CompletionStage<TakeSiteOfflineResponse> invokeAsync(ComponentRegistry registry) throws Throwable {
      TakeOfflineManager takeOfflineManager = registry.getTakeOfflineManager().running();
      return CompletableFuture.completedFuture(takeOfflineManager.takeSiteOffline(siteName));
   }

   @Override
   public final boolean isReturnValueExpected() {
      return true;
   }

   @Override
   public byte getCommandId() {
      return COMMAND_ID;
   }

   @Override
   public String toString() {
      return "XSiteTakeOfflineCommand{" +
            "siteName='" + siteName + '\'' +
            '}';
   }
}
