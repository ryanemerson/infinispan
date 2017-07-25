package org.infinispan.server.datasources.subsystem;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

class DataSourcesSubsystemRootDefinition extends SimpleResourceDefinition {
   static final PathElement PATH = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, DataSourcesExtension.SUBSYSTEM_NAME);

   private final boolean registerRuntimeOnly;
   private final boolean deployed;

   private DataSourcesSubsystemRootDefinition(boolean registerRuntimeOnly, boolean deployed) {
      super(PATH, DataSourcesExtension.getResourceDescriptionResolver(DataSourcesExtension.SUBSYSTEM_NAME),
            deployed ? null : DataSourcesSubsystemAdd.INSTANCE,
            deployed ? null : ReloadRequiredRemoveStepHandler.INSTANCE);
      this.registerRuntimeOnly = registerRuntimeOnly;
      this.deployed = deployed;
   }

   static DataSourcesSubsystemRootDefinition createInstance(boolean runtimeRegistration) {
      return new DataSourcesSubsystemRootDefinition(runtimeRegistration, false);
   }

   static DataSourcesSubsystemRootDefinition createDeployedInstance() {
      return new DataSourcesSubsystemRootDefinition(true, true);
   }

   @Override
   public void registerOperations(ManagementResourceRegistration resourceRegistration) {
      super.registerOperations(resourceRegistration);
      resourceRegistration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);
      if (registerRuntimeOnly && !deployed) {
         resourceRegistration.registerOperationHandler(JdbcDriverResource.INSTALLED_DRIVERS_LIST, JdbcDriverOperationHandlers.DRIVER_LIST);
         resourceRegistration.registerOperationHandler(JdbcDriverResource.GET_INSTALLED_DRIVER, JdbcDriverOperationHandlers.GET_DRIVER);
      }
   }

   @Override
   public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
      if (registerRuntimeOnly && !deployed)
         resourceRegistration.registerReadOnlyAttribute(JdbcDriverResource.INSTALLED_DRIVERS, JdbcDriverOperationHandlers.DRIVER_LIST);
   }

   @Override
   public void registerChildren(ManagementResourceRegistration resourceRegistration) {
      if (!deployed)
         resourceRegistration.registerSubModel(JdbcDriverDefinition.INSTANCE);
      resourceRegistration.registerSubModel(DataSourceDefinition.INSTANCE);
   }
}
