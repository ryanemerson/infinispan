package org.infinispan.commons.marshall;

import org.infinispan.protostream.WrappedMessage;

/**
 * TypeIds used by protostream in place of FQN message/enum names to reduce payload size.
 * <p>
 * ONCE SET VALUES IN THIS CLASS MUST NOT BE CHANGED AS IT WILL BREAK BACKWARDS COMPATIBILITY.
 * <p>
 * Values must in the range 0..65535, as this is marked for internal infinispan use by the protostream project.
 * <p>
 * TypeIds are written as a variable length uint32, so Ids in the range 0..127 should be prioritised for frequently
 * marshalled classes.
 * <p>
 * Message names should not end in _LOWER_BOUND as this is used by ProtoStreamTypeIdsUniquenessTest.
 * <p>
 * If message/enum types are no longer required, the variable should be commented instead of deleted.
 *
 * @author Ryan Emerson
 * @since 10.0
 */
// TODO change previous ID + 1 to LOWER_BOUND + x
public interface ProtoStreamTypeIds {

   // 1 byte Ids 0..127 -> Reserved for critical messages used a lot
   int WRAPPED_MESSAGE = WrappedMessage.PROTOBUF_TYPE_ID; // Id 0 is reserved for ProtoStream WrappedMessage class
   int WRAPPED_BYTE_ARRAY = 1;
   int MARSHALLABLE_USER_OBJECT = 2;
   int BYTE_STRING = 3;
   int EMBEDDED_METADATA = 4;
   int EMBEDDED_EXPIRABLE_METADATA = 5;
   int EMBEDDED_LIFESPAN_METADATA = 6;
   int EMBEDDED_MAX_IDLE_METADATA = 7;
   int NUMERIC_VERSION = 8;
   int SIMPLE_CLUSTERED_VERSION = 9;
   int JGROUPS_ADDRESS = 10;
   int PROTOBUF_VALUE_WRAPPER = 11;
   int MEDIA_TYPE = 12;
   int PRIVATE_METADATA = 13;
   int SUBJECT = 14;

   int FLAG = SUBJECT + 1;
   int JGROUPS_TOPOLOGY_AWARE_ADDRESS = FLAG + 1;
   int MARSHALLABLE_ARRAY = JGROUPS_TOPOLOGY_AWARE_ADDRESS + 1;
   int MARSHALLABLE_COLLECTION = MARSHALLABLE_ARRAY + 1;
   int MARSHALLABLE_LAMBDA = MARSHALLABLE_COLLECTION + 1;
   int MARSHALLABLE_MAP = MARSHALLABLE_LAMBDA + 1;
   int MARSHALLABLE_OBJECT = MARSHALLABLE_MAP + 1;
   int MARSHALLABLE_THROWABLE = MARSHALLABLE_OBJECT + 1;
   int COMMAND_INVOCATION_ID = MARSHALLABLE_THROWABLE + 1;
   int INVALIDATE_COMMAND = COMMAND_INVOCATION_ID + 1;
   int PUT_KEY_VALUE_COMMAND = INVALIDATE_COMMAND + 1;
   int REMOVE_COMMAND = PUT_KEY_VALUE_COMMAND + 1;
   int REMOVE_EXPIRED_COMMAND = REMOVE_COMMAND + 1;
   int REPLACE_COMMAND = REMOVE_EXPIRED_COMMAND + 1;
   int SINGLE_RPC_COMMAND = REPLACE_COMMAND + 1;
   int SUCCESSFUL_RESPONSE = SINGLE_RPC_COMMAND + 1;
   int PREPARE_RESPONSE = SUCCESSFUL_RESPONSE + 1;
   int VALUE_MATCHER = PREPARE_RESPONSE + 1;
   // TODO should only successful response be in the single byte range?
   // As exceptional responses are exceptions, it should be Ok to have larger payloads
   int UNSUCCESSFUL_RESPONSE = VALUE_MATCHER + 1;
   int EXCEPTION_RESPONSE = UNSUCCESSFUL_RESPONSE + 1;
   int UNSURE_RESPONSE = EXCEPTION_RESPONSE + 1;
   int INVALID_RESPONSE = UNSURE_RESPONSE + 1;
   int BIAS_REVOCATION_RESPONSE = INVALID_RESPONSE + 1;
   int CACHE_NOT_FOUND_RESPONSE = BIAS_REVOCATION_RESPONSE + 1;

   // Priority counter values
   int COUNTER_VALUE = 125;
   int STRONG_COUNTER_KEY = 126;
   int WEAK_COUNTER_KEY = 127;

   // 2 byte Ids 128..16383
   // Commons range 128 -> 999
   int COMMONS_LOWER_BOUND = 128;
   int NULL_VALUE = COMMONS_LOWER_BOUND;
   int XID_IMPL = COMMONS_LOWER_BOUND + 1;
   int ATOMIC_INTEGER_ARRAY = XID_IMPL + 1;
   int HASH_CRC16 = ATOMIC_INTEGER_ARRAY + 1;
   int INTSET_CONCURRENT_SMALL = HASH_CRC16 + 1;
   int INTSET_EMPTY = INTSET_CONCURRENT_SMALL + 1;
   int INTSET_RANGE = INTSET_EMPTY + 1;
   int INTSET_SINGLETON = INTSET_RANGE + 1;
   int INTSET_SMALL = INTSET_SINGLETON + 1;

   // Core range 1000 -> 3999
   int CORE_LOWER_BOUND = 1000;
   int EVENT_LOG_CATEGORY = CORE_LOWER_BOUND;
   int EVENT_LOG_LEVEL = CORE_LOWER_BOUND + 1;
   int MARSHALLED_VALUE_IMPL = CORE_LOWER_BOUND + 2;
   int META_PARAMS_INTERNAL_METADATA = CORE_LOWER_BOUND + 3;
   int REMOTE_METADATA = CORE_LOWER_BOUND + 4;
   int UUID = CORE_LOWER_BOUND + 5;
   int IRAC_VERSION = CORE_LOWER_BOUND + 6;
   int IRAC_SITE_VERSION = CORE_LOWER_BOUND + 7;
   int IRAC_VERSION_ENTRY = CORE_LOWER_BOUND + 8;
   int IRAC_METADATA = CORE_LOWER_BOUND + 9;
   int ROLE_SET = CORE_LOWER_BOUND + 10;
   int ROLE = CORE_LOWER_BOUND + 11;
   int AUTHORIZATION_PERMISSION = CORE_LOWER_BOUND + 12;
   int BITSET = CORE_LOWER_BOUND + 13;

   int ADAPTER_CLASS = BITSET + 1;
   int ADAPTER_COLLECTIONS_EMPTY_LIST = ADAPTER_CLASS + 1;
   int ADAPTER_COLLECTIONS_EMPTY_SET = ADAPTER_COLLECTIONS_EMPTY_LIST + 1;
   int ADAPTER_COLLECTIONS_LIST_OF = ADAPTER_COLLECTIONS_EMPTY_SET + 1;
   int ADAPTER_COLLECTIONS_SINGLETON_LIST = ADAPTER_COLLECTIONS_LIST_OF + 1;
   int ADAPTER_DOUBLE_SUMMARY_STATISTICS = ADAPTER_COLLECTIONS_SINGLETON_LIST + 1;
   int ADAPTER_INT_SUMMARY_STATISTICS = ADAPTER_DOUBLE_SUMMARY_STATISTICS + 1;
   int ADAPTER_LONG_SUMMARY_STATISTICS = ADAPTER_INT_SUMMARY_STATISTICS + 1;
   int ADAPTER_OPTIONAL = ADAPTER_LONG_SUMMARY_STATISTICS + 1;

