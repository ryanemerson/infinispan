package org.infinispan.server.datasources.subsystem;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.dmr.ModelNode;

/**
 * @author Ryan Emerson
 * @since 9.2
 */
class DataSourcesSubsystemAdd extends AbstractBoottimeAddStepHandler {
   static final DataSourcesSubsystemAdd INSTANCE = new DataSourcesSubsystemAdd();

   @Override
   public void populateModel(ModelNode operation, ModelNode model) {
      model.setEmptyObject();
   }
}
