package org.infinispan.server.datasources.subsystem;

import org.jboss.as.controller.ModelVersion;

/**
 * @author Ryan Emerson
 * @since 9.2
 */
enum DatasourcesModelVersion {

   VERSION_1_0_0(1, 0, 0);

   static final DatasourcesModelVersion CURRENT = VERSION_1_0_0;

   private final ModelVersion version;

   DatasourcesModelVersion(int major, int minor, int micro) {
      this.version = ModelVersion.create(major, minor, micro);
   }

   public ModelVersion getVersion() {
      return this.version;
   }
}
