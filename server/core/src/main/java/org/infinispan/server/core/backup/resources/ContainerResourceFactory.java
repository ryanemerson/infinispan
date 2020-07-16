package org.infinispan.server.core.backup.resources;

import static org.infinispan.server.core.BackupManager.ResourceType.COUNTERS;

import java.nio.file.Path;
import java.util.Collection;
import java.util.stream.Collectors;

import org.infinispan.commons.logging.LogFactory;
import org.infinispan.configuration.parsing.ParserRegistry;
import org.infinispan.counter.api.CounterManager;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.server.core.BackupManager;
import org.infinispan.server.core.backup.ContainerResource;
import org.infinispan.server.core.logging.Log;
import org.infinispan.util.concurrent.BlockingManager;

/**
 * // TODO: Document this
 *
 * @author Ryan Emerson
 * @since 11.0
 */
public class ContainerResourceFactory {

   private static final Log log = LogFactory.getLog(ContainerResourceFactory.class, Log.class);

   private final ParserRegistry parserRegistry = new ParserRegistry();
   private final BlockingManager blockingManager;
   private final EmbeddedCacheManager cm;
   private final Path containerRoot;

   public ContainerResourceFactory(BlockingManager blockingManager, EmbeddedCacheManager cm, Path containerRoot) {
      this.blockingManager = blockingManager;
      this.cm = cm;
      this.containerRoot = containerRoot;
   }

   public Collection<ContainerResource> getResources(BackupManager.Parameters params) {
      return params.includedResourceTypes().stream()
            .map(type -> get(type, params))
            .collect(Collectors.toList());
   }

   private ContainerResource get(BackupManager.ResourceType type, BackupManager.Parameters params) {
      GlobalComponentRegistry gcr = cm.getGlobalComponentRegistry();
      switch (type) {
         case CACHES:
            return new CacheResource(blockingManager, parserRegistry, cm, params, containerRoot);
         case CACHE_CONFIGURATIONS:
            return new CacheConfigResource(blockingManager, parserRegistry, cm, params, containerRoot);
         case COUNTERS:
            CounterManager counterManager = gcr.getComponent(CounterManager.class);
            if (counterManager == null) {
               throw log.missingBackupResourceModule(COUNTERS);
            }
            return new CounterResource(blockingManager, cm, params, containerRoot);
         case PROTO_SCHEMAS:
         case SCRIPTS:
            return new InternalCacheResource(type, blockingManager, cm, params, containerRoot);
      }
      throw new IllegalStateException();
   }
}
