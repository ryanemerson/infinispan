package org.infinispan.server.datasources.subsystem;

import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;

/**
 * @author Ryan Emerson
 * @since 9.2
 */
public enum Attribute {
   // must be first
   UNKNOWN(null),
   CONNECTABLE(ModelKeys.CONNECTABLE),
   ENABLED(ModelKeys.ENABLED),
   ENLISTMENT_TRACE(ModelKeys.ENLISTMENT_TRACE),
   JNDI_NAME(ModelKeys.JNDI_NAME),
   JTA(ModelKeys.JTA),
   MAJOR_VERSION(ModelKeys.DRIVER_MAJOR_VERSION),
   MCP(ModelKeys.MCP),
   MINOR_VERSION(ModelKeys.DRIVER_MINOR_VERSION),
   MODULE(ModelKeys.MODULE),
   NAME(ModelKeys.NAME),
   POOL_NAME(ModelKeys.POOL_NAME),
   SPY(ModelKeys.SPY),
   STATISTICS_ENABLED(ModelDescriptionConstants.STATISTICS_ENABLED),
   TRACKING(ModelKeys.TRACKING),
   USE_CCM(ModelKeys.USE_CCM),
   USE_JAVA_CONTEXT(ModelKeys.USE_JAVA_CONTEXT),;

   private final String name;

   Attribute(final String name) {
      this.name = name;
   }

   private static final Map<String, Attribute> attributes;

   static {
      final Map<String, Attribute> map = new HashMap<>();
      for (Attribute attribute : values()) {
         final String name = attribute.toString();
         if (name != null) map.put(name, attribute);
      }
      attributes = map;
   }

   public static Attribute forName(String localName) {
      final Attribute attribute = attributes.get(localName);
      return attribute == null ? UNKNOWN : attribute;
   }

   @Override
   public String toString() {
      return name;
   }
}
