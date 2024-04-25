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
   int ACCEPT_ALL_KEY_VALUE_FILTER = BITSET + 1;
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
   int FUNCTION_MAPPER = EXCEPTION_ACK_COMMAND + 1;
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
   int IRAC_PUT_MANY_REQUEST_REMOVE  = IRAC_PUT_MANY_REQUEST_EXPIRE + 1;
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
   int KEY_VALUE_FILTER_CONVERTER_AS_CACHE_KEY_VALUE_CONVERTER = KEY_VALUE_FILTER_CONVERTER_AS_CACHE_EVENT_FILTER_CONVERTER +1;
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
   int XSITE_STATE = XSITE_SET_STATE_TRANSFER_MODE_COMMAND + 1;
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
   int XSITE_STATE_TRANSFER_STATUS_REQUEST_COMMAND = XSITE_STATE_TRANSFER_START_SEND_COMMAND + 1;
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

   // Query range 4200 -> 4399
   int QUERY_LOWER_BOUND = 4200;
   int QUERY_METRICS = QUERY_LOWER_BOUND + 1;
   int LOCAL_QUERY_STATS = QUERY_LOWER_BOUND + 2;
   int LOCAL_INDEX_STATS = QUERY_LOWER_BOUND + 3;
   int INDEX_INFO = QUERY_LOWER_BOUND + 4;
   int INDEX_INFO_ENTRY = QUERY_LOWER_BOUND + 5;
   int SEARCH_STATISTICS = QUERY_LOWER_BOUND + 6;
   int STATS_TASK = QUERY_LOWER_BOUND + 7;
   //int KNOWN_CLASS_KEY = QUERY_LOWER_BOUND;

   // Remote Query range 4400 -> 4599
   int REMOTE_QUERY_LOWER_BOUND = 4400;
   int REMOTE_QUERY_REQUEST = REMOTE_QUERY_LOWER_BOUND;
   int REMOTE_QUERY_RESPONSE = REMOTE_QUERY_LOWER_BOUND + 1;
   int ICKLE_FILTER_RESULT = REMOTE_QUERY_LOWER_BOUND + 2;
   int ICKLE_CONTINUOUS_QUERY_RESULT = REMOTE_QUERY_LOWER_BOUND + 3;

   // Lucene Directory 4600 -> 4799
   int LUCENE_LOWER_BOUND = 4600;
   int CHUNK_CACHE_KEY = LUCENE_LOWER_BOUND;
   int FILE_CACHE_KEY = LUCENE_LOWER_BOUND + 1;
   int FILE_LIST_CACHE_KEY = LUCENE_LOWER_BOUND + 2;
   int FILE_METADATA = LUCENE_LOWER_BOUND + 3;
   int FILE_READ_LOCK_KEY = LUCENE_LOWER_BOUND + 4;
   int FILE_LIST_CACHE_VALUE = LUCENE_LOWER_BOUND + 5;

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
   @Deprecated(forRemoval=true, since = "13.0")
   int SPRING_NULL_VALUE = SPRING_LOWER_BOUND;
   int SPRING_SESSION = SPRING_LOWER_BOUND + 1;
   int SPRING_SESSION_ATTRIBUTE = SPRING_LOWER_BOUND + 2;
   int SPRING_SESSION_REMAP = SPRING_LOWER_BOUND + 3;

   // Data distribution metrics 6000 -> 6099
   int DATA_DISTRIBUTION_LOWER_BOUND = 6000;
   int CACHE_DISTRIBUTION_INFO = DATA_DISTRIBUTION_LOWER_BOUND;
   int CLUSTER_DISTRIBUTION_INFO = DATA_DISTRIBUTION_LOWER_BOUND + 1;
   int KEY_DISTRIBUTION_INFO = DATA_DISTRIBUTION_LOWER_BOUND + 2;

   // RESP Objects 6100 -> 6199
   int RESP_LOWER_BOUND = 6100;
   int RESP_HYPER_LOG_LOG = RESP_LOWER_BOUND + 1;
   int RESP_HYPER_LOG_LOG_EXPLICIT = RESP_LOWER_BOUND + 2;
   int RESP_HYPER_LOG_LOG_COMPACT = RESP_LOWER_BOUND + 3;
}
