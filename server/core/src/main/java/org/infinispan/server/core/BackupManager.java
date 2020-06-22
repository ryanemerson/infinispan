package org.infinispan.server.core;

import java.nio.file.Path;
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
    * Create a backup of all containers configured on the server.
    *
    * @return a {@link CompletionStage} that on completion returns the {@link Path} to the created backup file.
    */
   CompletionStage<Path> create();

   /**
    * Restore container content from the provided backup bytes.
    *
    * @param backup the bytes of the uploaded backup file.
    * @return a {@link CompletionStage} that completes when all of the entries in the backup have been restored.
    */
   CompletionStage<Void> restore(byte[] backup);
}
