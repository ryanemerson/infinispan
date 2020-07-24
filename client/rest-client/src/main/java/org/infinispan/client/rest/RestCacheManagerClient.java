package org.infinispan.client.rest;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import org.infinispan.commons.dataconversion.MediaType;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 10.0
 **/
public interface RestCacheManagerClient {
   String name();

   default CompletionStage<RestResponse> globalConfiguration() {
      return globalConfiguration(MediaType.APPLICATION_JSON_TYPE);
   }

   CompletionStage<RestResponse> globalConfiguration(String mediaType);

   CompletionStage<RestResponse> cacheConfigurations();

   CompletionStage<RestResponse> cacheConfigurations(String mediaType);

   CompletionStage<RestResponse> info();

   CompletionStage<RestResponse> health(boolean skipBody);

   default CompletionStage<RestResponse> health() {
      return health(false);
   }

   CompletionStage<RestResponse> templates(String mediaType);

   CompletionStage<RestResponse> healthStatus();

   CompletionStage<RestResponse> stats();

   CompletionStage<RestResponse> backupStatuses();

   CompletionStage<RestResponse> bringBackupOnline(String backup);

   CompletionStage<RestResponse> takeOffline(String backup);

   CompletionStage<RestResponse> pushSiteState(String backup);

   CompletionStage<RestResponse> cancelPushState(String backup);

   CompletionStage<RestResponse> caches();

   /**
    * Creates a backup file containing all resources in this container.
    */
   CompletionStage<RestResponse> createBackup(String name);

   /**
    * Creates a backup file containing only the resources specified in the provided {@link Map}.
    *
    * @param resources a map of BackupManager.Resources.Type with the names of the resources to backup. If the provided
    *                  list only contains "*" then all available resources of that type are backed up.
    */
   CompletionStage<RestResponse> createBackup(String name, Map<String, List<String>> resources);

   // TODO add docs
   CompletionStage<RestResponse> getBackup(String name);

   CompletionStage<RestResponse> deleteBackup(String name);

   /**
    * Restores all content associated with this containers name contained within the provided backup file. The backup
    * file is uploaded via the server endpoint for processing, returning once the restoration has completed.
    *
    * @param backup the backup {@link File} containing the data to be restored.
    */
   CompletionStage<RestResponse> restore(File backup);

   /**
    * Restores the specified content from the backup file that's associated with this container's name.
    *
    * @param backup the backup {@link File} containing the data to be restored.
    * @param resources a map of BackupManager.Resources.Type with the names of the resources to backup. If the provided
    *                  list only contains "*" then all available resources of that type are restored.
    */
   CompletionStage<RestResponse> restore(File backup, Map<String, List<String>> resources);
}
