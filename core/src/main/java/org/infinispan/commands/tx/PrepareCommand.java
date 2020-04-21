package org.infinispan.commands.tx;

import static org.infinispan.commons.util.InfinispanCollections.forEach;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import org.infinispan.commands.VisitableCommand;
import org.infinispan.commands.Visitor;
import org.infinispan.commands.functional.ReadWriteKeyCommand;
import org.infinispan.commands.functional.ReadWriteKeyValueCommand;
import org.infinispan.commands.functional.ReadWriteManyCommand;
import org.infinispan.commands.functional.ReadWriteManyEntriesCommand;
import org.infinispan.commands.functional.WriteOnlyKeyCommand;
import org.infinispan.commands.functional.WriteOnlyKeyValueCommand;
import org.infinispan.commands.functional.WriteOnlyManyCommand;
import org.infinispan.commands.functional.WriteOnlyManyEntriesCommand;
import org.infinispan.commands.write.ComputeCommand;
import org.infinispan.commands.write.ComputeIfAbsentCommand;
import org.infinispan.commands.write.DataWriteCommand;
import org.infinispan.commands.write.InvalidateCommand;
import org.infinispan.commands.write.PutKeyValueCommand;
import org.infinispan.commands.write.PutMapCommand;
import org.infinispan.commands.write.RemoveCommand;
import org.infinispan.commands.write.RemoveExpiredCommand;
import org.infinispan.commands.write.ReplaceCommand;
import org.infinispan.commands.write.WriteCommand;
import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.context.InvocationContext;
import org.infinispan.context.InvocationContextFactory;
import org.infinispan.context.impl.FlagBitSets;
import org.infinispan.context.impl.RemoteTxInvocationContext;
import org.infinispan.context.impl.TxInvocationContext;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.interceptors.AsyncInterceptorChain;
import org.infinispan.marshall.protostream.impl.MarshallableArray;
import org.infinispan.notifications.cachelistener.CacheNotifier;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.transaction.impl.RemoteTransaction;
import org.infinispan.transaction.impl.TransactionTable;
import org.infinispan.transaction.xa.GlobalTransaction;
import org.infinispan.transaction.xa.recovery.RecoveryManager;
import org.infinispan.util.ByteString;
import org.infinispan.util.concurrent.CompletableFutures;
import org.infinispan.util.concurrent.CompletionStages;
import org.infinispan.util.concurrent.locks.TransactionalRemoteLockCommand;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * Command corresponding to the 1st phase of 2PC.
 *
 * @author Manik Surtani (<a href="mailto:manik@jboss.org">manik@jboss.org</a>)
 * @author Mircea.Markus@jboss.com
 * @since 4.0
 */
@ProtoTypeId(ProtoStreamTypeIds.PREPARE_COMMAND)
public class PrepareCommand extends AbstractTransactionBoundaryCommand implements TransactionalRemoteLockCommand {

   private static final Log log = LogFactory.getLog(PrepareCommand.class);
   private static boolean trace = log.isTraceEnabled();

   public static final byte COMMAND_ID = 12;

   protected WriteCommand[] modifications;
   protected boolean onePhaseCommit;
   private transient boolean replayEntryWrapping  = false;
   protected boolean retriedCommand;

   private static final WriteCommand[] EMPTY_WRITE_COMMAND_ARRAY = new WriteCommand[0];

   public PrepareCommand(ByteString cacheName, GlobalTransaction gtx, Collection<WriteCommand> commands, boolean onePhaseCommit) {
      this(cacheName, gtx, commands.toArray(new WriteCommand[0]), onePhaseCommit);
   }

   private PrepareCommand(ByteString cacheName, GlobalTransaction gtx, WriteCommand[] modifications, boolean onePhaseCommit) {
      super(cacheName, gtx);
      this.modifications = modifications;
      this.onePhaseCommit = onePhaseCommit;
      this.retriedCommand = false;
   }

   @ProtoFactory
   PrepareCommand(ByteString cacheName, GlobalTransaction globalTransaction, MarshallableArray<WriteCommand> wrappedModifications,
                  boolean onePhaseCommit, boolean retriedCommand) {
      super(cacheName, globalTransaction);
      this.modifications = MarshallableArray.unwrap(wrappedModifications, new WriteCommand[0]);
      this.onePhaseCommit = onePhaseCommit;
      this.retriedCommand = retriedCommand;
   }

   @ProtoField(number = 3, name = "modifications")
   MarshallableArray<WriteCommand> getWrappedModifications() {
      return MarshallableArray.create(modifications);
   }

   @ProtoField(number = 4, defaultValue = "false")
   public boolean isOnePhaseCommit() {
      return onePhaseCommit;
   }

   @ProtoField(number = 5, defaultValue = "false")
   public boolean isRetriedCommand() {
      return retriedCommand;
   }

   @Override
   public CompletionStage<?> invokeAsync(ComponentRegistry registry) throws Throwable {
      markTransactionAsRemote(true);
      RemoteTxInvocationContext ctx = createContext(registry);
      if (ctx == null) {
         return CompletableFutures.completedNull();
      }

      if (trace)
         log.tracef("Invoking remotely originated prepare: %s with invocation context: %s", this, ctx);
      CacheNotifier notifier = registry.getCacheNotifier().running();
      CompletionStage<Void> stage = notifier.notifyTransactionRegistered(ctx.getGlobalTransaction(), false);

      AsyncInterceptorChain invoker = registry.getInterceptorChain().running();
      for (VisitableCommand nested : getModifications())
         nested.init(registry);
      if (CompletionStages.isCompletedSuccessfully(stage)) {
         return invoker.invokeAsync(ctx, this);
      } else {
         return stage.thenCompose(v -> invoker.invokeAsync(ctx, this)).toCompletableFuture();
      }
   }

