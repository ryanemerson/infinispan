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
    * Create a backup of all containers configured on the server, including all available resources.
    *
    * @return a {@link CompletionStage} that on completion returns the {@link Path} to the created backup file.
    */
   CompletionStage<Path> create();

   /**
    * Create a backup of the specified containers, including the resources defined in the provided {@link Parameters}
    * object.
    *
    * @param params a map of container names and an associated {@link Parameters} instance.
    * @return a {@link CompletionStage} that on completion returns the {@link Path} to the created backup file.
    */
   CompletionStage<Path> create(Map<String, Parameters> params);

   /**
    * Restore all content from the provided backup bytes.
    *
    * @param backup the bytes of the uploaded backup file.
    * @return a {@link CompletionStage} that completes when all of the entries in the backup have been restored.
    */
   CompletionStage<Void> restore(byte[] backup);

   /**
    * Restore content from the provided backup bytes. The keyset of the provided {@link Map} determines which containers
    * are restored from the backup file. Similarly, the {@link Parameters} object determines which {@link Resource}s are
    * restored.
    *
    * @param backup the bytes of the uploaded backup file.
    * @return a {@link CompletionStage} that completes when all of the entries in the backup have been restored.
    */
   CompletionStage<Void> restore(byte[] backup, Map<String, Parameters> params);

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

   /**
    * An interface to encapsulate the various arguments required by the {@link BackupManager} in order to include/exclude
    * resources from a backup/restore operation.
    */
   interface Parameters {

      /**
       * @return the name of the backup file to be created/restored.
       */
      String name();

      /**
       * @return The names of the {@link Resource} to be restored. An empty {@link Set} indicates that all available
       * resources of that type should be included in a backup or restore operation. Conversely, if a null value is
       * returned for a resource, this indicates that the resource should be excluded from the backup/restore operation.
       */
      Set<String> get(Resource resource);

      /**
       * Replaces an empty {@link Set} associated with a {@link Resource}, with that provided by the {@link Supplier}.
       * This is useful for replacing the wildcard, i.e. an empty set, with the actual resource names available.
       *
       * @param resource
       * @param supplier
       * @return
       */
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
