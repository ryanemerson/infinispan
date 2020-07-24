package org.infinispan.server.core.backup;

import static org.infinispan.server.core.backup.Constants.WORKING_DIR;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.infinispan.commons.CacheException;
import org.infinispan.configuration.parsing.ParserRegistry;
import org.infinispan.lock.EmbeddedClusteredLockManagerFactory;
import org.infinispan.lock.api.ClusteredLock;
import org.infinispan.lock.api.ClusteredLockManager;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.server.core.BackupManager;
import org.infinispan.server.core.logging.Log;
import org.infinispan.util.concurrent.BlockingManager;
import org.infinispan.util.concurrent.CompletableFutures;
import org.infinispan.util.concurrent.CompletionStages;
import org.infinispan.util.logging.LogFactory;

/**
 * @author Ryan Emerson
 * @since 12.0
 */
public class BackupManagerImpl implements BackupManager {

   private static final Log log = LogFactory.getLog(BackupManagerImpl.class, Log.class);

   final ParserRegistry parserRegistry;
   final BlockingManager blockingManager;
   final Path rootDir;
   final BackupReader reader;
   final Lock backupLock;
   final Lock restoreLock;
   final Map<String, DefaultCacheManager> cacheManagers;
   final Map<String, CompletableFuture<Path>> backupMap;

   public BackupManagerImpl(BlockingManager blockingManager, EmbeddedCacheManager cm,
                            Map<String, DefaultCacheManager> cacheManagers, Path dataRoot) {
      this.blockingManager = blockingManager;
      this.rootDir = dataRoot.resolve(WORKING_DIR);
      this.cacheManagers = cacheManagers;
      this.parserRegistry = new ParserRegistry();
      this.reader = new BackupReader(blockingManager, cacheManagers, parserRegistry, rootDir);
      this.backupLock = new Lock("backup", cm);
      this.restoreLock = new Lock("restore", cm);
      this.backupMap = new ConcurrentHashMap<>();
   }

   @Override
   public void init() {
      rootDir.toFile().mkdir();
   }

   @Override
   public Status getBackupStatus(String name) {
      return getBackupStatus(backupMap.get(name));
   }

   @Override
   public Path getBackup(String name) {
      CompletableFuture<Path> future = backupMap.get(name);
      Status status = getBackupStatus(future);
      if (status != Status.COMPLETE)
         throw new IllegalStateException(String.format("Backup '%s' not complete, current Status %s", name, status));
      return future.join();
   }

   private Status getBackupStatus(CompletableFuture<Path> future) {
      if (future == null)
         return Status.NOT_FOUND;

      if (future.isCompletedExceptionally())
         return Status.FAILED;

      return future.isDone() ? Status.COMPLETE : Status.IN_PROGRESS;
   }

   @Override
   public CompletionStage<Void> removeBackup(String name) {
      CompletableFuture<Path> future = backupMap.remove(name);
      Status status = getBackupStatus(future);
      if (status == Status.NOT_FOUND)
         return CompletableFutures.completedNull();

      return blockingManager.runBlocking(() -> {
         if (status == Status.IN_PROGRESS)
            future.cancel(true);

         try {
            // Remove the zip file and the working directory
            Path zip = future.join();
            Files.delete(zip);
            Files.delete(zip.getParent());
         } catch (IOException e) {
            throw new CacheException(String.format("Unable to delete backup '%s'", name));
         }
      }, "remove-backup-file");
   }

   @Override
   public CompletionStage<Path> create(String name) {
      return create(
            name,
            cacheManagers.entrySet().stream()
                  .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        p -> new BackupManagerResources.Builder().includeAll().build()))
      );
   }

   @Override
   public CompletionStage<Path> create(String name, Map<String, Resources> params) {
      BackupWriter writer = new BackupWriter(name, blockingManager, cacheManagers, parserRegistry, rootDir);
      CompletionStage<Path> backupStage = backupLock.lock()
            .thenCompose(lockAcquired -> {
               if (!lockAcquired)
                  return CompletableFutures.completedExceptionFuture(log.backupInProgress());

               log.initiatingClusterBackup();
               return writer.create(params);
            });

      backupStage = CompletionStages.handleAndCompose(backupStage,
            (path, t) -> {
               CompletionStage<Void> unlock = backupLock.unlock();
               if (t != null) {
                  log.debug("Exception encountered when creating a cluster backup", t);
                  return unlock.thenCompose(ignore ->
                        CompletableFutures.completedExceptionFuture(log.errorCreatingBackup(t))
                  );
               }
               log.backupComplete(path.getFileName().toString());
               return unlock.thenCompose(ignore -> CompletableFuture.completedFuture(path));
            });

      backupMap.put(name, backupStage.toCompletableFuture());
      return backupStage;
   }

   @Override
   public CompletionStage<Void> restore(InputStream is) {
      return restore(
            is,
            cacheManagers.entrySet().stream()
                  .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        p -> new BackupManagerResources.Builder().includeAll().build()))
      );
   }

   @Override
   public CompletionStage<Void> restore(InputStream is, Map<String, Resources> params) {
      CompletionStage<Void> restoreStage = restoreLock.lock()
            .thenCompose(lockAcquired -> {
               if (!lockAcquired)
                  return CompletableFutures.completedExceptionFuture(log.restoreInProgress());

               log.initiatingClusterRestore();
               return reader.restore(is, params);
            });

      return CompletionStages.handleAndCompose(restoreStage,
            (path, t) -> {
               CompletionStage<Void> unlock = restoreLock.unlock();
               if (t != null) {
                  log.debug("Exception encountered when restoring a cluster backup", t);
                  return unlock.thenCompose(ignore ->
                        CompletableFutures.completedExceptionFuture(log.errorRestoringBackup(t))
                  );
               }
               log.restoreComplete();
               return unlock.thenCompose(ignore -> CompletableFuture.completedFuture(path));
            });
   }

   static class Lock {
      final String name;
      final EmbeddedCacheManager cm;
      final boolean isClustered;
      volatile ClusteredLock clusteredLock;
      volatile AtomicBoolean localLock;

      Lock(String name, EmbeddedCacheManager cm) {
         this.name = String.format("%s-%s", BackupManagerImpl.class.getSimpleName(), name);
         this.cm = cm;
         this.isClustered = cm.getCacheManagerConfiguration().isClustered();
      }

      CompletionStage<Boolean> lock() {
         if (isClustered)
            return getClusteredLock().tryLock();

         return CompletableFuture.completedFuture(getLocalLock().compareAndSet(false, true));
      }

      CompletionStage<Void> unlock() {
         if (isClustered)
            return getClusteredLock().unlock();

         getLocalLock().compareAndSet(true, false);
         return CompletableFutures.completedNull();
      }

      private ClusteredLock getClusteredLock() {
         if (clusteredLock == null) {
            synchronized (this) {
               if (clusteredLock == null) {
                  ClusteredLockManager lockManager = EmbeddedClusteredLockManagerFactory.from(cm);
                  boolean isDefined = lockManager.isDefined(name);
                  if (!isDefined) {
                     lockManager.defineLock(name);
                  }
                  clusteredLock = lockManager.get(name);
               }
            }
         }
         return clusteredLock;
      }

      private AtomicBoolean getLocalLock() {
         if (localLock == null)
            localLock = new AtomicBoolean();
         return localLock;
      }
   }
}