   int ACCEPT_ALL_KEY_VALUE_FILTER = ADAPTER_OPTIONAL + 1;
   int AVAILABILITY_MODE = ACCEPT_ALL_KEY_VALUE_FILTER + 1;
   int BACKUP_ACK_COMMAND = AVAILABILITY_MODE + 1;
   int BACKUP_MULTI_KEY_ACK_COMMAND = BACKUP_ACK_COMMAND + 1;
   int BACKUP_NOOP_COMMAND = BACKUP_MULTI_KEY_ACK_COMMAND + 1;
   int BI_FUNCTION_MAPPER = BACKUP_NOOP_COMMAND + 1;
   int CACHE_AVAILABILITY_UPDATE_COMMAND = BI_FUNCTION_MAPPER + 1;
   int CACHE_COLLECTORS_COLLECTOR_SUPPLIER = CACHE_AVAILABILITY_UPDATE_COMMAND + 1;
   int CACHE_CONTAINER_ADMIN_FLAG = CACHE_COLLECTORS_COLLECTOR_SUPPLIER + 1;
   int CACHE_ENTRY_GROUP_PREDICATE = CACHE_CONTAINER_ADMIN_FLAG + 1;
   int CACHE_EVENT_CONVERTER_AS_CONVERTER = CACHE_ENTRY_GROUP_PREDICATE + 1;
   int CACHE_EVENT_FILTER_AS_KEY_VALUE_FILTER = CACHE_EVENT_CONVERTER_AS_CONVERTER + 1;
   int CACHE_FILTERS_CONVERTER_AS_CACHE_ENTRY_FUNCTION = CACHE_EVENT_FILTER_AS_KEY_VALUE_FILTER + 1;
   int CACHE_FILTERS_FILTER_CONVERTER_AS_CACHE_ENTRY_FUNCTION = CACHE_FILTERS_CONVERTER_AS_CACHE_ENTRY_FUNCTION + 1;
   int CACHE_FILTERS_FILTER_CONVERTER_AS_KEY_FUNCTION = CACHE_FILTERS_FILTER_CONVERTER_AS_CACHE_ENTRY_FUNCTION + 1;
   int CACHE_FILTERS_FILTER_CONVERTER_AS_VALUE_FUNCTION = CACHE_FILTERS_FILTER_CONVERTER_AS_KEY_FUNCTION + 1;
   int CACHE_FILTERS_KEY_VALUE_FILTER_AS_PREDICATE = CACHE_FILTERS_FILTER_CONVERTER_AS_VALUE_FUNCTION + 1;
   int CACHE_FILTERS_NOT_NULL_CACHE_ENTRY_PREDICATE = CACHE_FILTERS_KEY_VALUE_FILTER_AS_PREDICATE + 1;
   int CACHE_INTERMEDIATE_PUBLISHER = CACHE_FILTERS_NOT_NULL_CACHE_ENTRY_PREDICATE + 1;
   int CACHE_JOIN_INFO = CACHE_INTERMEDIATE_PUBLISHER + 1;
   int CACHE_JOIN_COMMAND = CACHE_JOIN_INFO + 1;
   int CACHE_LEAVE_COMMAND = CACHE_JOIN_COMMAND + 1;
   int CACHE_MODE = CACHE_LEAVE_COMMAND + 1;
   int CACHE_SHUTDOWN_COMMAND = CACHE_MODE + 1;
   int CACHE_SHUTDOWN_REQUEST_COMMAND = CACHE_SHUTDOWN_COMMAND + 1;
   int CACHE_STATE = CACHE_SHUTDOWN_REQUEST_COMMAND + 1;
   int CACHE_STATUS_REQUEST_COMMAND = CACHE_STATE + 1;
   int CACHE_STATUS_RESPONSE = CACHE_STATUS_REQUEST_COMMAND + 1;
   int CACHE_STREAM_INTERMEDIATE_REDUCER = CACHE_STATUS_RESPONSE + 1;
   int CACHE_TOPOLOGY = CACHE_STREAM_INTERMEDIATE_REDUCER + 1;
   int CACHE_TOPOLOGY_PHASE = CACHE_TOPOLOGY + 1;
   int CANCEL_PUBLISHER_COMMAND = CACHE_TOPOLOGY_PHASE + 1;
   int CHECK_TRANSACTION_RPC_COMMAND = CANCEL_PUBLISHER_COMMAND + 1;
   int CLEAR_COMMAND = CHECK_TRANSACTION_RPC_COMMAND + 1;
   int CLUSTER_EVENT = CLEAR_COMMAND + 1;
   int CLUSTER_EVENT_TYPE = CLUSTER_EVENT + 1;
   int CLUSTER_LISTENER_REMOVE_CALLABLE = CLUSTER_EVENT_TYPE + 1;
   int CLUSTER_LISTENER_REPLICATE_CALLABLE = CLUSTER_LISTENER_REMOVE_CALLABLE + 1;
   int CLUSTERED_GET_ALL_COMMAND = CLUSTER_LISTENER_REPLICATE_CALLABLE + 1;
   int CLUSTERED_GET_COMMAND = CLUSTERED_GET_ALL_COMMAND + 1;
   int CONFLICT_RESOLUTION_START_COMMAND = CLUSTERED_GET_COMMAND + 1;
   int COMMIT_COMMAND = CONFLICT_RESOLUTION_START_COMMAND + 1;
   int COMPLETE_TRANSACTION_COMMAND = COMMIT_COMMAND + 1;
   int COMPOSITE_KEY_VALUE_FILTER = COMPLETE_TRANSACTION_COMMAND + 1;
   int COMPUTE_COMMAND = COMPOSITE_KEY_VALUE_FILTER + 1;
   int COMPUTE_IF_ABSENT_COMMAND = COMPUTE_COMMAND + 1;
   int DATA_CONVERSION = COMPUTE_IF_ABSENT_COMMAND + 1;
   int DEFAULT_CONSISTENT_HASH = DATA_CONVERSION + 1;
   int DEFAULT_CONSISTENT_HASH_FACTORY = DEFAULT_CONSISTENT_HASH + 1;
   int DELIVERY_GUARANTEE = DEFAULT_CONSISTENT_HASH_FACTORY + 1;
   int DISTRIBUTED_CACHE_STATS_CALLABLE = DELIVERY_GUARANTEE + 1;
   int ENCODER_KEY_MAPPER = DISTRIBUTED_CACHE_STATS_CALLABLE + 1;
   int ENCODER_ENTRY_MAPPER = ENCODER_KEY_MAPPER + 1;
   int ENCODER_VALUE_MAPPER = ENCODER_ENTRY_MAPPER + 1;
   int ENTRY_VIEWS_NO_VALUE_READ_ONLY = ENCODER_VALUE_MAPPER + 1;
   int ENTRY_VIEWS_READ_ONLY_SNAPSHOT = ENTRY_VIEWS_NO_VALUE_READ_ONLY + 1;
   int ENTRY_VIEWS_READ_WRITE_SNAPSHOT = ENTRY_VIEWS_READ_ONLY_SNAPSHOT + 1;
   int EXCEPTION_ACK_COMMAND = ENTRY_VIEWS_READ_WRITE_SNAPSHOT + 1;
   int EXCEPTION_WRITE_SKEW = EXCEPTION_ACK_COMMAND + 1;
   int FUNCTION_MAPPER = EXCEPTION_WRITE_SKEW + 1;
   int FUNCTIONAL_PARAMS = FUNCTION_MAPPER + 1;
   int FUNCTIONAL_STATS_ENVELOPE = FUNCTIONAL_PARAMS + 1;
   int GET_KEYS_IN_GROUP_COMMAND = FUNCTIONAL_STATS_ENVELOPE + 1;
   int GET_KEY_VALUE_COMMAND = GET_KEYS_IN_GROUP_COMMAND + 1;
   int GET_IN_DOUBT_TRANSACTIONS_COMMAND = GET_KEY_VALUE_COMMAND + 1;
   int GET_IN_DOUBT_TX_INFO_COMMAND = GET_IN_DOUBT_TRANSACTIONS_COMMAND + 1;
   int GLOBAL_TRANSACTION = GET_IN_DOUBT_TX_INFO_COMMAND + 1;
   int HEART_BEAT_COMMAND = GLOBAL_TRANSACTION + 1;
   int IMMORTAL_CACHE_ENTRY = HEART_BEAT_COMMAND + 1;
   int IMMORTAL_CACHE_VALUE = IMMORTAL_CACHE_ENTRY + 1;
   int IN_DOUBT_TX_INFO = IMMORTAL_CACHE_VALUE + 1;
   int INITIAL_PUBLISHER_COMMAND = IN_DOUBT_TX_INFO + 1;
   int INTERNAL_METADATA_IMPL = INITIAL_PUBLISHER_COMMAND + 1;
   int INVALIDATE_L1_COMMAND = INTERNAL_METADATA_IMPL + 1;
   int INVALIDATE_VERSIONS_COMMAND = INVALIDATE_L1_COMMAND + 1;
   int IRAC_CLEANUP_KEYS_COMMAND = INVALIDATE_VERSIONS_COMMAND + 1;
   int IRAC_CLEAR_KEYS_COMMAND = IRAC_CLEANUP_KEYS_COMMAND + 1;
   int IRAC_MANAGER_KEY_INFO = IRAC_CLEAR_KEYS_COMMAND + 1;
   int IRAC_METADATA_REQUEST_COMMAND = IRAC_MANAGER_KEY_INFO + 1;
   int IRAC_PUT_KEY_VALUE_COMMAND = IRAC_METADATA_REQUEST_COMMAND + 1;
   int IRAC_PUT_MANY_REQUEST = IRAC_PUT_KEY_VALUE_COMMAND + 1;
   int IRAC_PUT_MANY_REQUEST_EXPIRE = IRAC_PUT_MANY_REQUEST + 1;
   int IRAC_PUT_MANY_REQUEST_REMOVE = IRAC_PUT_MANY_REQUEST_EXPIRE + 1;
   int IRAC_PUT_MANY_REQUEST_WRITE = IRAC_PUT_MANY_REQUEST_REMOVE + 1;
   int IRAC_REQUEST_STATE_COMMAND = IRAC_PUT_MANY_REQUEST_WRITE + 1;
   int IRAC_STATE_RESPONSE_COMMAND = IRAC_REQUEST_STATE_COMMAND + 1;
   int IRAC_STATE_RESPONSE_COMMAND_STATE = IRAC_STATE_RESPONSE_COMMAND + 1;
   int IRAC_TOMBSTONE_CHECKOUT_REQUEST = IRAC_STATE_RESPONSE_COMMAND_STATE + 1;
   int IRAC_TOMBSTONE_CLEANUP_COMMAND = IRAC_TOMBSTONE_CHECKOUT_REQUEST + 1;
   int IRAC_TOMBSTONE_INFO = IRAC_TOMBSTONE_CLEANUP_COMMAND + 1;
   int IRAC_TOMBSTONE_PRIMARY_CHECK_COMMAND = IRAC_TOMBSTONE_INFO + 1;
   int IRAC_TOMBSTONE_REMOTE_SITE_CHECK_COMMAND = IRAC_TOMBSTONE_PRIMARY_CHECK_COMMAND + 1;
   int IRAC_TOMBSTONE_STATE_RESPONSE_COMMAND = IRAC_TOMBSTONE_REMOTE_SITE_CHECK_COMMAND + 1;
   int IRAC_TOUCH_KEY_REQUEST = IRAC_TOMBSTONE_STATE_RESPONSE_COMMAND + 1;
   int IRAC_UPDATE_VERSION_COMMAND = IRAC_TOUCH_KEY_REQUEST + 1;
   int KEY_PUBLISHER_RESPONSE = IRAC_UPDATE_VERSION_COMMAND + 1;

