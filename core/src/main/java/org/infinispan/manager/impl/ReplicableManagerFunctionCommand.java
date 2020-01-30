package org.infinispan.manager.impl;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.infinispan.commands.GlobalRpcCommand;
import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.factories.scopes.Scope;
import org.infinispan.factories.scopes.Scopes;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.marshall.protostream.impl.MarshallableObject;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * Replicable Command that runs the given Function passing the {@link EmbeddedCacheManager} as an argument
 *
 * @author wburns
 * @since 8.2
 */
@ProtoTypeId(ProtoStreamTypeIds.REPLICABLE_MANAGER_FUNCTION_COMMAND)
@Scope(Scopes.NONE)
public class ReplicableManagerFunctionCommand implements GlobalRpcCommand {

   public static final byte COMMAND_ID = 60;

   @ProtoField(number = 1)
   MarshallableObject<Function<? super EmbeddedCacheManager, ?>> function;

   @ProtoFactory
   ReplicableManagerFunctionCommand(MarshallableObject<Function<? super EmbeddedCacheManager, ?>> function) {
      this.function = function;
   }

   public ReplicableManagerFunctionCommand(Function<? super EmbeddedCacheManager, ?> function) {
      this(MarshallableObject.create(function));
   }

   @Override
   public CompletableFuture<Object> invokeAsync(GlobalComponentRegistry globalComponentRegistry) throws Throwable {
      EmbeddedCacheManager manager = globalComponentRegistry.getCacheManager();
      return CompletableFuture.completedFuture(function.get().apply(new UnwrappingEmbeddedCacheManager(manager)));
   }

   @Override
   public byte getCommandId() {
      return COMMAND_ID;
   }

   @Override
   public boolean isReturnValueExpected() {
      return true;
   }

   @Override
   public boolean canBlock() {
      // Note that it is highly possible that a user command could block, and some internal Infinispan ones already do
      // This should be remedied with https://issues.redhat.com/browse/ISPN-11482
      return false;
   }
}
