package org.infinispan.commands.remote.recovery;

import org.infinispan.commands.InitializableCommand;
import org.infinispan.commands.remote.BaseRpcCommand;
import org.infinispan.transaction.xa.recovery.RecoveryManager;
import org.infinispan.util.ByteString;

/**
 * Base class for recovery-related rpc-commands.
 *
 * @author Mircea.Markus@jboss.com
 * @since 5.0
 */
public abstract class RecoveryCommand extends BaseRpcCommand implements InitializableCommand {

   protected RecoveryManager recoveryManager;

   private RecoveryCommand() {
      super(null); // For command id uniqueness test
   }

   protected RecoveryCommand(ByteString cacheName) {
      super(cacheName);
   }

   @Override
   public void init(CommandContext context, boolean isRemote) {
      this.recoveryManager = context.getRecoveryManager();
   }

   @Override
   public boolean isReturnValueExpected() {
      return true;
   }
}
