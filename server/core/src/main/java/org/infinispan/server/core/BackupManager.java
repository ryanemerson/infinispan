package org.infinispan.server.core;

import java.nio.file.Path;
import java.util.concurrent.CompletionStage;

/**
 * // TODO: Document this
 *
 * @author Ryan Emerson
 * @since 11.0
 */
public interface BackupManager {
   /**
    * Create a backup of all containers configured on the server.
    *
    * @return a {@link CompletionStage} that on completion returns the {@link Path} to the created backup file.
    */
   CompletionStage<Path> create();

   /**
    * Create a backup of a specific container.
    *
    * @param containerName the name of the container to backup.
    * @return a {@link CompletionStage} that on completion returns the {@link Path} to the created backup file.
    */
   CompletionStage<Path> create(String containerName);

   /**
    * Create a backup of a specific cache.
    *
    * @param containerName the name of the container to which the cache belongs.
    * @param cacheName     the name of the cache to backup.
    * @return a {@link CompletionStage} that on completion returns the {@link Path} to the created backup file.
    */
   CompletionStage<Path> create(String containerName, String cacheName);

   CompletionStage<Void> restore(byte[] backup);

   CompletionStage<Void> restore(byte[] backup, String cacheName);
}
