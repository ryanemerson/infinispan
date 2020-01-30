package org.infinispan.commands.remote;

import java.util.concurrent.CompletionStage;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.marshall.protostream.impl.MarshallableArray;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.scattered.BiasManager;
import org.infinispan.util.ByteString;
import org.infinispan.util.concurrent.CompletableFutures;

@ProtoTypeId(ProtoStreamTypeIds.RENEW_BIAS_COMMAND)
public class RenewBiasCommand extends BaseRpcCommand {
   public static final byte COMMAND_ID = 75;

   Object[] keys;

   public RenewBiasCommand(ByteString cacheName, Object[] keys) {
      super(cacheName);
      this.keys = keys;
   }

   @ProtoFactory
   RenewBiasCommand(ByteString cacheName, MarshallableArray<Object> keys) {
      this(cacheName, MarshallableArray.unwrap(keys, new Object[0]));
   }

   @ProtoField(number = 2)
   MarshallableArray<Object> getKeys() {
      return MarshallableArray.create(keys);
   }

   @Override
   public CompletionStage<?> invokeAsync(ComponentRegistry componentRegistry) throws Throwable {
      BiasManager biasManager = componentRegistry.getBiasManager().running();
      for (Object key : keys) {
         biasManager.renewRemoteBias(key, getOrigin());
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
}