   @Override
   public RemoteTxInvocationContext createContext(ComponentRegistry componentRegistry) {
      RecoveryManager recoveryManager = componentRegistry.getRecoveryManager().running();
      if (recoveryManager != null && recoveryManager.isTransactionPrepared(globalTx)) {
         log.tracef("The transaction %s is already prepared. Skipping prepare call.", globalTx);
         return null;
      }

      // 1. first create a remote transaction (or get the existing one)
      TransactionTable txTable = componentRegistry.getTransactionTableRef().running();
      RemoteTransaction remoteTransaction = txTable.getOrCreateRemoteTransaction(globalTx, modifications);
      //set the list of modifications anyway, as the transaction might have already been created by a previous
      //LockControlCommand with null modifications.
      if (hasModifications()) {
         remoteTransaction.setModifications(Arrays.asList(modifications));
      }

      // 2. then set it on the invocation context
      InvocationContextFactory icf = componentRegistry.getInvocationContextFactory().running();
      return icf.createRemoteTxInvocationContext(remoteTransaction, getOrigin());
   }

   @Override
   public Collection<?> getKeysToLock() {
      if (modifications == null || modifications.length == 0) {
         return Collections.emptyList();
      }
      final Set<Object> set = new HashSet<>(modifications.length);
      forEach(modifications, writeCommand -> {
         if (writeCommand.hasAnyFlag(FlagBitSets.SKIP_LOCKING)) {
            return;
         }
         switch (writeCommand.getCommandId()) {
            case PutKeyValueCommand.COMMAND_ID:
            case RemoveCommand.COMMAND_ID:
            case ComputeCommand.COMMAND_ID:
            case ComputeIfAbsentCommand.COMMAND_ID:
            case RemoveExpiredCommand.COMMAND_ID:
            case ReplaceCommand.COMMAND_ID:
            case ReadWriteKeyCommand.COMMAND_ID:
            case ReadWriteKeyValueCommand.COMMAND_ID:
            case WriteOnlyKeyCommand.COMMAND_ID:
            case WriteOnlyKeyValueCommand.COMMAND_ID:
               set.add(((DataWriteCommand) writeCommand).getKey());
               break;
            case PutMapCommand.COMMAND_ID:
            case InvalidateCommand.COMMAND_ID:
            case ReadWriteManyCommand.COMMAND_ID:
            case ReadWriteManyEntriesCommand.COMMAND_ID:
            case WriteOnlyManyCommand.COMMAND_ID:
            case WriteOnlyManyEntriesCommand.COMMAND_ID:
               set.addAll(writeCommand.getAffectedKeys());
               break;
            default:
               break;
         }
      });
      return set;
   }

   @Override
   public Object getKeyLockOwner() {
      return globalTx;
   }

   @Override
   public boolean hasZeroLockAcquisition() {
      if (modifications == null || modifications.length == 0) {
         return false;
      }
      for (WriteCommand wc : modifications) {
         // If even a single command doesn't have the zero lock acquisition timeout flag, we can't use a zero timeout
         if (!wc.hasAnyFlag(FlagBitSets.ZERO_LOCK_ACQUISITION_TIMEOUT)) {
            return false;
         }
      }
      return true;
   }

   @Override
   public boolean hasSkipLocking() {
      return false;
   }

   @Override
   public Object acceptVisitor(InvocationContext ctx, Visitor visitor) throws Throwable {
      return visitor.visitPrepareCommand((TxInvocationContext) ctx, this);
   }

   public WriteCommand[] getModifications() {
      return modifications == null ? EMPTY_WRITE_COMMAND_ARRAY : modifications;
   }

   @Override
   public byte getCommandId() {
      return COMMAND_ID;
   }

   public PrepareCommand copy() {
      return new PrepareCommand(cacheName, globalTx, modifications == null ? null : modifications.clone(), onePhaseCommit);
   }

   @Override
   public String toString() {
      return "PrepareCommand {" +
            "modifications=" + (modifications == null ? null : Arrays.asList(modifications)) +
            ", onePhaseCommit=" + onePhaseCommit +
            ", retried=" + retriedCommand +
            ", " + super.toString();
   }

   public boolean hasModifications() {
      return modifications != null && modifications.length > 0;
   }

   public Collection<?> getAffectedKeys() {
      if (modifications == null || modifications.length == 0)
         return Collections.emptySet();

      if (modifications.length == 1) return modifications[0].getAffectedKeys();
      Set<Object> keys = new HashSet<>(modifications.length);
      for (WriteCommand wc: modifications) keys.addAll(wc.getAffectedKeys());
      return keys;
   }

   /**
    * If set to true, then the keys touched by this transaction are to be wrapped again and original ones discarded.
    */
   public boolean isReplayEntryWrapping() {
      return replayEntryWrapping;
   }

   /**
    * @see #isReplayEntryWrapping()
    */
   public void setReplayEntryWrapping(boolean replayEntryWrapping) {
      this.replayEntryWrapping = replayEntryWrapping;
   }

   @Override
   public boolean isReturnValueExpected() {
      return false;
   }

   public void setRetriedCommand(boolean retriedCommand) {
      this.retriedCommand = retriedCommand;
   }
}
