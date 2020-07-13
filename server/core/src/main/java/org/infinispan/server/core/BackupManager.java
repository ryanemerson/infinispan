package org.infinispan.server.core;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

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
    * Create a backup of all containers configured on the server.
    *
    * @return a {@link CompletionStage} that on completion returns the {@link Path} to the created backup file.
    */
   CompletionStage<Path> create();

   /**
    * Create a backup of the specified containers and their resources defined in the mapped {@link BackupParameters}.
    *
    * @param params a map of container names and an associated {@link BackupParameters} instance.
    * @return a {@link CompletionStage} that on completion returns the {@link Path} to the created backup file.
    */
   CompletionStage<Path> create(Map<String, BackupParameters> params);

   /**
    * Restore container content from the provided backup bytes.
    *
    * @param backup the bytes of the uploaded backup file.
    * @return a {@link CompletionStage} that completes when all of the entries in the backup have been restored.
    */
   CompletionStage<Void> restore(byte[] backup);

   /**
    * Restore container content from the provided backup bytes.
    *
    * @param backup the bytes of the uploaded backup file.
    * @return a {@link CompletionStage} that completes when all of the entries in the backup have been restored.
    */
   CompletionStage<Void> restore(byte[] backup, Map<String, BackupParameters> params);

   enum Resource {
      CACHES("caches"),
      CACHE_CONFIGURATIONS("cache-configs"),
      COUNTERS("counters"),
      PROTO_SCHEMAS("proto-schemas"),
      SCRIPTS("scripts");

      final String name;
      Resource(String name) {
         this.name = name;
      }

      @Override
      public String toString() {
         return this.name;
      }
   }

   interface BackupParameters {

      /**
       * @return the name of the backup file to be created/restored.
       */
      String name();


      /**
       * @return The associated resource names to be processed. An empty {@link Set} indicates that all available
       * resources of that type should be included in a backup or restore operation. If no value is associated with a
       * given resource, then this indicates that the resource should be excluded from the backup/restore operation.
       */
      Set<String> get(Resource resource);

      Set<String> computeIfEmpty(Resource resource, Supplier<Set<String>> supplier);

      /**
       * @param resource the {@link Resource} to be queried
       * @return true if the resource is required by the backup/restore operation
       */
      default boolean include(Resource resource) {
         return get(resource) != null;
      }
   }
}
