package org.infinispan.transaction.impl;

import static javax.transaction.xa.XAResource.XA_OK;
import static javax.transaction.xa.XAResource.XA_RDONLY;

import java.util.List;

import javax.transaction.Transaction;
import javax.transaction.xa.XAException;

import org.infinispan.commands.CommandsFactory;
import org.infinispan.commands.tx.CommitCommand;
import org.infinispan.commands.tx.PrepareCommand;
import org.infinispan.commands.tx.RollbackCommand;
import org.infinispan.commands.write.WriteCommand;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.Configurations;
import org.infinispan.context.InvocationContextFactory;
import org.infinispan.context.impl.LocalTxInvocationContext;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.factories.annotations.Start;
import org.infinispan.factories.annotations.Stop;
import org.infinispan.factories.impl.ComponentRef;
import org.infinispan.factories.scopes.Scope;
import org.infinispan.factories.scopes.Scopes;
import org.infinispan.interceptors.AsyncInterceptorChain;
import org.infinispan.transaction.xa.GlobalTransaction;
import org.infinispan.transaction.xa.recovery.RecoveryManager;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * Coordinates transaction prepare/commits as received from the {@link javax.transaction.TransactionManager}.
 * Integrates with the TM through either {@link org.infinispan.transaction.xa.TransactionXaAdapter} or
 * through {@link org.infinispan.transaction.synchronization.SynchronizationAdapter}.
 *
 * @author Mircea.Markus@jboss.com
 * @author Pedro Ruivo
 * @since 5.0
 */
@Scope(Scopes.NAMED_CACHE)
public class TransactionCoordinator {
   private static final Log log = LogFactory.getLog(TransactionCoordinator.class);
   private static final boolean trace = log.isTraceEnabled();

   @Inject CommandsFactory commandsFactory;
   @Inject ComponentRef<InvocationContextFactory> icf;
   @Inject ComponentRef<AsyncInterceptorChain> invoker;
   @Inject ComponentRef<TransactionTable> txTable;
   @Inject ComponentRef<RecoveryManager> recoveryManager;
   @Inject Configuration configuration;

   private CommandCreator commandCreator;
   private volatile boolean shuttingDown = false;

   private boolean totalOrder;
   private boolean defaultOnePhaseCommit;
   private boolean use1PcForAutoCommitTransactions;

   @Start(priority = 1)
   void setStartStatus() {
      shuttingDown = false;
   }

   @Stop(priority = 1)
   void setStopStatus() {
      shuttingDown = true;
   }

   @Start
   public void start() {
      use1PcForAutoCommitTransactions = configuration.transaction().use1PcForAutoCommitTransactions();
      totalOrder = configuration.transaction().transactionProtocol().isTotalOrder();
      defaultOnePhaseCommit = Configurations.isOnePhaseCommit(configuration) ||
            Configurations.isOnePhaseTotalOrderCommit(configuration);

      if (Configurations.isTxVersioned(configuration)) {
         // We need to create versioned variants of PrepareCommand and CommitCommand
         commandCreator = new CommandCreator() {
            @Override
            public CommitCommand createCommitCommand(GlobalTransaction gtx) {
               return commandsFactory.buildVersionedCommitCommand(gtx);
            }

            @Override
            public PrepareCommand createPrepareCommand(GlobalTransaction gtx, List<WriteCommand> modifications, boolean onePhaseCommit) {
               return commandsFactory.buildVersionedPrepareCommand(gtx, modifications, onePhaseCommit);
            }
         };
      } else {
         commandCreator = new CommandCreator() {
            @Override
            public CommitCommand createCommitCommand(GlobalTransaction gtx) {
               return commandsFactory.buildCommitCommand(gtx);
            }

            @Override
            public PrepareCommand createPrepareCommand(GlobalTransaction gtx, List<WriteCommand> modifications, boolean onePhaseCommit) {
               return commandsFactory.buildPrepareCommand(gtx, modifications, onePhaseCommit);
            }
         };
      }
   }

   public final int prepare(LocalTransaction localTransaction) throws XAException {
      return prepare(localTransaction, false);
   }

   public final int prepare(LocalTransaction localTransaction, boolean replayEntryWrapping) throws XAException {
      validateNotMarkedForRollback(localTransaction);

      if (isOnePhaseCommit(localTransaction)) {
         if (trace) log.tracef("Received prepare for tx: %s. Skipping call as 1PC will be used.", localTransaction);
         return XA_OK;
      }

      PrepareCommand prepareCommand = commandCreator.createPrepareCommand(localTransaction.getGlobalTransaction(), localTransaction.getModifications(), false);
      if (trace) log.tracef("Sending prepare command through the chain: %s", prepareCommand);

      LocalTxInvocationContext ctx = icf.running().createTxInvocationContext(localTransaction);
      prepareCommand.setReplayEntryWrapping(replayEntryWrapping);
      try {
         invoker.running().invoke(ctx, prepareCommand);
         if (localTransaction.isReadOnly()) {
            if (trace) log.tracef("Readonly transaction: %s", localTransaction.getGlobalTransaction());
            // force a cleanup to release any objects held.  Some TMs don't call commit if it is a READ ONLY tx.  See ISPN-845
            commitInternal(ctx);
            return XA_RDONLY;
         } else {
            txTable.running().localTransactionPrepared(localTransaction);
            return XA_OK;
         }
      } catch (Throwable e) {
         if (shuttingDown)
            log.trace("Exception while preparing back, probably because we're shutting down.");
         else
            log.errorProcessingPrepare(e);

         //rollback transaction before throwing the exception as there's no guarantee the TM calls XAResource.rollback
         //after prepare failed.
         rollback(localTransaction);
         // XA_RBROLLBACK tells the TM that we've rolled back already: the TM shouldn't call rollback after this.
         XAException xe = new XAException(XAException.XA_RBROLLBACK);
         xe.initCause(e);
         throw xe;
      }
   }