   int KEY_VALUE_FILTER_AS_CACHE_EVENT_FILTER = KEY_PUBLISHER_RESPONSE + 1;
   int KEY_VALUE_FILTER_CONVERTER_AS_CACHE_EVENT_FILTER_CONVERTER = KEY_VALUE_FILTER_AS_CACHE_EVENT_FILTER + 1;
   int KEY_VALUE_FILTER_CONVERTER_AS_CACHE_KEY_VALUE_CONVERTER = KEY_VALUE_FILTER_CONVERTER_AS_CACHE_EVENT_FILTER_CONVERTER + 1;
   int KEY_VALUE_PAIR = KEY_VALUE_FILTER_CONVERTER_AS_CACHE_KEY_VALUE_CONVERTER + 1;
   int LOCK_CONTROL_COMMAND = KEY_VALUE_PAIR + 1;
   int MANAGER_STATUS_RESPONSE = LOCK_CONTROL_COMMAND + 1;
   int MERGE_FUNCTION = MANAGER_STATUS_RESPONSE + 1;
   int METADATA_IMMORTAL_ENTRY = MERGE_FUNCTION + 1;
   int METADATA_IMMORTAL_VALUE = METADATA_IMMORTAL_ENTRY + 1;
   int METADATA_MORTAL_ENTRY = METADATA_IMMORTAL_VALUE + 1;
   int METADATA_MORTAL_VALUE = METADATA_MORTAL_ENTRY + 1;
   int METADATA_TRANSIENT_CACHE_ENTRY = METADATA_MORTAL_VALUE + 1;
   int METADATA_TRANSIENT_CACHE_VALUE = METADATA_TRANSIENT_CACHE_ENTRY + 1;
   int METADATA_TRANSIENT_MORTAL_CACHE_ENTRY = METADATA_TRANSIENT_CACHE_VALUE + 1;
   int METADATA_TRANSIENT_MORTAL_CACHE_VALUE = METADATA_TRANSIENT_MORTAL_CACHE_ENTRY + 1;
   int META_PARAMS_ENTRY_VERSION = METADATA_TRANSIENT_MORTAL_CACHE_VALUE + 1;
   int META_PARAMS_LIFESPAN = META_PARAMS_ENTRY_VERSION + 1;
   int META_PARAMS_MAX_IDLE = META_PARAMS_LIFESPAN + 1;
   int MORTAL_CACHE_ENTRY = META_PARAMS_MAX_IDLE + 1;
   int MORTAL_CACHE_VALUE = MORTAL_CACHE_ENTRY + 1;
   int MULTI_CLUSTER_EVENT_COMMAND = MORTAL_CACHE_VALUE + 1;
   int MULTI_ENTRIES_FUNCTIONAL_BACKUP_WRITE_COMMAND = MULTI_CLUSTER_EVENT_COMMAND + 1;
   int MULTI_KEY_FUNCTIONAL_BACKUP_WRITE_COMMAND = MULTI_ENTRIES_FUNCTIONAL_BACKUP_WRITE_COMMAND + 1;
   int MUTATIONS_READ_WRITE = MULTI_KEY_FUNCTIONAL_BACKUP_WRITE_COMMAND + 1;
   int MUTATIONS_READ_WRITE_WITH_VALUE = MUTATIONS_READ_WRITE + 1;
   int MUTATIONS_WRITE = MUTATIONS_READ_WRITE_WITH_VALUE + 1;
   int MUTATIONS_WRITE_WITH_VALUE = MUTATIONS_WRITE + 1;
   int NEXT_PUBLISHER_COMMAND = MUTATIONS_WRITE_WITH_VALUE + 1;

   int PERSISTENCE_UUID = NEXT_PUBLISHER_COMMAND + 1;
   int PREPARE_COMMAND = PERSISTENCE_UUID + 1;
   int PUBLISHER_RESPONSE = PREPARE_COMMAND + 1;

