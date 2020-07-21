package org.infinispan.server.core.backup;

import static org.infinispan.server.core.backup.Constants.WORKING_DIR;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.infinispan.commons.CacheException;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.server.core.BackupManager;
import org.infinispan.server.core.logging.Log;
import org.infinispan.util.concurrent.BlockingManager;
import org.infinispan.util.concurrent.CompletableFutures;
import org.infinispan.util.logging.LogFactory;

/**
 * @author Ryan Emerson
 * @since 12.0
 */
public class BackupManagerImpl implements BackupManager {

   private static final Log log = LogFactory.getLog(BackupManagerImpl.class, Log.class);

   private final AtomicBoolean backupInProgress = new AtomicBoolean();
   private final AtomicBoolean restoreInProgress = new AtomicBoolean();

   final Path rootDir;
   final BackupReader reader;
   final BackupWriter writer;
   final Map<String, DefaultCacheManager> cacheManagers;

   public BackupManagerImpl(BlockingManager blockingManager, Map<String, DefaultCacheManager> cacheManagers,
                            Path dataRoot) {
      this.rootDir = dataRoot.resolve(WORKING_DIR);
      this.cacheManagers = cacheManagers;
      this.reader = new BackupReader(blockingManager, cacheManagers, rootDir);
      this.writer = new BackupWriter(blockingManager, cacheManagers, rootDir);
   }

   @Override
   public void init() {
      rootDir.toFile().mkdir();
   }

   @Override
   public CompletionStage<Path> create() {
      return create(
            cacheManagers.entrySet().stream()
                  .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        p -> new org.infinispan.server.core.backup.ContainerResources.Builder().importAll().build()))
      );
   }

   @Override
   public CompletionStage<Path> create(Map<String, ContainerResources> params) {
      if (!backupInProgress.compareAndSet(false, true))
         return failedBackupLockFuture();

      log.initiatingClusterBackup();
      return writer.create(params)
            .whenComplete(this::releaseWriterLock);
   }

   @Override
   public CompletionStage<Void> restore(InputStream is) {
      return restore(
            is,
            cacheManagers.entrySet().stream()
                  .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        p -> new org.infinispan.server.core.backup.ContainerResources.Builder().importAll().build()))
      );
   }

   @Override
   public CompletionStage<Void> restore(InputStream is, Map<String, ContainerResources> params) {
      if (!restoreInProgress.compareAndSet(false, true))
         return failedRestoreInProgress();

      log.initiatingClusterRestore();
      return reader.restore(is, params)
            .whenComplete(this::releaseImportLock);
   }

   private CompletionStage<Path> failedBackupLockFuture() {
      CacheException e = log.backupInProgress();
      log.debug(e);
      return CompletableFutures.completedExceptionFuture(e);
   }

   private CompletionStage<Void> failedRestoreInProgress() {
      CacheException e = log.restoreInProgress();
      log.debug(e);
      return CompletableFutures.completedExceptionFuture(e);
   }

   private void releaseWriterLock(Path path, Throwable t) {
      backupInProgress.compareAndSet(true, false);
      if (t != null) {
         log.debug("Exception encountered when creating a cluster backup", t);
         throw log.errorCreatingBackup(t);
      }
      log.backupComplete(path.getFileName().toString());
   }

   private void releaseImportLock(Void ignore, Throwable t) {
      restoreInProgress.compareAndSet(true, false);
      if (t != null) {
         log.debug("Exception encountered when restoring a cluster backup", t);
         throw log.errorRestoringBackup(t);
      }
      log.restoreComplete();
   }
}
