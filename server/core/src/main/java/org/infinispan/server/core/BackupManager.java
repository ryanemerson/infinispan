package org.infinispan.server.core;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import org.infinispan.factories.scopes.Scope;
import org.infinispan.factories.scopes.Scopes;

/**
 * Handles all tasks related to the creation/restoration of server backups.
 *
 * @author Ryan Emerson
 * @since 11.0
 */
@Scope(Scopes.GLOBAL)
public interface BackupManager {

   /**
    * Performs initialisation of all resources required by the implementation before backup files can be created or restored.
    */
   void init();

   /**
    * Create a backup of all containers configured on the server, including all available resources.
    *
    * @return a {@link CompletionStage} that on completion returns the {@link Path} to the created backup file.
    */
   CompletionStage<Path> create();

   /**
    * Create a backup of the specified containers, including the resources defined in the provided {@link ContainerResources}
    * object.
    *
    * @param params a map of container names and an associated {@link ContainerResources} instance.
    * @return a {@link CompletionStage} that on completion returns the {@link Path} to the created backup file.
    */
   CompletionStage<Path> create(Map<String, ContainerResources> params);

   /**
    * Restore all content from the provided backup bytes.
    *
    * @param is a {@link InputStream} containing the bytes of the uploaded backup file.
    * @return a {@link CompletionStage} that completes when all of the entries in the backup have been restored.
    */
   CompletionStage<Void> restore(InputStream is);

   /**
    * Restore content from the provided backup bytes. The keyset of the provided {@link Map} determines which containers
    * are restored from the backup file. Similarly, the {@link ContainerResources} object determines which {@link ResourceType}s are
    * restored.
    *
    * @param is a {@link InputStream} containing the bytes of the uploaded backup file.
    * @return a {@link CompletionStage} that completes when all of the entries in the backup have been restored.
    */
   CompletionStage<Void> restore(InputStream is, Map<String, ContainerResources> params);

   enum ResourceType {
      CACHES("caches"),
      CACHE_CONFIGURATIONS("cache-configs"),
      COUNTERS("counters"),
      PROTO_SCHEMAS("proto-schemas"),
      SCRIPTS("scripts");

      final String name;
      ResourceType(String name) {
         this.name = name;
      }

      @Override
      public String toString() {
         return this.name;
      }
   }

   /**
    * An interface to encapsulate the various arguments required by the {@link BackupManager} in order to include/exclude
    * resources from a backup/restore operation.
    */
   interface ContainerResources {

      /**
       * @return the {@link ResourceType} to be included in the backup/restore.
       */
      Set<ResourceType> includeTypes();

      /**
       * @param type the {@link ResourceType} to retrieve the associated resources for.
       * @return a {@link Set} of resource names to process.
       */
      Set<String> getQualifiedResources(ResourceType type);
   }
}