   int PUBLISHER_HANDLER_SEGMENT_RESPONSE = PUBLISHER_RESPONSE + 1;
   int PUBLISHER_TRANSFORMERS_IDENTITY_TRANSFORMER = PUBLISHER_HANDLER_SEGMENT_RESPONSE + 1;
   int PUT_MAP_BACKUP_WRITE_COMMAND = PUBLISHER_TRANSFORMERS_IDENTITY_TRANSFORMER + 1;
   int PUT_MAP_COMMAND = PUT_MAP_BACKUP_WRITE_COMMAND + 1;
   int READ_ONLY_KEY_COMMAND = PUT_MAP_COMMAND + 1;
   int READ_ONLY_MANY_COMMAND = READ_ONLY_KEY_COMMAND + 1;
   int READ_WRITE_KEY_COMMAND = READ_ONLY_MANY_COMMAND + 1;
   int READ_WRITE_KEY_VALUE_COMMAND = READ_WRITE_KEY_COMMAND + 1;
   int READ_WRITE_MANY_COMMAND = READ_WRITE_KEY_VALUE_COMMAND + 1;
   int READ_WRITE_MANY_ENTRIES_COMMAND = READ_WRITE_MANY_COMMAND + 1;
   int REBALANCE_PHASE_CONFIRM_COMMAND = READ_WRITE_MANY_ENTRIES_COMMAND + 1;
   int REBALANCE_POLICY_UPDATE_COMMAND = REBALANCE_PHASE_CONFIRM_COMMAND + 1;
   int REBALANCE_STATUS = REBALANCE_POLICY_UPDATE_COMMAND + 1;
   int REBALANCE_START_COMMAND = REBALANCE_STATUS + 1;
   int REBALANCE_STATUS_REQUEST_COMMAND = REBALANCE_START_COMMAND + 1;
   int REDUCTION_PUBLISHER_REQUEST_COMMAND = REBALANCE_STATUS_REQUEST_COMMAND + 1;
   int RENEW_BIAS_COMMAND = REDUCTION_PUBLISHER_REQUEST_COMMAND + 1;
   int REPLICABLE_MANAGER_FUNCTION_COMMAND = RENEW_BIAS_COMMAND + 1;
   int REPLICATED_CONSISTENT_HASH = REPLICABLE_MANAGER_FUNCTION_COMMAND + 1;
   int REPLICATED_CONSISTENT_HASH_FACTORY = REPLICATED_CONSISTENT_HASH + 1;
   int REPLICABLE_RUNNABLE_COMMAND = REPLICATED_CONSISTENT_HASH_FACTORY + 1;
   int REVOKE_BIAS_COMMAND = REPLICABLE_RUNNABLE_COMMAND + 1;
   int ROLLBACK_COMMAND = REVOKE_BIAS_COMMAND + 1;
   int SCATTERED_CONSISTENT_HASH = ROLLBACK_COMMAND + 1;
   int SCATTERED_CONSISTENT_HASH_FACTORY = SCATTERED_CONSISTENT_HASH + 1;
   int SCATTERED_STATE_CONFIRM_REVOKE_COMMAND = SCATTERED_CONSISTENT_HASH_FACTORY + 1;
   int SCATTERED_STATE_GET_KEYS_COMMAND = SCATTERED_STATE_CONFIRM_REVOKE_COMMAND + 1;
   int SCOPE_FILTER = SCATTERED_STATE_GET_KEYS_COMMAND + 1;
   int SCOPED_STATE = SCOPE_FILTER + 1;
   int SEGMENT_OWNERSHIP = SCOPED_STATE + 1;
   int SEGMENT_PUBLISHER_RESULT = SEGMENT_OWNERSHIP + 1;
   int SINGLE_KEY_BACKUP_WRITE_COMMAND = SEGMENT_PUBLISHER_RESULT + 1;
   int SINGLE_KEY_BACKUP_WRITE_COMMAND_OPERATION = SINGLE_KEY_BACKUP_WRITE_COMMAND + 1;
   int SINGLE_KEY_FUNCTIONAL_BACKUP_WRITE_COMMAND = SINGLE_KEY_BACKUP_WRITE_COMMAND_OPERATION + 1;
   int SINGLE_KEY_FUNCTIONAL_BACKUP_WRITE_COMMAND_OPERATION = SINGLE_KEY_FUNCTIONAL_BACKUP_WRITE_COMMAND + 1;

