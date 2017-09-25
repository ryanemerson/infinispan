package org.infinispan.server.datasources.subsystem;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Ryan Emerson
 * @since 9.2
 */
public enum Element {

   UNKNOWN(null),
   DATASOURCES(ModelKeys.DATASOURCES),
   DATA_SOURCE(ModelKeys.DATA_SOURCE),
   XA_DATASOURCE(ModelKeys.XA_DATASOURCE),
   CONNECTION_URL_NAME(ModelKeys.CONNECTION_URL_NAME),
   JDBC_DRIVER_NAME(ModelKeys.JDBC_DRIVER_NAME),
   DATASOURCE_DRIVER_CLASS_NAME(ModelKeys.DATASOURCE_DRIVER_CLASS_NAME),
   DATASOURCE_CLASS_NAME(ModelKeys.DATASOURCE_CLASS_NAME),
   DATASOURCE_DRIVER_NAME(ModelKeys.DATASOURCE_DRIVER_NAME),
   DRIVER_NAME_NAME(ModelKeys.DRIVER_NAME_NAME, true),
   DRIVER_MODULE_NAME_NAME(ModelKeys.DRIVER_MODULE_NAME_NAME, true),
   DRIVER_MAJOR_VERSION_NAME(ModelKeys.DRIVER_MAJOR_VERSION_NAME, true),
   DRIVER_MINOR_VERSION_NAME(ModelKeys.DRIVER_MINOR_VERSION_NAME, true),
   DRIVER_CLASS_NAME_NAME(ModelKeys.DRIVER_CLASS_NAME_NAME, true),
   DRIVER_DATASOURCE_CLASS_NAME_NAME(ModelKeys.DRIVER_DATASOURCE_CLASS_NAME_NAME, true),
   DRIVER_XA_DATASOURCE_CLASS_NAME_NAME(ModelKeys.DRIVER_XA_DATASOURCE_CLASS_NAME_NAME, true),
   CONNECTION_PROPERTIES_NAME(ModelKeys.CONNECTION_PROPERTIES_NAME, true),
   CONNECTION_PROPERTY_VALUE_NAME(ModelKeys.CONNECTION_PROPERTY_VALUE_NAME, true),
   NEW_CONNECTION_SQL_NAME(ModelKeys.NEW_CONNECTION_SQL_NAME, true),
   TRANSACTION_ISOLATION_NAME(ModelKeys.TRANSACTION_ISOLATION_NAME, true),
   URL_DELIMITER_NAME(ModelKeys.URL_DELIMITER_NAME, true),
   URL_PROPERTY_NAME(ModelKeys.URL_PROPERTY_NAME, true),
   URL_SELECTOR_STRATEGY_CLASS_NAME_NAME(ModelKeys.URL_SELECTOR_STRATEGY_CLASS_NAME_NAME, true),
   USE_JAVA_CONTEXT_NAME(ModelKeys.USE_JAVA_CONTEXT_NAME, true),
   CONNECTABLE_NAME(ModelKeys.CONNECTABLE_NAME, true),
   MCP_NAME(ModelKeys.MCP_NAME, true),
   ENLISTMENT_TRACE_NAME(ModelKeys.ENLISTMENT_TRACE_NAME, true),
   TRACKING_NAME(ModelKeys.TRACKING_NAME, true),
   POOLNAME_NAME(ModelKeys.POOLNAME_NAME, true),
   ENABLED_NAME(ModelKeys.ENABLED_NAME, true),
   JTA_NAME(ModelKeys.JTA_NAME, true),
   JNDINAME_NAME(ModelKeys.JNDINAME_NAME, true),
   ALLOCATION_RETRY_NAME(ModelKeys.ALLOCATION_RETRY_NAME, true),
   ALLOCATION_RETRY_WAIT_MILLIS_NAME(ModelKeys.ALLOCATION_RETRY_WAIT_MILLIS_NAME, true),
   ALLOW_MULTIPLE_USERS_NAME(ModelKeys.ALLOW_MULTIPLE_USERS_NAME, true),
   CONNECTION_LISTENER_CLASS_NAME(ModelKeys.CONNECTION_LISTENER_CLASS_NAME, true),
   CONNECTION_LISTENER_PROPERTY_NAME(ModelKeys.CONNECTION_LISTENER_PROPERTY_NAME, true),
   SETTXQUERYTIMEOUT_NAME(ModelKeys.SETTXQUERYTIMEOUT_NAME, true),
   XA_RESOURCE_TIMEOUT_NAME(ModelKeys.XA_RESOURCE_TIMEOUT_NAME, true),
   QUERYTIMEOUT_NAME(ModelKeys.QUERYTIMEOUT_NAME, true),
   USETRYLOCK_NAME(ModelKeys.USETRYLOCK_NAME, true),
   USERNAME_NAME(ModelKeys.USERNAME_NAME, true),
   PASSWORD_NAME(ModelKeys.PASSWORD_NAME, true),
   SECURITY_DOMAIN_NAME(ModelKeys.SECURITY_DOMAIN_NAME, true),
   SHAREPREPAREDSTATEMENTS_NAME(ModelKeys.SHAREPREPAREDSTATEMENTS_NAME, true),
   PREPAREDSTATEMENTSCACHESIZE_NAME(ModelKeys.PREPAREDSTATEMENTSCACHESIZE_NAME, true),
   TRACKSTATEMENTS_NAME(ModelKeys.TRACKSTATEMENTS_NAME, true),
   VALID_CONNECTION_CHECKER_CLASSNAME_NAME(ModelKeys.VALID_CONNECTION_CHECKER_CLASSNAME_NAME, true),
   CHECKVALIDCONNECTIONSQL_NAME(ModelKeys.CHECKVALIDCONNECTIONSQL_NAME, true),
   VALIDATEONMATCH_NAME(ModelKeys.VALIDATEONMATCH_NAME, true),
   SPY_NAME(ModelKeys.SPY_NAME, true),
   USE_CCM_NAME(ModelKeys.USE_CCM_NAME, true),
   STALECONNECTIONCHECKERCLASSNAME_NAME(ModelKeys.STALECONNECTIONCHECKERCLASSNAME_NAME, true),
   EXCEPTIONSORTERCLASSNAME_NAME(ModelKeys.EXCEPTIONSORTERCLASSNAME_NAME, true),
   XADATASOURCEPROPERTIES_NAME(ModelKeys.XADATASOURCEPROPERTIES_NAME, true),
   XADATASOURCEPROPERTIES_VALUE_NAME(ModelKeys.XADATASOURCEPROPERTIES_VALUE_NAME, true),
   XADATASOURCECLASS_NAME(ModelKeys.XADATASOURCECLASS_NAME, true),
   INTERLEAVING_NAME(ModelKeys.INTERLEAVING_NAME, true),
   NOTXSEPARATEPOOL_NAME(ModelKeys.NOTXSEPARATEPOOL_NAME, true),
   PAD_XID_NAME(ModelKeys.PAD_XID_NAME, true),
   SAME_RM_OVERRIDE_NAME(ModelKeys.SAME_RM_OVERRIDE_NAME, true),
   WRAP_XA_RESOURCE_NAME(ModelKeys.WRAP_XA_RESOURCE_NAME, true),
   EXCEPTIONSORTER_PROPERTIES_NAME(ModelKeys.EXCEPTIONSORTER_PROPERTIES_NAME, true),
   STALECONNECTIONCHECKER_PROPERTIES_NAME(ModelKeys.STALECONNECTIONCHECKER_PROPERTIES_NAME, true),
   VALIDCONNECTIONCHECKER_PROPERTIES_NAME(ModelKeys.VALIDCONNECTIONCHECKER_PROPERTIES_NAME, true),
   REAUTHPLUGIN_CLASSNAME_NAME(ModelKeys.REAUTHPLUGIN_CLASSNAME_NAME, true),
   REAUTHPLUGIN_PROPERTIES_NAME(ModelKeys.REAUTHPLUGIN_PROPERTIES_NAME, true),
   RECOVERY_USERNAME_NAME(ModelKeys.RECOVERY_USERNAME_NAME, true),
   RECOVERY_PASSWORD_NAME(ModelKeys.RECOVERY_PASSWORD_NAME, true),
   RECOVERY_SECURITY_DOMAIN_NAME(ModelKeys.RECOVERY_SECURITY_DOMAIN_NAME, true),
   RECOVER_PLUGIN_CLASSNAME_NAME(ModelKeys.RECOVER_PLUGIN_CLASSNAME_NAME, true),
   RECOVER_PLUGIN_PROPERTIES_NAME(ModelKeys.RECOVER_PLUGIN_PROPERTIES_NAME, true),
   NO_RECOVERY_NAME(ModelKeys.NO_RECOVERY_NAME, true);

   private final String name;
   private final boolean deprecated;

   Element(String name) {
      this(name, false);
   }

   Element(String name, boolean deprecated) {
      this.name = name;
      this.deprecated = deprecated;
   }

   public String getLocalName() {
      return name;
   }

   public boolean isDeprecated(Namespace nameSpace) {
//      if (nameSpace.isLegacy())
      return deprecated;
   }

   private static final Map<String, Element> elements;

   static {
      final Map<String, Element> map = new HashMap<>();
      for (Element element : values()) {
         final String name = element.getLocalName();
         if (name != null) map.put(name, element);
      }
      elements = map;
   }

   public static Element forName(String localName) {
      final Element element = elements.get(localName);
      return element == null ? UNKNOWN : element;
   }
}
