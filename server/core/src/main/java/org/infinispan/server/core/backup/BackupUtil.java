package org.infinispan.server.core.backup;

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
   static final String GLOBAL_CONFIG_FILE = "global.xml";
   static final String PROTO_CACHE_NAME = "___protobuf_metadata";
   static final String PROTO_SCHEMA_PROPERTY = "proto-schemas";
   static final String MANIFEST_PROPERTIES_FILE = "manifest.properties";
   static final String RESTORE_LOCAL_ZIP = "restore.zip";
   static final String VERSION = "version";

   static String cacheDataFile(String cache) {
      return String.format("%s.dat", cache);
   }

   static String cacheConfigFile(String cache) {
      return String.format("%s.xml", cache);
   }
}
