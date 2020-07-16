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
 * Factory for creating the {@link ContainerResource}s required for a backup/restore operation.
 *
 * @author Ryan Emerson
 * @since 12.0
 */
public class ContainerResourceFactory {

   private static final Log log = LogFactory.getLog(ContainerResourceFactory.class, Log.class);

   private static volatile ContainerResourceFactory instance;

   public static ContainerResourceFactory getInstance() {
      if (instance == null) {
         synchronized (ContainerResourceFactory.class) {
            instance = new ContainerResourceFactory();
         }
      }
      return instance;
   }

   private final ParserRegistry parserRegistry;

   private ContainerResourceFactory() {
      this.parserRegistry = new ParserRegistry();
   }

   public Collection<ContainerResource> getResources(BackupManager.Parameters params, BlockingManager blockingManager,
                                                     EmbeddedCacheManager cm, Path containerRoot) {
      GlobalComponentRegistry gcr = cm.getGlobalComponentRegistry();
      return params.includeTypes().stream()
            .map(type -> {
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
                  default:
                     throw new IllegalStateException();
               }
            })
            .collect(Collectors.toList());
   }
}
