package org.infinispan.server.core.backup;

import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.zip.ZipFile;

/**
 * // TODO: Document this
 *
 * @author Ryan Emerson
 * @since 11.0
 */
public interface ContainerResource {

   void writeToManifest(Properties properties);

   Set<String> resourcesToRestore(Properties properties);

   CompletionStage<Void> backup();

   CompletionStage<Void> restore(Properties properties, ZipFile zip);
}