   int SIZE_COMMAND = SINGLE_KEY_FUNCTIONAL_BACKUP_WRITE_COMMAND_OPERATION + 1;
   int STATE_CHUNK = SIZE_COMMAND + 1;
   int STATE_RESPONSE_COMMAND = STATE_CHUNK + 1;
   int STATE_TRANSFER_CANCEL_COMMAND = STATE_RESPONSE_COMMAND + 1;
   int STATE_TRANSFER_GET_TRANSACTIONS_COMMAND = STATE_TRANSFER_CANCEL_COMMAND + 1;
   int STATE_TRANSFER_GET_LISTENERS_COMMAND = STATE_TRANSFER_GET_TRANSACTIONS_COMMAND + 1;
   int STATE_TRANSFER_START_COMMAND = STATE_TRANSFER_GET_LISTENERS_COMMAND + 1;
   int SYNC_CONSISTENT_HASH = STATE_TRANSFER_START_COMMAND + 1;
   int SYNC_REPLICATED_CONSISTENT_HASH = SYNC_CONSISTENT_HASH + 1;
   int TOPOLOGY_AWARE_CONSISTENT_HASH = SYNC_REPLICATED_CONSISTENT_HASH + 1;
   int TOPOLOGY_AWARE_SYNC_CONSISTENT_HASH = TOPOLOGY_AWARE_CONSISTENT_HASH + 1;
   int TOPOLOGY_UPDATE_COMMAND = TOPOLOGY_AWARE_SYNC_CONSISTENT_HASH + 1;
   int TOPOLOGY_UPDATE_STABLE_COMMAND = TOPOLOGY_UPDATE_COMMAND + 1;
   int TOUCH_COMMAND = TOPOLOGY_UPDATE_STABLE_COMMAND + 1;
   int TRANSACTION_INFO = TOUCH_COMMAND + 1;
   int TRANSIENT_CACHE_ENTRY = TRANSACTION_INFO + 1;
   int TRANSIENT_CACHE_VALUE = TRANSIENT_CACHE_ENTRY + 1;
   int TRANSIENT_MORTAL_CACHE_VALUE = TRANSIENT_CACHE_VALUE + 1;
   int TX_COMPLETION_NOTIFICATION_COMMAND = TRANSIENT_MORTAL_CACHE_VALUE + 1;
   int TX_READ_ONLY_KEY_COMMAND = TX_COMPLETION_NOTIFICATION_COMMAND + 1;
   int TX_READ_ONLY_MANY_COMMAND = TX_READ_ONLY_KEY_COMMAND + 1;
   int VERSIONED_COMMIT_COMMAND = TX_READ_ONLY_MANY_COMMAND + 1;
   int VERSIONED_PREPARE_COMMAND = VERSIONED_COMMIT_COMMAND + 1;
   int VERSIONED_RESULT = VERSIONED_PREPARE_COMMAND + 1;
   int VERSIONED_RESULTS = VERSIONED_RESULT + 1;
   int WRITE_ONLY_KEY_COMMAND = VERSIONED_RESULTS + 1;
   int WRITE_ONLY_KEY_VALUE_COMMAND = WRITE_ONLY_KEY_COMMAND + 1;
   int WRITE_ONLY_MANY_COMMAND = WRITE_ONLY_KEY_VALUE_COMMAND + 1;
   int WRITE_ONLY_MANY_ENTRIES_COMMAND = WRITE_ONLY_MANY_COMMAND + 1;
   int XSITE_AMEND_OFFLINE_STATUS_COMMAND = WRITE_ONLY_MANY_ENTRIES_COMMAND + 1;
   int XSITE_AUTO_STATE_TRANSFER_RESPONSE = XSITE_AMEND_OFFLINE_STATUS_COMMAND + 1;
   int XSITE_AUTO_TRANSFER_STATUS_COMMAND = XSITE_AUTO_STATE_TRANSFER_RESPONSE + 1;
   int XSITE_BRING_ONLINE_COMMAND = XSITE_AUTO_TRANSFER_STATUS_COMMAND + 1;
   int XSITE_LOCAL_EVENT_COMMAND = XSITE_BRING_ONLINE_COMMAND + 1;
   int XSITE_OFFLINE_STATUS_COMMAND = XSITE_LOCAL_EVENT_COMMAND + 1;
   int XSITE_REMOTE_EVENT_COMMAND = XSITE_OFFLINE_STATUS_COMMAND + 1;
   int XSITE_SINGLE_RPC_COMMAND = XSITE_REMOTE_EVENT_COMMAND + 1;
   int XSITE_EVENT = XSITE_SINGLE_RPC_COMMAND + 1;
   int XSITE_EVENT_TYPE = XSITE_EVENT + 1;
   int XSITE_SET_STATE_TRANSFER_MODE_COMMAND = XSITE_EVENT_TYPE + 1;
   int XSITE_SITE_STATE = XSITE_SET_STATE_TRANSFER_MODE_COMMAND + 1;
   int XSITE_STATE = XSITE_SITE_STATE + 1;
   int XSITE_STATE_PUSH_COMMAND = XSITE_STATE + 1;
   int XSITE_STATE_PUSH_REQUEST = XSITE_STATE_PUSH_COMMAND + 1;
   int XSITE_STATE_TRANSFER_CANCEL_SEND_COMMAND = XSITE_STATE_PUSH_REQUEST + 1;
   int XSITE_STATE_TRANSFER_CLEAR_STATUS_COMMAND = XSITE_STATE_TRANSFER_CANCEL_SEND_COMMAND + 1;
   int XSITE_STATE_TRANSFER_CONTROLLER_REQUEST = XSITE_STATE_TRANSFER_CLEAR_STATUS_COMMAND + 1;
   int XSITE_STATE_TRANSFER_FINISH_RECEIVE_COMMAND = XSITE_STATE_TRANSFER_CONTROLLER_REQUEST + 1;
   int XSITE_STATE_TRANSFER_FINISH_SEND_COMMAND = XSITE_STATE_TRANSFER_FINISH_RECEIVE_COMMAND + 1;
   int XSITE_STATE_TRANSFER_MODE = XSITE_STATE_TRANSFER_FINISH_SEND_COMMAND + 1;
   int XSITE_STATE_TRANSFER_RESTART_SENDING_COMMAND = XSITE_STATE_TRANSFER_MODE + 1;
   int XSITE_STATE_TRANSFER_START_RECEIVE_COMMAND = XSITE_STATE_TRANSFER_RESTART_SENDING_COMMAND + 1;
   int XSITE_STATE_TRANSFER_START_SEND_COMMAND = XSITE_STATE_TRANSFER_START_RECEIVE_COMMAND + 1;
   int XSITE_STATE_TRANSFER_STATUS = XSITE_STATE_TRANSFER_START_SEND_COMMAND + 1;
   int XSITE_STATE_TRANSFER_STATUS_REQUEST_COMMAND = XSITE_STATE_TRANSFER_STATUS + 1;
   int XSITE_STATUS_COMMAND = XSITE_STATE_TRANSFER_STATUS_REQUEST_COMMAND + 1;
   int XSITE_TAKE_OFFLINE_COMMAND = XSITE_STATUS_COMMAND + 1;
   int XSITE_BRING_ONLINE_RESPONSE = XSITE_TAKE_OFFLINE_COMMAND + 1;
   int XSITE_TAKE_OFFLINE_RESPONSE = XSITE_BRING_ONLINE_RESPONSE + 1;

   // MarshallableFunctions
   int MF_IDENTITY = XSITE_TAKE_OFFLINE_RESPONSE + 1;
   int MF_REMOVE = MF_IDENTITY + 1;
   int MF_REMOVE_IF_VALUE_EQUALS_RETURN_BOOLEAN = MF_REMOVE + 1;
   int MF_REMOVE_RETURN_BOOLEAN = MF_REMOVE_IF_VALUE_EQUALS_RETURN_BOOLEAN + 1;
   int MF_REMOVE_RETURN_PREV_OR_NULL = MF_REMOVE_RETURN_BOOLEAN + 1;
   int MF_RETURN_READ_ONLY_FIND_IS_PRESENT = MF_REMOVE_RETURN_PREV_OR_NULL + 1;
   int MF_RETURN_READ_ONLY_FIND_OR_NULL = MF_RETURN_READ_ONLY_FIND_IS_PRESENT + 1;
   int MF_RETURN_READ_WRITE_FIND = MF_RETURN_READ_ONLY_FIND_OR_NULL + 1;
   int MF_RETURN_READ_WRITE_GET = MF_RETURN_READ_WRITE_FIND + 1;
   int MF_RETURN_READ_WRITE_VIEW = MF_RETURN_READ_WRITE_GET + 1;
   int MF_SET_INTERNAL_CACHE_VALUE = MF_RETURN_READ_WRITE_VIEW + 1;
   int MF_SET_VALUE = MF_SET_INTERNAL_CACHE_VALUE + 1;
   int MF_SET_VALUE_META = MF_SET_VALUE + 1;
   int MF_SET_VALUE_IF_ABSENT_RETURN_BOOLEAN = MF_SET_VALUE_META + 1;
   int MF_SET_VALUE_IF_ABSENT_RETURN_PREV_OR_NULL = MF_SET_VALUE_IF_ABSENT_RETURN_BOOLEAN + 1;
   int MF_SET_VALUE_IF_EQUALS_RETURN_BOOLEAN = MF_SET_VALUE_IF_ABSENT_RETURN_PREV_OR_NULL + 1;
   int MF_SET_VALUE_IF_PRESENT_RETURN_PREV_OR_NULL = MF_SET_VALUE_IF_EQUALS_RETURN_BOOLEAN + 1;
   int MF_SET_VALUE_IF_PRESENT_RETURN_BOOLEAN = MF_SET_VALUE_IF_PRESENT_RETURN_PREV_OR_NULL + 1;
   int MF_SET_VALUE_METAS_IF_ABSENT_RETURN_BOOLEAN = MF_SET_VALUE_IF_PRESENT_RETURN_BOOLEAN + 1;
   int MF_SET_VALUE_METAS_IF_ABSENT_RETURN_PREV_OR_NULL = MF_SET_VALUE_METAS_IF_ABSENT_RETURN_BOOLEAN + 1;
   int MF_SET_VALUE_METAS_IF_PRESENT_RETURN_BOOLEAN = MF_SET_VALUE_METAS_IF_ABSENT_RETURN_PREV_OR_NULL + 1;
   int MF_SET_VALUE_METAS_IF_PRESENT_RETURN_PREV_OR_NULL = MF_SET_VALUE_METAS_IF_PRESENT_RETURN_BOOLEAN + 1;
   int MF_SET_VALUE_METAS_RETURN_PREV_OR_NULL = MF_SET_VALUE_METAS_IF_PRESENT_RETURN_PREV_OR_NULL + 1;
   int MF_SET_VALUE_METAS_RETURN_VIEW = MF_SET_VALUE_METAS_RETURN_PREV_OR_NULL + 1;
   int MF_SET_VALUE_RETURN_PREV_OR_NULL = MF_SET_VALUE_METAS_RETURN_VIEW + 1;
   int MF_SET_VALUE_RETURN_VIEW = MF_SET_VALUE_RETURN_PREV_OR_NULL + 1;

