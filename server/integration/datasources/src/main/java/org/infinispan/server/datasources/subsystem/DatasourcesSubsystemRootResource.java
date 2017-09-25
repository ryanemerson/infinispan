package org.infinispan.server.datasources.subsystem;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

class DatasourcesSubsystemRootResource extends SimpleResourceDefinition {
   static final PathElement PATH = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, DatasourcesExtension.SUBSYSTEM_NAME);

   DatasourcesSubsystemRootResource() {
      super(PATH, DatasourcesExtension.getResourceDescriptionResolver(DatasourcesExtension.SUBSYSTEM_NAME),
            null, ReloadRequiredRemoveStepHandler.INSTANCE);
   }

   @Override
   public void registerOperations(ManagementResourceRegistration resourceRegistration) {
      super.registerOperations(resourceRegistration);
   }


   @Override
   public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
      super.registerAttributes(resourceRegistration);
   }
}
