package org.infinispan.commons.marshall;

/**
 * TypeIds used by protostream in place of FQN message/enum names to reduce payload size.
 * <p>
 * ONCE SET VALUES IN THIS CLASS MUST NOT BE CHANGED AS IT WILL BREAK BACKWARDS COMPATIBILITY.
 * <p>
 * Values must in the range 0..65535, as this is marked for internal infinispan use by the protostream project.
 * <p>
 * If message/enum types are no longer required, the variable should be commented instead of deleted.
 *
 * @author Ryan Emerson
 * @since 10.0
 */
public interface ProtoStreamTypeIds {

   int MIN_ID = 0;

   // Commons range 1 -> 1000
   int COMMONS_START = MIN_ID;
   int MEDIA_TYPE = COMMONS_START + 1;
   int WRAPPED_BYTE_ARRAY = COMMONS_START + 2;

   // Core range 1001 -> 2000
   int CORE_START = COMMONS_START + 1000;
   int BYTE_STRING = CORE_START + 1;
   int EMBEDDED_METADATA = CORE_START + 2;
   int EMBEDDED_EXPIRABLE_METADATA = CORE_START + 3;
   int EMBEDDED_LIFESPAN_METADATA = CORE_START + 4;
   int EMBEDDED_MAX_IDLE_METADATA = CORE_START + 5;
   int EVENT_LOG_CATEGORY = CORE_START + 6;
   int EVENT_LOG_LEVEL = CORE_START + 7;
   int MARSHALLED_VALUE_IMPL = CORE_START + 8;
   int META_PARAMS_INTERNAL_METADATA = CORE_START + 9;
   int NUMERIC_VERSION = CORE_START + 10;
   int PM_USER_BYTES = CORE_START + 11;
   int SIMPLE_CLUSTERED_VERSION = CORE_START + 12;

   // Counter range 2001 -> 2200
   int COUNTERS_START = CORE_START + 1000;
   int COUNTER_STATE = COUNTERS_START + 1;
   int COUNTER_VALUE = COUNTERS_START + 2;
   int STRONG_COUNTER_KEY = COUNTERS_START + 3;
   int WEAK_COUNTER_KEY = COUNTERS_START + 4;

   // Query range 2201 -> 2400
   int QUERY_START = COUNTERS_START + 200;
   int KNOWN_CLASS_KEY = QUERY_START + 1;

   // Remote Query range 2401 -> 2600
   int REMOTE_QUERY_START = QUERY_START + 200;
   int PROTOBUF_VALUE_WRAPPER = REMOTE_QUERY_START + 1;

   // Lucene Directory 2601 -> 2800
   int LUCENE_START = REMOTE_QUERY_START + 200;
   int CHUNK_CACHE_KEY = LUCENE_START + 1;
   int FILE_CACHE_KEY = LUCENE_START + 2;
   int FILE_LIST_CACHE_KEY = LUCENE_START + 3;
   int FILE_METADATA = LUCENE_START + 4;
   int FILE_READ_LOCK_KEY = LUCENE_START + 5;
   int FILE_LIST_CACHE_VALUE = LUCENE_START + 6;

   // Scripting 2801 -> 3000
   int SCRIPTING_START = LUCENE_START + 200;
   int EXECUTION_MODE = SCRIPTING_START + 1;
   int SCRIPT_METADATA = SCRIPTING_START + 2;

   // Memcached 3001 -> 3100
   int MEMCACHED_START = SCRIPTING_START + 200;
   int MEMCACHED_METADATA = MEMCACHED_START + 1;

   // RocksDB 3101 -> 3200
   int ROCKSDB_START = MEMCACHED_START + 100;
   int ROCKSDB_EXPIRY_BUCKET = ROCKSDB_START + 1;

   // Event-logger 3201 -> 3300
   int EVENT_LOGGER_START = ROCKSDB_START + 100;
   int SERVER_EVENT_IMPL = EVENT_LOGGER_START + 1;
}