   // PublisherReducers
   int ALL_MATCH_REDUCER = MF_SET_VALUE_RETURN_VIEW + 1;
   int ANY_MATCH_REDUCER = ALL_MATCH_REDUCER + 1;
   int AND_FINALIZER = ANY_MATCH_REDUCER + 1;
   int COLLECT_REDUCER = AND_FINALIZER + 1;
   int COLLECTOR_FINALIZER = COLLECT_REDUCER + 1;
   int COLLECTOR_REDUCER = COLLECTOR_FINALIZER + 1;
   int COMBINER_FINALIZER = COLLECTOR_REDUCER + 1;
   int FIND_FIRST_REDUCER_FINALIZER = COMBINER_FINALIZER + 1;
   int MAX_REDUCER_FINALIZER = FIND_FIRST_REDUCER_FINALIZER + 1;
   int MIN_REDUCER_FINALIZER = MAX_REDUCER_FINALIZER + 1;
   int NONE_MATCH_REDUCER = MIN_REDUCER_FINALIZER + 1;
   int OR_FINALIZER = NONE_MATCH_REDUCER + 1;
   int REDUCE_WITH_IDENTITY_REDUCER = OR_FINALIZER + 1;
   int REDUCE_WITH_INITIAL_SUPPLIER_REDUCER = REDUCE_WITH_IDENTITY_REDUCER + 1;
   int REDUCE_REDUCER_FINALIZER = REDUCE_WITH_INITIAL_SUPPLIER_REDUCER + 1;
   int SUM_REDUCER = REDUCE_REDUCER_FINALIZER + 1;
   int SUM_FINALIZER = SUM_REDUCER + 1;
   int TO_ARRAY_FINALIZER = SUM_FINALIZER + 1;
   int TO_ARRAY_REDUCER = TO_ARRAY_FINALIZER + 1;

   // StreamMarshalling
   int ALWAYS_TRUE_PREDICATE = TO_ARRAY_REDUCER + 1;
   int ENTRY_KEY_FUNCTION = ALWAYS_TRUE_PREDICATE + 1;
   int ENTRY_VALUE_FUNCTION = ENTRY_KEY_FUNCTION + 1;
   int EQUALITY_PREDICATE = ENTRY_VALUE_FUNCTION + 1;
   int IDENTITY_FUNCTION = EQUALITY_PREDICATE + 1;
   int KEY_ENTRY_FUNCTION = IDENTITY_FUNCTION + 1;
   int NON_NULL_PREDICATE = KEY_ENTRY_FUNCTION + 1;

   // TODO add TypeIds for org.infinispan.stream.impl.intops & CacheBiConsumers

   // Counter range 4000 -> 4199
   int COUNTERS_LOWER_BOUND = 4000;
   int COUNTER_STATE = COUNTERS_LOWER_BOUND;
   int COUNTER_CONFIGURATION = COUNTERS_LOWER_BOUND + 1;
   int COUNTER_TYPE = COUNTERS_LOWER_BOUND + 2;
   int COUNTER_STORAGE = COUNTERS_LOWER_BOUND + 3;
   int COUNTER_FUNCTION_ADD = COUNTERS_LOWER_BOUND + 4;
   int COUNTER_FUNCTION_CAS = COUNTERS_LOWER_BOUND + 5;
   int COUNTER_FUNCTION_CREATE_AND_ADD = COUNTERS_LOWER_BOUND + 6;
   int COUNTER_FUNCTION_CREATE_AND_CAS = COUNTERS_LOWER_BOUND + 7;
   int COUNTER_FUNCTION_CREATE_AND_SET = COUNTERS_LOWER_BOUND + 8;
   int COUNTER_FUNCTION_INITIALIZE_COUNTER = COUNTERS_LOWER_BOUND + 9;
   int COUNTER_FUNCTION_READ = COUNTERS_LOWER_BOUND + 10;
   int COUNTER_FUNCTION_REMOVE = COUNTERS_LOWER_BOUND + 11;
   int COUNTER_FUNCTION_RESET = COUNTERS_LOWER_BOUND + 12;
   int COUNTER_FUNCTION_SET = COUNTERS_LOWER_BOUND + 13;

   // Query range 4200 -> 4399
   int QUERY_LOWER_BOUND = 4200;
   //int KNOWN_CLASS_KEY = QUERY_LOWER_BOUND;
   int QUERY_METRICS = QUERY_LOWER_BOUND + 1;
   int LOCAL_QUERY_STATS = QUERY_LOWER_BOUND + 2;
   int LOCAL_INDEX_STATS = QUERY_LOWER_BOUND + 3;
   int INDEX_INFO = QUERY_LOWER_BOUND + 4;
   int INDEX_INFO_ENTRY = QUERY_LOWER_BOUND + 5;
   int SEARCH_STATISTICS = QUERY_LOWER_BOUND + 6;
   int STATS_TASK = QUERY_LOWER_BOUND + 7;
   int CLUSTERED_QUERY_OPERATION  = QUERY_LOWER_BOUND + 8;
   int CQ_COMMAND_TYPE = QUERY_LOWER_BOUND + 9;
   int HIBERNATE_POJO_RAW_TYPE_IDENTIFIER = QUERY_LOWER_BOUND + 10;
   int ICKLE_CACHE_EVENT_FILTER_CONVERTER = QUERY_LOWER_BOUND + 11;
   int ICKLE_CONTINOUS_QUERY_CACHE_EVENT_FILTER_CONVERTER = QUERY_LOWER_BOUND + 12;
   int ICKLE_CONTINOUS_QUERY_RESULT = QUERY_LOWER_BOUND + 13;
   int ICKLE_CONTINOUS_QUERY_RESULT_TYPE = QUERY_LOWER_BOUND + 14;
   int ICKLE_DELETE_FUNCTION = QUERY_LOWER_BOUND + 15;
   int ICKE_FILTER_AND_CONVERTER = QUERY_LOWER_BOUND + 16;
   int ICKLE_FILTER_RESULT = QUERY_LOWER_BOUND + 17;
   int ICKLE_PARSING_RESULT_STATEMENT_TYPE = QUERY_LOWER_BOUND + 18;
   int INDEX_WORKER = QUERY_LOWER_BOUND + 19;
   int NODE_TOP_DOCS = QUERY_LOWER_BOUND + 20;
   int QUERY_DEFINITION = QUERY_LOWER_BOUND + 21;
   int QUERY_RESPONSE = QUERY_LOWER_BOUND + 22;
   int SEGMENTS_CLUSTERED_QUERY_COMMAND = QUERY_LOWER_BOUND + 23;

   // Remote Query range 4400 -> 4599
   int REMOTE_QUERY_LOWER_BOUND = 4400;
   int REMOTE_QUERY_REQUEST = REMOTE_QUERY_LOWER_BOUND;
   int REMOTE_QUERY_RESPONSE = REMOTE_QUERY_LOWER_BOUND + 1;
   int REMOTE_QUERY_ICKLE_FILTER_RESULT = REMOTE_QUERY_LOWER_BOUND + 2;
   int REMOTE_QUERY_ICKLE_CONTINUOUS_QUERY_RESULT = REMOTE_QUERY_LOWER_BOUND + 3;

