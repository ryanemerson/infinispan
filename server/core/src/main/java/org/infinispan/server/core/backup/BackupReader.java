package org.infinispan.server.core.backup;

import static org.infinispan.server.core.backup.Constants.CONTAINERS_PROPERTIES_FILE;
import static org.infinispan.server.core.backup.Constants.CONTAINER_KEY;
import static org.infinispan.server.core.backup.Constants.MANIFEST_PROPERTIES_FILE;
import static org.infinispan.server.core.backup.Constants.STAGING_ZIP;
import static org.infinispan.server.core.backup.Constants.VERSION_KEY;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.zip.ZipFile;

import org.infinispan.commons.CacheException;
import org.infinispan.commons.logging.LogFactory;
import org.infinispan.commons.util.Version;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.server.core.BackupManager;
import org.infinispan.server.core.backup.resources.ContainerResourceFactory;
import org.infinispan.server.core.logging.Log;
import org.infinispan.util.concurrent.AggregateCompletionStage;
import org.infinispan.util.concurrent.BlockingManager;
import org.infinispan.util.concurrent.CompletionStages;

/**
 * Responsible for reading backup bytes and restoring the contents to the appropriate cache manager.
 *
 * @author Ryan Emerson
 * @since 12.0
 */
class BackupReader {

   private static final Log log = LogFactory.getLog(BackupReader.class, Log.class);

   private final BlockingManager blockingManager;
   private final Map<String, DefaultCacheManager> cacheManagers;
   private final Path rootDir;

   public BackupReader(BlockingManager blockingManager, Map<String, DefaultCacheManager> cacheManagers, Path rootDir) {
      this.blockingManager = blockingManager;
      this.cacheManagers = cacheManagers;
      this.rootDir = rootDir;
   }

   CompletionStage<Void> restore(InputStream is, Map<String, BackupManager.ContainerResources> params) {
      final Path stagingFile = rootDir.resolve(STAGING_ZIP);

      CompletionStage<?> processContainers = blockingManager.supplyBlocking(() -> {
         try {
            Files.copy(is, stagingFile);
         } catch (IOException e) {
            throw new CacheException(e);
         }

         try (ZipFile zip = new ZipFile(stagingFile.toFile())) {
            Properties manifest = readManifestAndValidate(zip);

            List<String> backupContainers = Arrays.asList(manifest.getProperty(CONTAINER_KEY).split(","));
            Set<String> requestedContainers = new HashSet<>(params.keySet());
            requestedContainers.removeAll(backupContainers);
            if (!requestedContainers.isEmpty()) {
               throw log.unableToFindBackupResource("Containers", requestedContainers);
            }

            AggregateCompletionStage<Void> stages = CompletionStages.aggregateCompletionStage();
            for (Map.Entry<String, BackupManager.ContainerResources> e : params.entrySet()) {
               stages.dependsOn(restoreContainer(e.getKey(), e.getValue(), zip));
            }
            return stages.freeze();
         } catch (IOException e) {
            throw new CacheException(String.format("Unable to read zip file '%s'", stagingFile));
         }
      }, "process-containers");

      return processContainers.thenRun(() -> {
         try {
            Files.delete(stagingFile);
         } catch (IOException e) {
            log.errorf("Unable to remove '%s'", stagingFile, e);
         }
      });
   }

   private CompletionStage<Void> restoreContainer(String containerName, BackupManager.ContainerResources params, ZipFile zip) {
      // TODO validate container config
      EmbeddedCacheManager cm = cacheManagers.get(containerName);
      Path containerRoot = Paths.get(CONTAINER_KEY, containerName);

      Properties properties = readProperties(containerRoot.resolve(CONTAINERS_PROPERTIES_FILE), zip);

      Collection<ContainerResource> resources = ContainerResourceFactory.getInstance()
            .getResources(params, blockingManager, cm, containerRoot);

      resources.forEach(r -> r.prepareAndValidateRestore(properties));

      AggregateCompletionStage<Void> stages = CompletionStages.aggregateCompletionStage();
      for (ContainerResource cr : resources)
         stages.dependsOn(cr.restore(zip));
      return stages.freeze();
   }

   private Properties readManifestAndValidate(ZipFile zip) {
      Path manifestPath = Paths.get(MANIFEST_PROPERTIES_FILE);
      Properties properties = readProperties(manifestPath, zip);
      String version = properties.getProperty(VERSION_KEY);
      if (version == null)
         throw new IllegalStateException("Missing manifest version");

      int majorVersion = Integer.parseInt(version.split("[\\.\\-]")[0]);
      // TODO replace with check that version difference is in the supported range, i.e. across 3 majors etc
      if (majorVersion < 12)
         throw new CacheException(String.format("Unable to restore from a backup as '%s' is no longer supported in '%s %s'",
               version, Version.getBrandName(), Version.getVersion()));
      return properties;
   }

   private Properties readProperties(Path file, ZipFile zip) {
      try (InputStream is = zip.getInputStream(zip.getEntry(file.toString()))) {
         Properties props = new Properties();
         props.load(is);
         return props;
      } catch (IOException e) {
         throw new CacheException("Unable to read properties file", e);
      }
   }
}
