package org.infinispan.reactive.publisher.impl.commands.batch;

import java.util.concurrent.CompletionStage;

import org.infinispan.commands.TopologyAffectedCommand;
import org.infinispan.commands.remote.BaseRpcCommand;
import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.reactive.publisher.impl.PublisherHandler;
import org.infinispan.util.ByteString;

@ProtoTypeId(ProtoStreamTypeIds.NEXT_PUBLISHER_COMMAND)
public class  NextPublisherCommand extends BaseRpcCommand implements TopologyAffectedCommand {
   public static final byte COMMAND_ID = 25;

   @ProtoField(number = 2)
   final String requestId;

   @ProtoField(number = 3, defaultValue = "-1")
   int topologyId;

   @ProtoFactory
   NextPublisherCommand(ByteString cacheName, String requestId, int topologyId) {
      super(cacheName);
      this.requestId = requestId;
      this.topologyId = topologyId;
   }

   public NextPublisherCommand(ByteString cacheName, String requestId) {
      this(cacheName, requestId, -1);
   }

   @Override
   public CompletionStage<?> invokeAsync(ComponentRegistry componentRegistry) throws Throwable {
      PublisherHandler publisherHandler = componentRegistry.getPublisherHandler().running();
      return publisherHandler.getNext(requestId);
   }

   @Override
   public int getTopologyId() {
      return topologyId;
   }

   @Override
   public void setTopologyId(int topologyId) {
      this.topologyId = topologyId;
   }

   @Override
   public byte getCommandId() {
      return COMMAND_ID;
   }

   @Override
   public boolean isReturnValueExpected() {
      return true;
   }
}
