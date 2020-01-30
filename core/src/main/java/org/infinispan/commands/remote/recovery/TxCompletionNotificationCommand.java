package org.infinispan.commands.remote.recovery;

import static org.infinispan.commons.util.Util.toStr;

import java.util.Set;
import java.util.concurrent.CompletionStage;

import javax.transaction.xa.Xid;

import org.infinispan.commands.TopologyAffectedCommand;
import org.infinispan.commands.remote.BaseRpcCommand;
import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.marshall.protostream.impl.WrappedMessages;
import org.infinispan.protostream.WrappedMessage;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.statetransfer.StateTransferManager;
import org.infinispan.transaction.impl.RemoteTransaction;
import org.infinispan.transaction.impl.TransactionTable;
import org.infinispan.transaction.xa.GlobalTransaction;
import org.infinispan.transaction.xa.recovery.RecoveryManager;
import org.infinispan.util.ByteString;
import org.infinispan.util.concurrent.CompletableFutures;
import org.infinispan.util.concurrent.locks.LockManager;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * Command for removing recovery related information from the cluster.
 *
 * @author Mircea.Markus@jboss.com
 * @since 5.0
 */
@ProtoTypeId(ProtoStreamTypeIds.TX_COMPLETION_NOTIFICATION_COMMAND)
public class TxCompletionNotificationCommand extends BaseRpcCommand implements TopologyAffectedCommand {
   private static final Log log = LogFactory.getLog(TxCompletionNotificationCommand.class);
   private static final boolean trace = log.isTraceEnabled();

   public static final int COMMAND_ID = 22;

   @ProtoField(number = 2)
   final WrappedMessage xid;

   @ProtoField(number = 3, defaultValue = "-1")
   final long internalId;

   @ProtoField(number = 4)
   final GlobalTransaction gtx;

   @ProtoField(number = 5, defaultValue = "-1")
   int topologyId;

   @ProtoFactory
   TxCompletionNotificationCommand(ByteString cacheName, WrappedMessage xid, long internalId, GlobalTransaction gtx,
                                   int topologyId) {
      super(cacheName);
      this.xid = xid;
      this.internalId = internalId;
      this.gtx = gtx;
      this.topologyId = topologyId;
   }

   public TxCompletionNotificationCommand(Xid xid, GlobalTransaction gtx, ByteString cacheName) {
      this(cacheName, WrappedMessages.orElseNull(xid), -1, gtx, -1);
   }

   public TxCompletionNotificationCommand(long internalId, ByteString cacheName) {
      this(cacheName, null, internalId, null, -1);
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
   public boolean isReturnValueExpected() {
      return false;
   }

   @Override
   public CompletionStage<?> invokeAsync(ComponentRegistry componentRegistry) throws Throwable {
      if (trace)
         log.tracef("Processing completed transaction %s", gtx);
      RemoteTransaction remoteTx = null;
      RecoveryManager recoveryManager = componentRegistry.getRecoveryManager().running();
      if (recoveryManager != null) { //recovery in use
         Xid xid = WrappedMessages.unwrap(this.xid);
         if (xid != null) {
            remoteTx = (RemoteTransaction) recoveryManager.removeRecoveryInformation(xid);
         } else {
            remoteTx = (RemoteTransaction) recoveryManager.removeRecoveryInformation(internalId);
         }
      }
      if (remoteTx == null && gtx != null) {
         TransactionTable txTable = componentRegistry.getTransactionTableRef().running();
         remoteTx = txTable.removeRemoteTransaction(gtx);
      }
      if (remoteTx == null) return CompletableFutures.completedNull();
      forwardCommandRemotely(componentRegistry.getStateTransferManager(), remoteTx);

      LockManager lockManager = componentRegistry.getLockManager().running();
      lockManager.unlockAll(remoteTx.getLockedKeys(), remoteTx.getGlobalTransaction());
      return CompletableFutures.completedNull();
   }

   public GlobalTransaction getGlobalTransaction() {
      return gtx;
   }

   /**
    * This only happens during state transfer.
    */
   private void forwardCommandRemotely(StateTransferManager stateTransferManager, RemoteTransaction remoteTx) {
      Set<Object> affectedKeys = remoteTx.getAffectedKeys();
      if (trace)
         log.tracef("Invoking forward of TxCompletionNotification for transaction %s. Affected keys: %s", gtx,
               toStr(affectedKeys));
      stateTransferManager.forwardCommandIfNeeded(this, affectedKeys, remoteTx.getGlobalTransaction().getAddress());
   }

   @Override
   public byte getCommandId() {
      return COMMAND_ID;
   }

   @Override
   public boolean canBlock() {
      //this command can be forwarded (state transfer)
      return true;
   }

   @Override
   public String toString() {
      return getClass().getSimpleName() +
            "{ xid=" + xid +
            ", internalId=" + internalId +
            ", topologyId=" + topologyId +
            ", gtx=" + gtx +
            ", cacheName=" + cacheName + "} ";
   }
}