   // Lucene Directory 4600 -> 4799
   int LUCENE_LOWER_BOUND = 4600;
   int CHUNK_CACHE_KEY = LUCENE_LOWER_BOUND;
   int FILE_CACHE_KEY = LUCENE_LOWER_BOUND + 1;
   int FILE_LIST_CACHE_KEY = LUCENE_LOWER_BOUND + 2;
   int FILE_METADATA = LUCENE_LOWER_BOUND + 3;
   int FILE_READ_LOCK_KEY = LUCENE_LOWER_BOUND + 4;
   int FILE_LIST_CACHE_VALUE = LUCENE_LOWER_BOUND + 5;
   int LUCENE_FIELD_DOC = LUCENE_LOWER_BOUND + 6;
   int LUCENE_SORT = LUCENE_LOWER_BOUND + 7;
   int LUCENE_SORT_FIELD = LUCENE_LOWER_BOUND + 8;
   int LUCENE_SORT_FIELD_TYPE = LUCENE_LOWER_BOUND + 9;
   int LUCENE_TOP_DOCS = LUCENE_LOWER_BOUND + 10;
   int LUCENE_TOP_FIELD_DOCS = LUCENE_LOWER_BOUND + 11;
   int LUCENE_SCORE_DOC = LUCENE_LOWER_BOUND + 12;
   int LUCENE_TOTAL_HITS = LUCENE_LOWER_BOUND + 13;
   int LUCENE_BYTES_REF = LUCENE_LOWER_BOUND + 14;

   // Tasks + Scripting 4800 -> 4999
   int SCRIPTING_LOWER_BOUND = 4800;
   int EXECUTION_MODE = SCRIPTING_LOWER_BOUND;
   int SCRIPT_METADATA = SCRIPTING_LOWER_BOUND + 1;
   int DISTRIBUTED_SERVER_TASK = SCRIPTING_LOWER_BOUND + 2;
   int DISTRIBUTED_SERVER_TASK_PARAMETER = SCRIPTING_LOWER_BOUND + 3;
   int DISTRIBUTED_SERVER_TASK_CONTEXT = SCRIPTING_LOWER_BOUND + 4;

   // Memcached 5000 -> 5099
   int MEMCACHED_LOWER_BOUND = 5000;
   int MEMCACHED_METADATA = MEMCACHED_LOWER_BOUND;

   // RocksDB 5100 -> 5199
   int ROCKSDB_LOWER_BOUND = 5100;
   int ROCKSDB_EXPIRY_BUCKET = ROCKSDB_LOWER_BOUND;
   int ROCKSDB_PERSISTED_METADATA = ROCKSDB_LOWER_BOUND + 1;

   // Event-logger 5200 -> 5299
   int EVENT_LOGGER_LOWER_BOUND = 5200;
   int SERVER_EVENT_IMPL = EVENT_LOGGER_LOWER_BOUND;

   // MultiMap 5300 -> 5399
   int MULTIMAP_LOWER_BOUND = 5300;
   int MULTIMAP_BUCKET = MULTIMAP_LOWER_BOUND;
   int MULTIMAP_LIST_BUCKET = MULTIMAP_LOWER_BOUND + 2;
   int MULTIMAP_HASH_MAP_BUCKET = MULTIMAP_LOWER_BOUND + 3;
   int MULTIMAP_HASH_MAP_BUCKET_ENTRY = MULTIMAP_LOWER_BOUND + 4;
   int MULTIMAP_SET_BUCKET = MULTIMAP_LOWER_BOUND + 5;
   int MULTIMAP_OBJECT_WRAPPER = MULTIMAP_LOWER_BOUND + 6;
   int MULTIMAP_SORTED_SET_BUCKET = MULTIMAP_LOWER_BOUND + 7;
   int MULTIMAP_SCORED_VALUE = MULTIMAP_LOWER_BOUND + 8;
   int MULTIMAP_INDEX_VALUE = MULTIMAP_LOWER_BOUND + 9;
   int MULTIMAP_ADD_MANY_FUNCTION = MULTIMAP_LOWER_BOUND + 10;
   int MULTIMAP_CONTAINS_FUNCTION = MULTIMAP_LOWER_BOUND + 11;
   int MULTIMAP_COUNT_FUNCTION = MULTIMAP_LOWER_BOUND + 12;
   int MULTIMAP_HASH_MAP_PUT_FUNCTION = MULTIMAP_LOWER_BOUND + 13;
   int MULTIMAP_HASH_MAP_KEY_SET_FUNCTION = MULTIMAP_LOWER_BOUND + 14;
   int MULTIMAP_HASH_MAP_VALUES_FUNCTION = MULTIMAP_LOWER_BOUND + 15;
   int MULTIMAP_HASH_MAP_REMOVE_FUNCTION = MULTIMAP_LOWER_BOUND + 16;
   int MULTIMAP_HASH_MAP_REPLACE_FUNCTION = MULTIMAP_LOWER_BOUND + 17;
   int MULTIMAP_GET_FUNCTION = MULTIMAP_LOWER_BOUND + 18;
   int MULTIMAP_INCR_FUNCTION = MULTIMAP_LOWER_BOUND + 19;
   int MULTIMAP_INDEX_FUNCTION = MULTIMAP_LOWER_BOUND + 20;
   int MULTIMAP_INDEX_OF_FUNCTION = MULTIMAP_LOWER_BOUND + 21;
   int MULTIMAP_INDEX_OF_SORTED_SET_FUNCTION = MULTIMAP_LOWER_BOUND + 22;
   int MULTIMAP_INSERT_FUNCTION = MULTIMAP_LOWER_BOUND + 23;
   int MULTIMAP_OFFER_FUNCTION = MULTIMAP_LOWER_BOUND + 24;
   int MULTIMAP_POLL_FUNCTION = MULTIMAP_LOWER_BOUND + 25;
   int MULTIMAP_POP_FUNCTION = MULTIMAP_LOWER_BOUND + 26;
   int MULTIMAP_PUT_FUNCTION = MULTIMAP_LOWER_BOUND + 27;
   int MULTIMAP_REMOVE_FUNCTION = MULTIMAP_LOWER_BOUND + 28;
   int MULTIMAP_REMOVE_COUNT_FUNCTION = MULTIMAP_LOWER_BOUND + 29;
   int MULTIMAP_REMOVE_MANY_FUNCTION = MULTIMAP_LOWER_BOUND + 30;
   int MULTIMAP_REPLACE_LIST_FUNCTION = MULTIMAP_LOWER_BOUND + 31;
   int MULTIMAP_ROTATE_FUNCTION = MULTIMAP_LOWER_BOUND + 32;
   int MULTIMAP_SUBLIST_FUNCTION = MULTIMAP_LOWER_BOUND + 33;
   int MULTIMAP_S_ADD_FUNCTION = MULTIMAP_LOWER_BOUND + 34;
   int MULTIMAP_S_GET_FUNCTION = MULTIMAP_LOWER_BOUND + 35;
   int MULTIMAP_S_M_IS_MEMBER_FUNCTION = MULTIMAP_LOWER_BOUND + 36;
   int MULTIMAP_S_SET_FUNCTION = MULTIMAP_LOWER_BOUND + 37;
   int MULTIMAP_S_POP_FUNCTION = MULTIMAP_LOWER_BOUND + 38;
   int MULTIMAP_S_REMOVE_FUNCTION = MULTIMAP_LOWER_BOUND + 39;
   int MULTIMAP_SET_FUNCTION = MULTIMAP_LOWER_BOUND + 40;
   int MULTIMAP_SCORE_FUNCTION = MULTIMAP_LOWER_BOUND + 41;
   int MULTIMAP_SORTED_SET_AGGREGATE_FUNCTION = MULTIMAP_LOWER_BOUND + 42;
   int MULTIMAP_SORTED_SET_AGGREGATE_FUNCTION_TYPE = MULTIMAP_LOWER_BOUND + 43;
   int MULTIMAP_SORTED_SET_BUCKET_AGGREGATE_FUNCTION = MULTIMAP_LOWER_BOUND + 44;
   int MULTIMAP_SORTED_SET_OPERATION_TYPE = MULTIMAP_LOWER_BOUND + 45;
   int MULTIMAP_SORTED_SET_RANDOM_FUNCTION = MULTIMAP_LOWER_BOUND + 46;
   int MULTIMAP_SUBSET_FUNCTION = MULTIMAP_LOWER_BOUND + 47;
   int MULTIMAP_TRIM_FUNCTION = MULTIMAP_LOWER_BOUND + 48;

