package org.infinispan.server.datasources.subsystem;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleResourceDefinition;

/**
 * @author Ryan Emerson
 * @since 9.2
 */
class DataSourceDefinition extends SimpleResourceDefinition {
   static final PathElement PATH_DATASOURCE = PathElement.pathElement(ModelKeys.DATA_SOURCE);
   static final DataSourceDefinition INSTANCE = new DataSourceDefinition();

   private DataSourceDefinition() {
      super(PATH_DATASOURCE,
            DataSourcesExtension.getResourceDescriptionResolver(ModelKeys.JDBC_DRIVER),
            DataSourceAdd.INSTANCE,
            ReloadRequiredRemoveStepHandler.INSTANCE);
   }
}
