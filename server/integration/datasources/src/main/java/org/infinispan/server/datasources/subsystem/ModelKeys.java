package org.infinispan.server.datasources.subsystem;

/**
 * @author Ryan Emerson
 * @since 9.2
 */
class ModelKeys {
   static final String DATASOURCES = "datasources";

   static final String DATA_SOURCE = "data-source";

   static final String XA_DATASOURCE = "xa-data-source";

   static final String CONNECTION_URL_NAME = "connection-url";

   static final String JDBC_DRIVER_NAME = "jdbc-driver";

   static final String DATASOURCE_DRIVER_CLASS_NAME = "driver-class";

   static final String DATASOURCE_CLASS_NAME = "datasource-class";

   static final String DATASOURCE_DRIVER_NAME = "driver-name";

   static final String DRIVER_NAME_NAME = "driver-name";

   static final String DRIVER_MODULE_NAME_NAME = "driver-module-name";

   static final String DRIVER_MAJOR_VERSION_NAME = "driver-major-version";

   static final String DRIVER_MINOR_VERSION_NAME = "driver-minor-version";

   static final String DRIVER_CLASS_NAME_NAME = "driver-class-name";

   static final String DRIVER_DATASOURCE_CLASS_NAME_NAME = "driver-datasource-class-name";

   static final String DRIVER_XA_DATASOURCE_CLASS_NAME_NAME = "driver-xa-datasource-class-name";

   static final String CONNECTION_PROPERTIES_NAME = "connection-properties";

   static final String CONNECTION_PROPERTY_VALUE_NAME = "value";

   static final String NEW_CONNECTION_SQL_NAME = "new-connection-sql";

   static final String TRANSACTION_ISOLATION_NAME = "transaction-isolation";

   static final String URL_DELIMITER_NAME = "url-delimiter";

   static final String URL_PROPERTY_NAME = "url-property";

   static final String URL_SELECTOR_STRATEGY_CLASS_NAME_NAME = "url-selector-strategy-class-name";

   static final String USE_JAVA_CONTEXT_NAME = "use-java-context";

   static final String CONNECTABLE_NAME = "connectable";

   static final String MCP_NAME = "mcp";

   static final String ENLISTMENT_TRACE_NAME = "enlistment-trace";

   static final String TRACKING_NAME = "tracking";

   static final String POOLNAME_NAME = "pool-name";

   static final String ENABLED_NAME = "enabled";

   static final String JTA_NAME = "jta";

   static final String JNDINAME_NAME = "jndi-name";

   static final String ALLOCATION_RETRY_NAME = "allocation-retry";

   static final String ALLOCATION_RETRY_WAIT_MILLIS_NAME = "allocation-retry-wait-millis";

   static final String ALLOW_MULTIPLE_USERS_NAME = "allow-multiple-users";

   static final String CONNECTION_LISTENER_CLASS_NAME = "connection-listener-class";

   static final String CONNECTION_LISTENER_PROPERTY_NAME = "connection-listener-property";

   static final String SETTXQUERYTIMEOUT_NAME = "set-tx-query-timeout";

   static final String XA_RESOURCE_TIMEOUT_NAME = "xa-resource-timeout";

   static final String QUERYTIMEOUT_NAME = "query-timeout";

   static final String USETRYLOCK_NAME = "use-try-lock";

   static final String USERNAME_NAME = "user-name";

   static final String PASSWORD_NAME = "password";

   static final String SECURITY_DOMAIN_NAME = "security-domain";

   static final String SHAREPREPAREDSTATEMENTS_NAME = "share-prepared-statements";

   static final String PREPAREDSTATEMENTSCACHESIZE_NAME = "prepared-statements-cache-size";

   static final String TRACKSTATEMENTS_NAME = "track-statements";

   static final String VALID_CONNECTION_CHECKER_CLASSNAME_NAME = "valid-connection-checker-class-name";

   static final String CHECKVALIDCONNECTIONSQL_NAME = "check-valid-connection-sql";

   static final String VALIDATEONMATCH_NAME = "validate-on-match";

   static final String SPY_NAME = "spy";

   static final String USE_CCM_NAME = "use-ccm";

   static final String STALECONNECTIONCHECKERCLASSNAME_NAME = "stale-connection-checker-class-name";

   static final String EXCEPTIONSORTERCLASSNAME_NAME = "exception-sorter-class-name";

   static final String XADATASOURCEPROPERTIES_NAME = "xa-datasource-properties";

   static final String XADATASOURCEPROPERTIES_VALUE_NAME = "value";

   static final String XADATASOURCECLASS_NAME = "xa-datasource-class";

   static final String INTERLEAVING_NAME = "interleaving";

   static final String NOTXSEPARATEPOOL_NAME = "no-tx-separate-pool";

   static final String PAD_XID_NAME = "pad-xid";

   static final String SAME_RM_OVERRIDE_NAME = "same-rm-override";

   static final String WRAP_XA_RESOURCE_NAME = "wrap-xa-resource";

   static final String EXCEPTIONSORTER_PROPERTIES_NAME = "exception-sorter-properties";

   static final String STALECONNECTIONCHECKER_PROPERTIES_NAME = "stale-connection-checker-properties";

   static final String VALIDCONNECTIONCHECKER_PROPERTIES_NAME = "valid-connection-checker-properties";

   static final String REAUTHPLUGIN_CLASSNAME_NAME = "reauth-plugin-class-name";

   static final String REAUTHPLUGIN_PROPERTIES_NAME = "reauth-plugin-properties";

   static final String RECOVERY_USERNAME_NAME = "recovery-username";

   static final String RECOVERY_PASSWORD_NAME = "recovery-password";

   static final String RECOVERY_SECURITY_DOMAIN_NAME = "recovery-security-domain";

   static final String RECOVER_PLUGIN_CLASSNAME_NAME = "recovery-plugin-class-name";

   static final String RECOVER_PLUGIN_PROPERTIES_NAME = "recovery-plugin-properties";

   static final String NO_RECOVERY_NAME = "no-recovery";
}