   // Server Core 5400 -> 5799
   int SERVER_CORE_LOWER_BOUND = 5400;
   int IGNORED_CACHES = SERVER_CORE_LOWER_BOUND;
   int CACHE_BACKUP_ENTRY = SERVER_CORE_LOWER_BOUND + 1;
   int COUNTER_BACKUP_ENTRY = SERVER_CORE_LOWER_BOUND + 2;
   int IP_FILTER_RULES = SERVER_CORE_LOWER_BOUND + 3;
   int IP_FILTER_RULE = SERVER_CORE_LOWER_BOUND + 4;

   // JDBC Store 5800 -> 5899
   int JDBC_LOWER_BOUND = 5800;
   int JDBC_PERSISTED_METADATA = JDBC_LOWER_BOUND;

   // Spring integration 5900 -> 5999
   int SPRING_LOWER_BOUND = 5900;
   @Deprecated(forRemoval = true, since = "13.0")
   int SPRING_NULL_VALUE = SPRING_LOWER_BOUND;
   int SPRING_SESSION = SPRING_LOWER_BOUND + 1;
   int SPRING_SESSION_ATTRIBUTE = SPRING_LOWER_BOUND + 2;
   int SPRING_SESSION_REMAP = SPRING_LOWER_BOUND + 3;

   // Data distribution metrics 6000 -> 6099
   int DATA_DISTRIBUTION_LOWER_BOUND = 6000;
   int CACHE_DISTRIBUTION_INFO = DATA_DISTRIBUTION_LOWER_BOUND;
   int CLUSTER_DISTRIBUTION_INFO = DATA_DISTRIBUTION_LOWER_BOUND + 1;
   int KEY_DISTRIBUTION_INFO = DATA_DISTRIBUTION_LOWER_BOUND + 2;

   // RESP Objects 6100 -> 6299
   int RESP_LOWER_BOUND = 6100;
   int RESP_HYPER_LOG_LOG = RESP_LOWER_BOUND + 1;
   int RESP_HYPER_LOG_LOG_EXPLICIT = RESP_LOWER_BOUND + 2;
   int RESP_HYPER_LOG_LOG_COMPACT = RESP_LOWER_BOUND + 3;
   int RESP_COMPOSED_FILTER_CONVERTER = RESP_LOWER_BOUND + 4;
   int RESP_EVENT_LISTENER_CONVERTER = RESP_LOWER_BOUND + 5;
   int RESP_EVENT_LISTENER_KEYS_FILTER = RESP_LOWER_BOUND + 6;
   int RESP_GLOB_MATCH_FILTER_CONVERTER = RESP_LOWER_BOUND + 7;
   int RESP_TYPE_FILTER_CONVERTER = RESP_LOWER_BOUND + 8;
   int RESP_WATCH = RESP_LOWER_BOUND + 9;
   int RESP_WATCH_TX_EVENT_CONVERTER_EMPTY = RESP_LOWER_BOUND + 10;

   // Clustered Locks 6300 -> 6399
   int CLUSTERED_LOCK_LOWER_BOUND = 6300;
   int CLUSTERED_LOCK_KEY = CLUSTERED_LOCK_LOWER_BOUND;
   int CLUSTERED_LOCK_FILTER = CLUSTERED_LOCK_LOWER_BOUND + 1;
   int CLUSTERED_LOCK_FUNCTION_IS_LOCKED = CLUSTERED_LOCK_LOWER_BOUND + 2;
   int CLUSTERED_LOCK_FUNCTION_LOCK = CLUSTERED_LOCK_LOWER_BOUND + 3;
   int CLUSTERED_LOCK_FUNCTION_UNLOCK = CLUSTERED_LOCK_LOWER_BOUND + 4;
   int CLUSTERED_LOCK_STATE = CLUSTERED_LOCK_LOWER_BOUND + 5;
   int CLUSTERED_LOCK_VALUE = CLUSTERED_LOCK_LOWER_BOUND + 6;

   // Remote Store 6400 -> 6499
   int REMOTE_STORE_LOWER_BOUND = 6400;
   int REMOTE_STORE_ADD = REMOTE_STORE_LOWER_BOUND;
   int REMOTE_STORE_CHECK = REMOTE_STORE_LOWER_BOUND + 1;
   int REMOTE_STORE_DISCONNECT = REMOTE_STORE_LOWER_BOUND + 2;
   int REMOTE_STORE_MIGRATION_TASK = REMOTE_STORE_LOWER_BOUND + 3;
   int REMOTE_STORE_MIGRATION_TASK_ENTRY_WRITER = REMOTE_STORE_LOWER_BOUND + 4;
   int REMOTE_STORE_REMOVED_FILTER = REMOTE_STORE_LOWER_BOUND + 5;

   // Server Core 6500 -> 6599
   int SERVER_LOWER_BOUND = 6500;
   int SERVER_ITERATION_FILTER = SERVER_LOWER_BOUND;

   // Server HotRod 6600 -> 6799
   int SERVER_HR_LOWER_BOUND = 6600;
   int SERVER_HR_CACHE_XID = SERVER_HR_LOWER_BOUND;
   int SERVER_HR_CONDITION_MARK_ROLLBACK_FUNCTION = SERVER_HR_LOWER_BOUND + 1;
   int SERVER_HR_CHECK_ADDRESS_TASK = SERVER_HR_LOWER_BOUND + 2;
   int SERVER_HR_CLIENT_ADDRESS = SERVER_HR_LOWER_BOUND + 3;
   int SERVER_HR_KEY_VALUE_VERSION_CONVERTER = SERVER_HR_LOWER_BOUND + 4;
   int SERVER_HR_KEY_VALUE_WITH_PREVIOUS_CONVERTER = SERVER_HR_LOWER_BOUND + 5;
   int SERVER_HR_FUNCTION_CREATE_STATE = SERVER_HR_LOWER_BOUND + 6;
   int SERVER_HR_FUNCTION_SET_COMPLETED_TX = SERVER_HR_LOWER_BOUND + 7;
   int SERVER_HR_FUNCTION_DECISION = SERVER_HR_LOWER_BOUND + 8;
   int SERVER_HR_FUNCTION_PREPARED = SERVER_HR_LOWER_BOUND + 9;
   int SERVER_HR_FUNCTION_PREPARING_DECISION = SERVER_HR_LOWER_BOUND + 10;
   int SERVER_HR_MULTI_HOMED_SERVER_ADDRESS = SERVER_HR_LOWER_BOUND + 11;
   int SERVER_HR_MULTI_HOMED_SERVER_ADDRESS_INET = SERVER_HR_LOWER_BOUND + 12;
   int SERVER_HR_SINGLE_HOMED_SERVER_ADDRESS = SERVER_HR_LOWER_BOUND + 13;
   int SERVER_HR_TO_EMPTY_BYTES_KEY_VALUE_FILTER_CONVERTER = SERVER_HR_LOWER_BOUND + 14;
   int SERVER_HR_TX_FORWARD_COMMIT_COMMAND = SERVER_HR_LOWER_BOUND + 15;
   int SERVER_HR_TX_FORWARD_ROLLBACK_COMMAND = SERVER_HR_LOWER_BOUND + 16;
   int SERVER_HR_TX_STATE = SERVER_HR_LOWER_BOUND + 17;
   int SERVER_HR_TX_STATUS = SERVER_HR_LOWER_BOUND + 18;
   int SERVER_HR_XID_PREDICATE = SERVER_HR_LOWER_BOUND + 19;
}
