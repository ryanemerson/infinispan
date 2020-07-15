package org.infinispan.server.core.backup;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.server.core.BackupManager;

/**
 * @author Ryan Emerson
 * @since 11.0
 */
class BackupUtil {
   static final String BACKUP_WORKING_DIR = "backup-manager";
   static final String CONTAINER_KEY = "containers";
   static final String CONTAINERS_PROPERTIES_FILE = "container.properties";
   static final String COUNTERS_FILE = "counters.dat";
   static final String GLOBAL_CONFIG_FILE = "global.xml";
   static final String MANIFEST_PROPERTIES_FILE = "manifest.properties";
   static final String PROTO_CACHE_NAME = "___protobuf_metadata";
   static final String SCRIPT_CACHE_NAME = "___script_cache";
   static final String STAGING_ZIP = "staging.zip";
   static final String VERSION_KEY = "version";

   // TODO remove? Leave in AbstractContainerResource
   static Path resolve(Path root, BackupManager.ResourceType resource, String... subPaths) {
      Path path = root.resolve(resource.toString());
      for (String p : subPaths)
         path = path.resolve(p);
      return path;
   }

   static Set<String> asSet(Properties properties, BackupManager.ResourceType resource) {
      String prop = properties.getProperty(resource.toString());
      if (prop == null || prop.isEmpty())
         return Collections.emptySet();
      return new HashSet<>(Arrays.asList(prop.split(",")));
   }

   static void writeMessageStream(Object o, ImmutableSerializationContext serCtx, OutputStream output) throws IOException {
      // It's necessary to first write the length of each message stream as the Protocol Buffer wire format is not self-delimiting
      // https://developers.google.com/protocol-buffers/docs/techniques#streaming
      byte[] b = ProtobufUtil.toByteArray(serCtx, o);
      output.write(b.length);
      output.write(b);
   }

   static <T> T readMessageStream(ImmutableSerializationContext ctx, Class<T> clazz, InputStream is) throws IOException {
      int length = is.read();
      byte[] b = new byte[length];
      is.read(b);
      return ProtobufUtil.fromByteArray(ctx, b, clazz);
   }

   @FunctionalInterface
   interface IOConsumer<T> {
      void accept(T t) throws IOException;
   }
}
