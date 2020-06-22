package org.infinispan.server.core.backup;

import static org.infinispan.server.core.backup.BackupUtil.BACKUP_WORKING_DIR;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;

import org.infinispan.commons.CacheException;
import org.infinispan.factories.scopes.Scope;
import org.infinispan.factories.scopes.Scopes;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.server.core.BackupManager;
import org.infinispan.server.core.CacheIgnoreManager;
import org.infinispan.util.concurrent.BlockingManager;
import org.infinispan.util.concurrent.CompletableFutures;

/**
 * @author Ryan Emerson
 * @since 11.0
 */
@Scope(Scopes.GLOBAL)
public class BackupManagerImpl implements BackupManager {

   private final AtomicBoolean backupInProgress = new AtomicBoolean();
   private final AtomicBoolean importInProgress = new AtomicBoolean();

   final Path rootDir;
   final BackupReader reader;
   final BackupWriter writer;
   final BlockingManager blockingManager;

   public BackupManagerImpl(BlockingManager blockingManager, Map<String, DefaultCacheManager> cacheManagers,
                            CacheIgnoreManager cacheIgnoreManager, Path dataRoot) {
      this.blockingManager = blockingManager;
      this.rootDir = dataRoot.resolve(BACKUP_WORKING_DIR);
      this.reader = new BackupReader(blockingManager, cacheManagers, rootDir);
      this.writer = new BackupWriter(blockingManager, cacheManagers, cacheIgnoreManager, rootDir);
      rootDir.toFile().mkdir();
   }

   @Override
   public CompletionStage<Path> create() {
      if (!backupInProgress.compareAndSet(false, true))
         return failedBackupLockFuture();

      return writer.create()
            .whenComplete(this::releaseWriterLock);
   }

   @Override
   public CompletionStage<Path> create(String containerName) {
      if (!backupInProgress.compareAndSet(false, true))
         return failedBackupLockFuture();

      return writer.create(containerName)
            .whenComplete(this::releaseWriterLock);
   }

   @Override
   public CompletionStage<Path> create(String containerName, String cacheName) {
      if (!backupInProgress.compareAndSet(false, true))
         return failedBackupLockFuture();

      return writer.create(containerName, cacheName)
            .whenComplete(this::releaseWriterLock);
   }

   @Override
   public CompletionStage<Void> restore(byte[] backup) {
      if (!importInProgress.compareAndSet(false, true))
         return failedImportLockFuture();
      return reader.restore(backup);
   }

   @Override
   public CompletionStage<Void> restore(byte[] backup, String cacheName) {
      // TODO
      return restore(backup);
   }

   private CompletionStage<Path> failedBackupLockFuture() {
      return CompletableFutures.completedExceptionFuture(new CacheException("Unable to acquire the backup lock, backup currently in progress"));
   }

   private CompletionStage<Void> failedImportLockFuture() {
      return CompletableFutures.completedExceptionFuture(new CacheException("Unable to acquire the import lock, import currently in progress"));
   }

   private void releaseWriterLock(Path path, Throwable t) {
      assert backupInProgress.compareAndSet(true, false);
      handleExceptions(t);
   }

   private void releaseImportLock(Path path, Throwable t) {
      assert importInProgress.compareAndSet(true, false);
      handleExceptions(t);
   }

   private void handleExceptions(Throwable t) {
      if (t != null) {
         // TODO remove failed backup files?
         if (t instanceof CacheException) {
            throw (CacheException) t;
         } else if (t.getCause() instanceof CacheException) {
            throw (CacheException) t.getCause();
         }
         throw new CacheException(t);

      }
   }
}
