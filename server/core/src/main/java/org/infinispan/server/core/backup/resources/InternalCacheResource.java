package org.infinispan.server.core.backup.resources;

import static org.infinispan.server.core.BackupManager.ResourceType.PROTO_SCHEMAS;
import static org.infinispan.server.core.BackupManager.ResourceType.SCRIPTS;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.commons.CacheException;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.server.core.BackupManager;
import org.infinispan.util.concurrent.BlockingManager;

/**
 * // TODO: Document this
 *
 * @author Ryan Emerson
 * @since 11.0
 */
class InternalCacheResource extends AbstractContainerResource {

   private static final Map<BackupManager.ResourceType, String> cacheMap = new HashMap<>(2);

   static {
      cacheMap.put(PROTO_SCHEMAS, "___protobuf_metadata");
      cacheMap.put(SCRIPTS, "___script_cache");
   }

   private final AdvancedCache<String, String> cache;

   InternalCacheResource(BackupManager.ResourceType type, BlockingManager blockingManager, EmbeddedCacheManager cm,
                         BackupManager.Parameters params, Path root) {
      super(type, blockingManager, cm, params, root);

      Cache<String, String> internalCache = cm.getCache(cacheMap.get(type));
      if (internalCache == null)
         throw log.missingBackupResourceModule(type);
      this.cache = internalCache.getAdvancedCache();
   }

   @Override
   public CompletionStage<Void> backup() {
      return blockingManager.runBlocking(() -> {
         Set<String> allowList = qualifiedResources;
         if (wildcard)
            allowList.addAll(cache.keySet());

         root.toFile().mkdir();
         for (Map.Entry<String, String> entry : cache.entrySet()) {
            String fileName = entry.getKey();
            if (allowList.contains(fileName)) {
               Path file = root.resolve(fileName);
               try {
                  Files.write(file, entry.getValue().getBytes(StandardCharsets.UTF_8));
               } catch (IOException e) {
                  throw new CacheException(String.format("Unable to create %s", file), e);
               }
            }
         }
      }, "write-" + type.toString());
   }

   @Override
   public CompletionStage<Void> restore(Properties properties, ZipFile zip) {
      return blockingManager.runBlocking(() -> {
         for (String file : resourcesToRestore(properties)) {
            String zipPath = root.resolve(file).toString();
            try (InputStream is = zip.getInputStream(zip.getEntry(zipPath));
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
               String content = reader.lines().collect(Collectors.joining("\n"));
               cache.put(file, content);
            } catch (IOException e) {
               throw new CacheException(e);
            }
         }
      }, "restore-" + type.toString());
   }
}
