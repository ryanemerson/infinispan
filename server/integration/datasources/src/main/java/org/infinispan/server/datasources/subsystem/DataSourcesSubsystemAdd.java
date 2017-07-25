package org.infinispan.server.datasources.subsystem;

import org.infinispan.server.datasources.services.driver.registry.DriverRegistryService;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 * @author Ryan Emerson
 * @since 9.2
 */
class DataSourcesSubsystemAdd extends AbstractBoottimeAddStepHandler {
   static final DataSourcesSubsystemAdd INSTANCE = new DataSourcesSubsystemAdd();

   @Override
   protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
      final DriverRegistryService driverRegistryService = new DriverRegistryService();
      context.getServiceTarget().addService(DriverRegistryService.SERVICE_NAME, driverRegistryService).install();
   }
}
