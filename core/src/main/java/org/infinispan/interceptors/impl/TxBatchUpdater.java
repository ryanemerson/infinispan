package org.infinispan.interceptors.impl;

import static org.infinispan.persistence.PersistenceUtil.internalMetadata;
import static org.infinispan.persistence.manager.PersistenceManager.AccessMode.BOTH;
import static org.infinispan.persistence.manager.PersistenceManager.AccessMode.PRIVATE;

import java.util.Map;
import java.util.Set;

import org.infinispan.commands.AbstractVisitor;
import org.infinispan.commands.FlagAffectedCommand;
import org.infinispan.commands.functional.AbstractWriteManyCommand;
import org.infinispan.commands.functional.ReadWriteKeyCommand;
import org.infinispan.commands.functional.ReadWriteKeyValueCommand;
import org.infinispan.commands.functional.ReadWriteManyCommand;
import org.infinispan.commands.functional.ReadWriteManyEntriesCommand;
import org.infinispan.commands.functional.WriteOnlyKeyCommand;
import org.infinispan.commands.functional.WriteOnlyKeyValueCommand;
import org.infinispan.commands.functional.WriteOnlyManyCommand;
import org.infinispan.commands.functional.WriteOnlyManyEntriesCommand;
import org.infinispan.commands.write.ClearCommand;
import org.infinispan.commands.write.PutKeyValueCommand;
import org.infinispan.commands.write.PutMapCommand;
import org.infinispan.commands.write.RemoveCommand;
import org.infinispan.commands.write.ReplaceCommand;
import org.infinispan.container.entries.CacheEntry;
import org.infinispan.container.entries.InternalCacheValue;
import org.infinispan.container.impl.InternalEntryFactory;
import org.infinispan.context.InvocationContext;
import org.infinispan.marshall.core.MarshalledEntry;
import org.infinispan.marshall.core.MarshalledEntryFactory;
import org.infinispan.persistence.manager.PersistenceManager;
import org.infinispan.persistence.support.BatchModification;

/**
 * @author Ryan Emerson
 * @since 9.1
 */
public class TxBatchUpdater extends AbstractVisitor {
   private final CacheWriterInterceptor cwi;
   private final PersistenceManager persistenceManager;
   private final InternalEntryFactory entryFactory;
   private final MarshalledEntryFactory marshalledEntryFactory;
   private final BatchModification modifications;
   private final BatchModification nonSharedModifications;
   private final boolean generateStatistics;
   private int putCount;

   static TxBatchUpdater createNonTxStoreUpdater(CacheWriterInterceptor interceptor, PersistenceManager persistenceManager,
                                                 InternalEntryFactory entryFactory, MarshalledEntryFactory marshalledEntryFactory) {
      return new TxBatchUpdater(interceptor, persistenceManager, entryFactory, marshalledEntryFactory,
            new BatchModification(null), new BatchModification(null));
   }

   static TxBatchUpdater createTxStoreUpdater(PersistenceManager persistenceManager, InternalEntryFactory entryFactory,
                                              MarshalledEntryFactory marshalledEntryFactory, Set<Object> affectedKeys) {
      return new TxBatchUpdater(null, persistenceManager, entryFactory, marshalledEntryFactory, new BatchModification(affectedKeys), null);
   }

   private TxBatchUpdater(CacheWriterInterceptor cwi, PersistenceManager persistenceManager, InternalEntryFactory entryFactory,
                          MarshalledEntryFactory marshalledEntryFactory, BatchModification modifications, BatchModification nonSharedModifications) {
      this.cwi = cwi;
      this.persistenceManager = persistenceManager;
      this.entryFactory = entryFactory;
      this.marshalledEntryFactory = marshalledEntryFactory;
      this.modifications = modifications;
      this.nonSharedModifications = nonSharedModifications;
      this.generateStatistics = cwi != null && cwi.getStatisticsEnabled();
   }

   @Override
   public Object visitPutKeyValueCommand(InvocationContext ctx, PutKeyValueCommand command) throws Throwable {
      return visitSingleStore(ctx, command, command.getKey());
   }

   @Override
   public Object visitReplaceCommand(InvocationContext ctx, ReplaceCommand command) throws Throwable {
      return visitSingleStore(ctx, command, command.getKey());
   }

   @Override
   public Object visitPutMapCommand(InvocationContext ctx, PutMapCommand command) throws Throwable {
      Map<Object, Object> map = command.getMap();
      for (Object key : map.keySet())
         visitSingleStore(ctx, command, key);
      return null;
   }

