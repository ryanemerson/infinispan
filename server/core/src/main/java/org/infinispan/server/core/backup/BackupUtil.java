package org.infinispan.server.core.backup;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.ProtobufUtil;

/**
 * @author Ryan Emerson
 * @since 11.0
 */
class BackupUtil {
   static final String BACKUP_WORKING_DIR = "backup-manager";
   static final String CACHE_CONFIG_DIR = "cache-configs";
   static final String CACHES_CONFIG_PROPERTY = "cache-configs";
   static final String CACHES_DIR = "caches";
   static final String CACHES_PROPERTY = "caches";
   static final String CONTAINER_DIR = "containers";
   static final String CONTAINERS_PROPERTIES_FILE = "container.properties";
   static final String CONTAINERS_PROPERTY = "containers";
   static final String COUNTERS_DIR = "counters";
   static final String COUNTERS_FILE = "counters.dat";
   static final String COUNTERS_PROPERTY = "counters";
   static final String GLOBAL_CONFIG_FILE = "global.xml";
   static final String MANIFEST_PROPERTIES_FILE = "manifest.properties";
   static final String PROTO_CACHE_NAME = "___protobuf_metadata";
   static final String PROTO_SCHEMA_DIR = "proto-schemas";
   static final String PROTO_SCHEMA_PROPERTY = "proto-schemas";
   static final String RESTORE_LOCAL_ZIP = "restore.zip";
   static final String SCRIPT_CACHE_NAME = "___script_cache";
   static final String SCRIPT_DIR = "scripts";
   static final String SCRIPT_PROPERTY = "scripts";
   static final String VERSION = "version";

   static String cacheDataFile(String cache) {
      return String.format("%s.dat", cache);
   }

   static String cacheConfigFile(String cache) {
      return String.format("%s.xml", cache);
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
}
