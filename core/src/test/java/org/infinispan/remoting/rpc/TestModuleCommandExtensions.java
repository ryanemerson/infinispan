package org.infinispan.remoting.rpc;

import java.io.IOException;
import java.util.Set;

import org.infinispan.commands.ReplicableCommand;
import org.infinispan.commands.module.ModuleCommandExtensions;
import org.infinispan.commands.module.ModuleCommandFactory;
import org.infinispan.commands.module.ModuleCommandInitializer;
import org.infinispan.commons.util.Util;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.protostream.SerializationContext;

/**
 * @author anistor@redhat.com
 * @since 5.3
 */
public final class TestModuleCommandExtensions implements ModuleCommandExtensions {

   @Override
   public ModuleCommandFactory getModuleCommandFactory() {
      return new ModuleCommandFactory() {
         @Override
         public Set<Class<? extends ReplicableCommand>> getModuleCommandSet() {
            return Util.asSet(CustomReplicableCommand.class, CustomCacheRpcCommand.class, SleepingCacheRpcCommand.class);
         }

//         @Override
//         public ReplicableCommand fromStream(byte commandId) {
//            ReplicableCommand c;
//            switch (commandId) {
//               case CustomReplicableCommand.COMMAND_ID:
//                  c = new CustomReplicableCommand();
//                  break;
//               default:
//                  throw new IllegalArgumentException("Not registered to handle command id " + commandId);
//            }
//            return c;
//         }
//
//         @Override
//         public CacheRpcCommand fromStream(byte commandId, ByteString cacheName) {
//            CacheRpcCommand c;
//            switch (commandId) {
//               case CustomCacheRpcCommand.COMMAND_ID:
//                  c = new CustomCacheRpcCommand(cacheName);
//                  break;
//               case SleepingCacheRpcCommand.COMMAND_ID:
//                  c = new SleepingCacheRpcCommand(cacheName);
//                  break;
//               default:
//                  throw new IllegalArgumentException("Not registered to handle command id " + commandId);
//            }
//            return c;
//         }

         @Override
         public void registerInternalClasses(SerializationContext serializationContext) throws IOException {
            // TODO: Update to register CustomCacheRpcCommand etc
         }

         @Override
         public void registerPersistenceClasses(SerializationContext serializationContext) throws IOException {
            // TODO: Update to register CustomCacheRpcCommand etc
         }
      };
   }

   @Override
   public ModuleCommandInitializer getModuleCommandInitializer() {
      return new ModuleCommandInitializer() {
         @Inject EmbeddedCacheManager cacheManager;

         @Override
         public void initializeReplicableCommand(ReplicableCommand c, boolean isRemote) {
            // nothing to do here
         }
      };
   }
}
