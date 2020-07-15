package org.infinispan.server.core.backup;

import static org.infinispan.server.core.BackupManager.ResourceType.CACHE_CONFIGURATIONS;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.zip.ZipFile;

import javax.xml.stream.XMLStreamException;

import org.infinispan.commons.CacheException;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ParserRegistry;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.server.core.BackupManager;
import org.infinispan.util.concurrent.BlockingManager;

class CacheConfigResource extends AbstractContainerResource {

   final ParserRegistry parserRegistry;

   public CacheConfigResource(BlockingManager blockingManager, ParserRegistry parserRegistry, EmbeddedCacheManager cm,
                              BackupManager.Parameters params, Path root) {
      super(CACHE_CONFIGURATIONS, params, root, blockingManager, cm);
      this.parserRegistry = parserRegistry;
   }

   @Override
   public CompletionStage<Void> backup() {
      return blockingManager.runBlocking(() -> {
         root.toFile().mkdir();
         Set<String> configNames = wildcard ? cm.getCacheConfigurationNames() : qualifiedResources;

         for (String configName : configNames) {
            Configuration config = cm.getCacheConfiguration(configName);
            if (wildcard) {
               if (config.isTemplate()) {
                  qualifiedResources.add(configName);
               } else {
                  configNames.remove(configName);
                  continue;
               }
            } else if (!config.isTemplate()){
               throw new CacheException(String.format("Unable to backup %s '%s' as it is not a template", CACHE_CONFIGURATIONS, configName));
            }

            String fileName = configFile(configName);
            Path xmlPath = root.resolve(String.format("%s.xml", configName));
            try (OutputStream os = Files.newOutputStream(xmlPath)) {
               parserRegistry.serialize(os, configName, config);
            } catch (XMLStreamException | IOException e) {
               throw new CacheException(String.format("Unable to create backup file '%s'", fileName), e);
            }
         }
      }, "cache-config-write");
   }

   @Override
   public CompletionStage<Void> restore(Properties properties, ZipFile zip) {
      return blockingManager.runBlocking(() -> {
         Set<String> configNames = resourcesToRestore(properties);
         for (String configName : configNames) {
            String configFile = configFile(configName);
            String zipPath = root.resolve(configFile).toString();
            try (InputStream is = zip.getInputStream(zip.getEntry(zipPath))) {
               ConfigurationBuilderHolder builderHolder = parserRegistry.parse(is, null);
               ConfigurationBuilder builder = builderHolder.getNamedConfigurationBuilders().get(configName);
               Configuration cfg = builder.template(true).build();

               // Only define configurations that don't already exist so that we don't overwrite newer versions of the default
               // templates e.g. org.infinispan.DIST_SYNC when upgrading a cluster
               if (cm.getCacheConfiguration(configName) == null)
                  cm.defineConfiguration(configName, cfg);
            } catch (IOException e) {
               throw new CacheException(e);
            }
         }
      }, "cache-config-read");
   }

   private String configFile(String config) {
      return String.format("%s.xml", config);
   }
}
