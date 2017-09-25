package org.infinispan.server.datasources.subsystem;

import org.jboss.as.controller.ModelVersion;

/**
 * @author Ryan Emerson
 * @since 9.2
 */
public enum Namespace {
   // must be first
   UNKNOWN(null, 0, 0),
   INFINISPAN_ENDPOINT_9_0("urn:jboss:domain:datasources", 4, 0),
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

   public String format(String format) {
      return String.format(format, major, minor);
   }

   public boolean since(Namespace schema) {
      return (this.major > schema.major) || ((this.major == schema.major) && (this.minor >= schema.minor));
   }

   public boolean isLegacy() {
      return domain.contains("jboss");
   }

   @Override
   public String toString() {
      return String.format(URN_PATTERN, domain, this.major, this.minor);
   }
}
