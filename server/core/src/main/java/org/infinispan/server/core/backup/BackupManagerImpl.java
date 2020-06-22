package org.infinispan.server.core.backup;

import static org.infinispan.server.core.backup.Constants.WORKING_DIR;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

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

   final Path rootDir;
   final BackupReader reader;
   final BackupWriter writer;
   final Lock backupLock;
   final Lock restoreLock;
   final Map<String, DefaultCacheManager> cacheManagers;

   public BackupManagerImpl(BlockingManager blockingManager, EmbeddedCacheManager cm,
                            Map<String, DefaultCacheManager> cacheManagers, Path dataRoot) {
      this.rootDir = dataRoot.resolve(WORKING_DIR);
      this.cacheManagers = cacheManagers;
      this.reader = new BackupReader(blockingManager, cacheManagers, rootDir);
      this.writer = new BackupWriter(blockingManager, cacheManagers, rootDir);
      this.backupLock = new Lock("backup", cm);
      this.restoreLock = new Lock("restore", cm);
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
                        p -> new BackupManagerResources.Builder().includeAll().build()))
      );
   }

   @Override
   public CompletionStage<Path> create(Map<String, Resources> params) {
      CompletionStage<Path> backupStage = backupLock.lock()
            .thenCompose(lockAcquired -> {
               if (!lockAcquired)
                  return CompletableFutures.completedExceptionFuture(log.backupInProgress());

               log.initiatingClusterBackup();
               return writer.create(params);
            });

      return CompletionStages.handleAndCompose(backupStage,
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
