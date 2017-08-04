package org.infinispan.server.datasources.subsystem;

import org.jboss.as.controller.ModelVersion;

/**
 * @author Ryan Emerson
 * @since 9.2
 */
public enum Namespace {
   // must be first
   UNKNOWN("", 0, 0),
   WILDFLY_DATASOURCES_4_0("jboss:domain:datasources", 4, 0),
   INFINISPAN_DATASOURCES_9_2("infinispan:server:datasources", 9, 2);
   private static final String URN_PATTERN = "urn:%s:%d.%d";

   public static final Namespace CURRENT = INFINISPAN_DATASOURCES_9_2;

   private final int major;
   private final int minor;
   private final String domain;
   private final ModelVersion version;

   Namespace(String domain, int major, int minor) {
      this.domain = domain;
      this.major = major;
      this.minor = minor;
      this.version = ModelVersion.create(major, minor);
   }

   public ModelVersion getVersion() {
      return this.version;
   }

   public boolean since(Namespace schema) {
      return (this.major > schema.major) || ((this.major == schema.major) && (this.minor >= schema.minor));
   }

   public boolean isLegacy() {
      return !since(INFINISPAN_DATASOURCES_9_2);
   }

   public static Namespace fromURI(String urn) {
      for (Namespace ns : values()) {
         if (ns.toString().equals(urn))
            return ns;
      }
      throw new IllegalArgumentException();
   }

   @Override
   public String toString() {
      return String.format(URN_PATTERN, domain, this.major, this.minor);
   }


}
