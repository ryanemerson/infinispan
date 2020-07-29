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
 * @since 12.0
 */
@Scope(Scopes.GLOBAL)
public interface BackupManager {

   /**
    * An enum representing the current state of a Backup operation.
    */
   enum Status {
      COMPLETE,
      FAILED,
      IN_PROGRESS,
      NOT_FOUND
   }

   /**
    * Performs initialisation of all resources required by the implementation before backup files can be created or
    * restored.
    */
   void init();

   /**
    * Return the current {@link Status} of a Backup request.
    *
    * @param name the name of the backup.
    * @return the {@link Status} of the backup.
    */
   Status getBackupStatus(String name);

   /**
    * Returns the {@link Path} of a backup file if it is complete.
    *
    * @param name the name of the backup.
    * @return the {@link Path} of the created backup file if {@link Status#COMPLETE}, otherwise null.
    */
   Path getBackupLocation(String name);

   /**
    * Remove the created backup file from the server. When it's possible to remove a backup file immediately, then a
    * {@link Status#COMPLETE} is returned. However, if a backup operation is currently in progress, then the removal is
    * attempted once the backup has completed and {@link Status#IN_PROGRESS} is returned. Finally, {@link
    * Status#NOT_FOUND} is returned if no backup exists with the specified name.
    *
    * @param name the name of the backup.
    * @return a {@link CompletionStage} that returns a {@link Status} when complete to indicate what course of action
    * was taken.
    */
   CompletionStage<Status> removeBackup(String name);

   /**
    * Create a backup of all containers configured on the server, including all available resources.
    *
    * @return a {@link CompletionStage} that on completion returns the {@link Path} to the backup file that will be
    * created.
    */
   CompletionStage<Path> create(String name);

   /**
    * Create a backup of the specified containers, including the resources defined in the provided {@link Resources}
    * object.
    *
    * @param params a map of container names and an associated {@link Resources} instance.
    * @return a {@link CompletionStage} that on completion returns the {@link Path} to the backup file that will be
    * created.
    */
   CompletionStage<Path> create(String name, Map<String, Resources> params);

   /**
    * Restore all content from the provided backup bytes.
    *
    * @param is a {@link InputStream} containing the bytes of the uploaded backup file.
    * @return a {@link CompletionStage} that completes when all of the entries in the backup have been restored.
    */
   CompletionStage<Void> restore(InputStream is);

   /**
    * Restore content from the provided backup bytes. The keyset of the provided {@link Map} determines which containers
    * are restored from the backup file. Similarly, the {@link Resources} object determines which {@link
    * Resources.Type}s are restored.
    *
    * @param is a {@link InputStream} containing the bytes of the uploaded backup file.
    * @return a {@link CompletionStage} that completes when all of the entries in the backup have been restored.
    */
   CompletionStage<Void> restore(InputStream is, Map<String, Resources> params);

   /**
    * An interface to encapsulate the various arguments required by the {@link BackupManager} in order to
    * include/exclude resources from a backup/restore operation.
    */
   interface Resources {

      enum Type {
         CACHES("caches"),
         CACHE_CONFIGURATIONS("cache-configs"),
         COUNTERS("counters"),
         PROTO_SCHEMAS("proto-schemas"),
         SCRIPTS("scripts");

         final String name;

         Type(String name) {
            this.name = name;
         }

         @Override
         public String toString() {
            return this.name;
         }

         public static Type fromString(String s) {
            for (Type t : Type.values()) {
               if (t.name.equalsIgnoreCase(s)) {
                  return t;
               }
            }
            throw new IllegalArgumentException(String.format("Type with name '%s' does not exist", s));
         }
      }

      /**
       * @return the {@link Type} to be included in the backup/restore.
       */
      Set<Type> includeTypes();

      /**
       * @param type the {@link Type} to retrieve the associated resources for.
       * @return a {@link Set} of resource names to process.
       */
      Set<String> getQualifiedResources(Type type);
   }
}
