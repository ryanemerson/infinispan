package org.infinispan.commands.topology;

import java.util.concurrent.CompletionStage;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * The coordinator is requesting information about the running caches.
 *
 * @author Ryan Emerson
 * @since 11.0
 */
@ProtoTypeId(ProtoStreamTypeIds.CACHE_STATUS_REQUEST_COMMAND)
public class CacheStatusRequestCommand extends AbstractCacheControlCommand {

   public static final byte COMMAND_ID = 96;

   @ProtoField(number = 1, defaultValue = "-1")
   final int viewId;

   @ProtoFactory
   public CacheStatusRequestCommand(int viewId) {
      super(COMMAND_ID);
      this.viewId = viewId;
   }

   @Override
   public CompletionStage<?> invokeAsync(GlobalComponentRegistry gcr) throws Throwable {
      return gcr.getLocalTopologyManager()
            .handleStatusRequest(viewId);
   }

   @Override
   public String toString() {
      return "CacheStatusCommand{" +
            "viewId=" + viewId +
            '}';
   }
}
