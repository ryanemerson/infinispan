package org.infinispan.server.core.backup;

import java.nio.file.Path;
import java.util.Collection;
import java.util.stream.Collectors;

import org.infinispan.configuration.parsing.ParserRegistry;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.server.core.BackupManager;
import org.infinispan.util.concurrent.BlockingManager;

/**
 * // TODO: Document this
 *
 * @author Ryan Emerson
 * @since 11.0
 */
class ContainerResourceFactory {

   private final ParserRegistry parserRegistry = new ParserRegistry();
   private final BlockingManager blockingManager;
   private final EmbeddedCacheManager cm;
   private final Path containerRoot;

   ContainerResourceFactory(BlockingManager blockingManager, EmbeddedCacheManager cm, Path containerRoot) {
      this.blockingManager = blockingManager;
      this.cm = cm;
      this.containerRoot = containerRoot;
   }

   Collection<ContainerResource> getResources(BackupManager.Parameters params) {
      return params.includedResourceTypes().stream()
            .map(type -> get(type, params))
            .collect(Collectors.toList());
   }

   ContainerResource get(BackupManager.ResourceType type, BackupManager.Parameters params) {
      switch (type) {
         case CACHES:
            return new CacheResource(blockingManager, parserRegistry, cm, params, containerRoot);
         case CACHE_CONFIGURATIONS:
            return new CacheConfigResource(blockingManager, parserRegistry, cm, params, containerRoot);
         default:
            throw new IllegalStateException("TODO remove");
      }
   }
}
