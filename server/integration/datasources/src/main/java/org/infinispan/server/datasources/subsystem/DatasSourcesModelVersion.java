package org.infinispan.server.datasources.subsystem;

import org.jboss.as.controller.ModelVersion;

/**
 * @author Ryan Emerson
 * @since 9.2
 */
enum DatasSourcesModelVersion {

   VERSION_1_0(1, 0);

   static final DatasSourcesModelVersion CURRENT = VERSION_1_0;

   private final ModelVersion version;

   DatasSourcesModelVersion(int major, int minor) {
      this.version = ModelVersion.create(major, minor);
   }

   public ModelVersion getVersion() {
      return this.version;
   }
}