   public boolean commit(LocalTransaction localTransaction, boolean isOnePhase) throws XAException {
      if (trace) log.tracef("Committing transaction %s", localTransaction.getGlobalTransaction());
      LocalTxInvocationContext ctx = icf.running().createTxInvocationContext(localTransaction);
      if (isOnePhaseCommit(localTransaction) || isOnePhase) {
         validateNotMarkedForRollback(localTransaction);

         if (trace) log.trace("Doing an 1PC prepare call on the interceptor chain");
         List<WriteCommand> modifications = localTransaction.getModifications();
         PrepareCommand command = commandCreator.createPrepareCommand(localTransaction.getGlobalTransaction(), modifications, true);
         try {
            invoker.running().invoke(ctx, command);
         } catch (Throwable e) {
            handleCommitFailure(e, true, ctx);
         }
         return true;
      } else if (!localTransaction.isReadOnly()) {
         commitInternal(ctx);
      }
      return false;
   }

   public void rollback(LocalTransaction localTransaction) throws XAException {
      try {
         rollbackInternal(icf.running().createTxInvocationContext(localTransaction));
      } catch (Throwable e) {
         if (shuttingDown)
            log.trace("Exception while rolling back, probably because we're shutting down.");
         else
            log.errorRollingBack(e);

         final Transaction transaction = localTransaction.getTransaction();
         //this might be possible if the cache has stopped and TM still holds a reference to the XAResource
         if (transaction != null) {
            txTable.running().failureCompletingTransaction(transaction);
         }
         XAException xe = new XAException(XAException.XAER_RMERR);
         xe.initCause(e);
         throw xe;
      }
   }

   private void handleCommitFailure(Throwable e, boolean onePhaseCommit, LocalTxInvocationContext ctx) throws XAException {
      if (trace) log.tracef("Couldn't commit transaction %s, trying to rollback.", ctx.getCacheTransaction());
      if (onePhaseCommit) {
         log.errorProcessing1pcPrepareCommand(e);
      } else {
         log.errorProcessing2pcCommitCommand(e);
      }
      try {
         boolean isRecoveryEnabled = recoveryManager.running() != null;
         boolean isTotalOrder = onePhaseCommit && totalOrder;
         if (!isRecoveryEnabled && !isTotalOrder) {
            //we cannot send the rollback in Total Order because it will create a new remote transaction.
            //the rollback is not needed any way, because if one node aborts the transaction, then all the nodes will
            //abort too.
            rollbackInternal(ctx);
         }
      } catch (Throwable e1) {
         log.couldNotRollbackPrepared1PcTransaction(ctx.getCacheTransaction(), e1);
         // inform the TM that a resource manager error has occurred in the transaction branch (XAER_RMERR).
         XAException xe = new XAException(XAException.XAER_RMERR);
         xe.initCause(e);
         throw xe;
      } finally {
         txTable.running().failureCompletingTransaction(ctx.getTransaction());
      }
      XAException xe = new XAException(XAException.XA_HEURRB);
      xe.initCause(e);
      throw xe; //this is a heuristic rollback
   }

   private void commitInternal(LocalTxInvocationContext ctx) throws XAException {
      CommitCommand commitCommand = commandCreator.createCommitCommand(ctx.getGlobalTransaction());
      try {
         invoker.running().invoke(ctx, commitCommand);
         txTable.running().removeLocalTransaction(ctx.getCacheTransaction());
      } catch (Throwable e) {
         handleCommitFailure(e, false, ctx);
      }
   }

   private void rollbackInternal(LocalTxInvocationContext ctx) throws Throwable {
      if (trace) log.tracef("rollback transaction %s ", ctx.getGlobalTransaction());
      RollbackCommand rollbackCommand = commandsFactory.buildRollbackCommand(ctx.getGlobalTransaction());
      invoker.running().invoke(ctx, rollbackCommand);
      txTable.running().removeLocalTransaction(ctx.getCacheTransaction());
   }

   private void validateNotMarkedForRollback(LocalTransaction localTransaction) throws XAException {
      if (localTransaction.isMarkedForRollback()) {
         if (trace) log.tracef("Transaction already marked for rollback. Forcing rollback for %s", localTransaction);
         rollback(localTransaction);
         throw new XAException(XAException.XA_RBROLLBACK);
      }
   }

   public boolean is1PcForAutoCommitTransaction(LocalTransaction localTransaction) {
      return use1PcForAutoCommitTransactions && localTransaction.isImplicitTransaction();
   }

   private interface CommandCreator {
      CommitCommand createCommitCommand(GlobalTransaction gtx);
      PrepareCommand createPrepareCommand(GlobalTransaction gtx, List<WriteCommand> modifications, boolean onePhaseCommit);
   }

   private boolean isOnePhaseCommit(LocalTransaction localTransaction) {
      return defaultOnePhaseCommit || is1PcForAutoCommitTransaction(localTransaction);
   }
}