   @Override
   public Object visitWriteOnlyKeyCommand(InvocationContext ctx, WriteOnlyKeyCommand command) throws Throwable {
      return visitModify(ctx, command, command.getKey());
   }

   @Override
   public Object visitReadWriteKeyValueCommand(InvocationContext ctx, ReadWriteKeyValueCommand command) throws Throwable {
      return visitModify(ctx, command, command.getKey());
   }

   @Override
   public Object visitReadWriteKeyCommand(InvocationContext ctx, ReadWriteKeyCommand command) throws Throwable {
      return visitModify(ctx, command, command.getKey());
   }

   @Override
   public Object visitWriteOnlyManyEntriesCommand(InvocationContext ctx, WriteOnlyManyEntriesCommand command) throws Throwable {
      return visitManyModify(ctx, command);
   }

   @Override
   public Object visitWriteOnlyKeyValueCommand(InvocationContext ctx, WriteOnlyKeyValueCommand command) throws Throwable {
      return visitModify(ctx, command, command.getKey());
   }

   @Override
   public Object visitWriteOnlyManyCommand(InvocationContext ctx, WriteOnlyManyCommand command) throws Throwable {
      return visitManyModify(ctx, command);
   }

   @Override
   public Object visitReadWriteManyCommand(InvocationContext ctx, ReadWriteManyCommand command) throws Throwable {
      return visitManyModify(ctx, command);
   }

   @Override
   public Object visitReadWriteManyEntriesCommand(InvocationContext ctx, ReadWriteManyEntriesCommand command) throws Throwable {
      return visitManyModify(ctx, command);
   }

   @Override
   public Object visitRemoveCommand(InvocationContext ctx, RemoveCommand command) throws Throwable {
      Object key = command.getKey();
      if (isProperWriter(ctx, command, key)) {
         getModifications(ctx, key, command).removeEntry(key);
      }
      return null;
   }

   @Override
   public Object visitClearCommand(InvocationContext ctx, ClearCommand command) throws Throwable {
      persistenceManager.clearAllStores(ctx.isOriginLocal() ? PRIVATE : BOTH);
      return null;
   }

   private Object visitSingleStore(InvocationContext ctx, FlagAffectedCommand command, Object key) {
      if (isProperWriter(ctx, command, key)) {
         if (generateStatistics) putCount++;
         InternalCacheValue sv = entryFactory.getValueFromCtx(key, ctx);
         if (sv != null && sv.getValue() != null) {
            MarshalledEntry me = marshalledEntryFactory.newMarshalledEntry(key, sv.getValue(), internalMetadata(sv));
            getModifications(ctx, key, command).addMarshalledEntry(key, me);
         }
      }
      return null;
   }

   private Object visitModify(InvocationContext ctx, FlagAffectedCommand command, Object key) {
      if (isProperWriter(ctx, command, key)) {
         CacheEntry entry = ctx.lookupEntry(key);
         if (!entry.isChanged()) {
            return null;
         } else if (entry.getValue() == null) {
            getModifications(ctx, key, command).removeEntry(key);
         } else {
            if (generateStatistics) putCount++;
            InternalCacheValue sv = entryFactory.getValueFromCtx(key, ctx);
            if (sv != null) {
               MarshalledEntry me = marshalledEntryFactory.newMarshalledEntry(key, sv.getValue(), internalMetadata(sv));
               getModifications(ctx, key, command).addMarshalledEntry(key, me);
            }
         }
      }
      return null;
   }

   private Object visitManyModify(InvocationContext ctx, AbstractWriteManyCommand command) throws Throwable {
      for (Object key : command.getAffectedKeys()) {
         visitModify(ctx, command, key);
      }
      return null;
   }

   private BatchModification getModifications(InvocationContext ctx, Object key, FlagAffectedCommand command) {
      if (cwi != null && cwi.skipSharedStores(ctx, key, command))
         return nonSharedModifications;
      return modifications;
   }

   private boolean isProperWriter(InvocationContext ctx, FlagAffectedCommand command, Object key) {
      return cwi == null || cwi.isProperWriter(ctx, command, key);
   }

   BatchModification getModifications() {
      return modifications;
   }

   BatchModification getNonSharedModifications() {
      return nonSharedModifications;
   }

   int getPutCount() {
      return putCount;
   }
}
