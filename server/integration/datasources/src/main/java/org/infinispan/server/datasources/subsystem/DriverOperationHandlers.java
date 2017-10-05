package org.infinispan.server.datasources.subsystem;

import static org.infinispan.server.datasources.subsystem.JdbcDriverResource.DEPLOYMENT_NAME;
import static org.infinispan.server.datasources.subsystem.JdbcDriverResource.DRIVER_CLASS;
import static org.infinispan.server.datasources.subsystem.JdbcDriverResource.MAJOR_VERSION;
import static org.infinispan.server.datasources.subsystem.JdbcDriverResource.MINOR_VERSION;
import static org.infinispan.server.datasources.subsystem.JdbcDriverResource.MODULE_NAME;
import static org.infinispan.server.datasources.subsystem.JdbcDriverResource.MODULE_SLOT;
import static org.infinispan.server.datasources.subsystem.JdbcDriverResource.NAME;
import static org.infinispan.server.datasources.subsystem.JdbcDriverResource.JDBC_COMPLIANT;

import org.infinispan.server.datasources.DatasourcesLogger;
import org.infinispan.server.datasources.services.driver.InstalledDriver;
import org.infinispan.server.datasources.services.driver.registry.DriverRegistry;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringBytesLengthValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * @author Ryan Emerson
 * @since 9.2
 */
public class DriverOperationHandlers {

   static final OperationStepHandler GET_DRIVER = (context, operation) -> {
      ParametersValidator validator = new ParametersValidator();
      validator.registerValidator(NAME.getName(), new StringBytesLengthValidator(1));
      validator.validate(operation);

      final String name = operation.require(NAME.getName()).asString();
      if (context.isNormalServer()) {
         context.addStep((ctx, op) -> {
            ServiceController<?> sc = context.getServiceRegistry(false).getRequiredService(JdbcDriverResource.JDBC_DRIVER_REGISTRY_SERVICE);
            DriverRegistry driverRegistry = DriverRegistry.class.cast(sc.getValue());
            InstalledDriver driver = driverRegistry.getInstalledDriver(name);
            context.getResult().set(createModelNode(driver));
         }, OperationContext.Stage.RUNTIME);
      }
   };

   static final OperationStepHandler DRIVER_LIST = (context, operation) -> {
      if (context.isNormalServer()) {
         ServiceController<?> sc = context.getServiceRegistry(false).getRequiredService(JdbcDriverResource.JDBC_DRIVER_REGISTRY_SERVICE);
         DriverRegistry driverRegistry = DriverRegistry.class.cast(sc.getValue());
         ModelNode result = context.getResult();
         driverRegistry.getInstalledDrivers().forEach(driver -> result.add(createModelNode(driver)));
      } else {
         context.getResult().set(DatasourcesLogger.ROOT_LOGGER.noMetricsAvailable());
      }
   };

   private static ModelNode createModelNode(InstalledDriver driver) {
      ModelNode driverNode = new ModelNode();
      driverNode.get(NAME.getName()).set(driver.getDriverName());
      if (driver.isFromDeployment()) {
         driverNode.get(DEPLOYMENT_NAME.getName()).set(driver.getDriverName());
         driverNode.get(MODULE_NAME.getName());
         driverNode.get(MODULE_SLOT.getName());
      } else {
         driverNode.get(DEPLOYMENT_NAME.getName());
         driverNode.get(MODULE_NAME.getName()).set(driver.getModuleName().getName());
         driverNode.get(MODULE_SLOT.getName()).set(driver.getModuleName() != null ? driver.getModuleName().getSlot() : "");
      }
      // TODO how to we handle all three class types?
      driverNode.get(DRIVER_CLASS.getName()).set(driver.getDriverClassName());
      driverNode.get(MAJOR_VERSION.getName()).set(driver.getMajorVersion());
      driverNode.get(MINOR_VERSION.getName()).set(driver.getMinorVersion());
      driverNode.get(JDBC_COMPLIANT.getName()).set(driver.isJdbcCompliant());
      return driverNode;
   }
}
