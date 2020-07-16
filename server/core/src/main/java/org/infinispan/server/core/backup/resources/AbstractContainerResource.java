package org.infinispan.server.core.backup.resources;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.infinispan.commons.logging.LogFactory;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.server.core.BackupManager;
import org.infinispan.server.core.backup.ContainerResource;
import org.infinispan.server.core.logging.Log;
import org.infinispan.util.concurrent.BlockingManager;

/**
 * // TODO: Document this
 *
 * @author Ryan Emerson
 * @since 11.0
 */
abstract class AbstractContainerResource implements ContainerResource {

   protected static final Log log = LogFactory.getLog(MethodHandles.lookup().lookupClass(), Log.class);

   protected final BackupManager.ResourceType type;
   protected final BackupManager.Parameters params;
   protected final Path root;
   protected final BlockingManager blockingManager;
   protected final EmbeddedCacheManager cm;
   protected final boolean wildcard;
   protected final Set<String> qualifiedResources;

   // TODO make order consistent with implementations
   protected AbstractContainerResource(BackupManager.ResourceType type, BackupManager.Parameters params, Path root,
                                       BlockingManager blockingManager, EmbeddedCacheManager cm) {
      this.type = type;
      this.params = params;
      this.root = root.resolve(type.toString());
      this.blockingManager = blockingManager;
      this.cm = cm;
      Set<String> qualifiedResources = params.getQualifiedResources(type);
      this.wildcard = qualifiedResources == null;
      this.qualifiedResources = ConcurrentHashMap.newKeySet();
      if (!wildcard)
         this.qualifiedResources.addAll(qualifiedResources);
   }

   @Override
   public void writeToManifest(Properties properties) {
      properties.put(type.toString(), String.join(",", qualifiedResources));
   }

   @Override
   public Set<String> resourcesToRestore(Properties properties) {
      // Only process specific resources if specified
      Set<String> resourcesToProcess = asSet(properties, type);

      if (!wildcard) {
         resourcesToProcess.retainAll(qualifiedResources);

         if (resourcesToProcess.isEmpty()) {
            Set<String> missingResources = new HashSet<>(qualifiedResources);
            missingResources.removeAll(resourcesToProcess);
            throw log.unableToFindBackupResource(type.toString(), missingResources);
         }
      }
      return resourcesToProcess;
   }

   static Set<String> asSet(Properties properties, BackupManager.ResourceType resource) {
      String prop = properties.getProperty(resource.toString());
      if (prop == null || prop.isEmpty())
         return Collections.emptySet();
      return new HashSet<>(Arrays.asList(prop.split(",")));
   }

   protected static void writeMessageStream(Object o, ImmutableSerializationContext serCtx, OutputStream output) throws IOException {
      // It's necessary to first write the length of each message stream as the Protocol Buffer wire format is not self-delimiting
      // https://developers.google.com/protocol-buffers/docs/techniques#streaming
      byte[] b = ProtobufUtil.toByteArray(serCtx, o);
      output.write(b.length);
      output.write(b);
   }

   protected static <T> T readMessageStream(ImmutableSerializationContext ctx, Class<T> clazz, InputStream is) throws IOException {
      int length = is.read();
      byte[] b = new byte[length];
      is.read(b);
      return ProtobufUtil.fromByteArray(ctx, b, clazz);
   }
}
