package org.infinispan.server.core.backup;

import static org.infinispan.server.core.backup.BackupUtil.asSet;
import static org.infinispan.server.core.backup.BackupUtil.resolve;

import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.infinispan.commons.logging.LogFactory;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.server.core.BackupManager;
import org.infinispan.server.core.logging.Log;
import org.infinispan.util.concurrent.BlockingManager;

/**
 * // TODO: Document this
 *
 * @author Ryan Emerson
 * @since 11.0
 */
abstract class AbstractContainerResource implements ContainerResource {

   protected static final Log log = LogFactory.getLog(MethodHandles.lookup().lookupClass(), Log.class);

   final BackupManager.ResourceType type;
   final BackupManager.Parameters params;
   final Path root;
   final BlockingManager blockingManager;
   final EmbeddedCacheManager cm;
   final boolean wildcard;
   final Set<String> qualifiedResources;

   // TODO make order consistent with implementations
   protected AbstractContainerResource(BackupManager.ResourceType type, BackupManager.Parameters params, Path root,
                                       BlockingManager blockingManager, EmbeddedCacheManager cm) {
      this.type = type;
      this.params = params;
      this.root = resolve(root, type); // TODO move to this class?
      this.blockingManager = blockingManager;
      this.cm = cm;
      Set<String> qualifiedResources = params.getQualifiedResources(type);
      this.wildcard = qualifiedResources == null;
      this.qualifiedResources = ConcurrentHashMap.newKeySet();
      if (!wildcard)
         this.qualifiedResources.addAll(qualifiedResources);
   }

   @Override
   public void writeToManifest(Properties properties) {
      properties.put(type.toString(), String.join(",", qualifiedResources));
   }

   @Override
   public Set<String> resourcesToRestore(Properties properties) {
      // Only process specific resources if specified
      Set<String> resourcesToProcess = asSet(properties, type);

      if (!wildcard) {
         resourcesToProcess.retainAll(qualifiedResources);

         if (resourcesToProcess.isEmpty()) {
            Set<String> missingResources = new HashSet<>(qualifiedResources);
            missingResources.removeAll(resourcesToProcess);
            throw log.unableToFindBackupResource(type, missingResources);
         }
      }
      return resourcesToProcess;
   }
}
