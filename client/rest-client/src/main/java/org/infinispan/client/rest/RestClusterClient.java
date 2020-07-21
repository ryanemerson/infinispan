package org.infinispan.client.rest;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 10.0
 **/
public interface RestClusterClient {
   /**
    * Shuts down the cluster
    */
   CompletionStage<RestResponse> stop();

   /**
    * Shuts down the specified servers
    */
   CompletionStage<RestResponse> stop(List<String> server);

   /**
    * Creates a backup file containing all of the current container content (caches, counters etc).
    */
   CompletionStage<RestResponse> backup();

   /**
    * Restores all content from a backup file, by uploading the file to the server endpoint for processing, returning
    * once the restoration has completed.
    *
    * @param backup the backup {@link File} containing the data to be restored.
    */
   CompletionStage<RestResponse> restore(File backup);
}
