package org.infinispan.server.datasources.subsystem;

import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceName;

/**
 * @author Ryan Emerson
 * @since 9.2
 */
public class JdbcDriverResource extends SimpleResourceDefinition {

   static final ServiceName JDBC_DRIVER_REGISTRY_SERVICE = ServiceName.JBOSS.append("jdbc-driver", "registry");

   static final SimpleAttributeDefinition DEPLOYMENT_NAME = SimpleAttributeDefinitionBuilder.create("deployment-name", ModelType.STRING)
         .setAllowExpression(true)
         .setAllowNull(true)
         .build();

   static final SimpleAttributeDefinition NAME = new SimpleAttributeDefinitionBuilder(ModelKeys.NAME, ModelType.STRING)
         .setAllowNull(false)
         .build();

   static final SimpleAttributeDefinition MAJOR_VERSION = new SimpleAttributeDefinitionBuilder(ModelKeys.MAJOR_VERSION, ModelType.INT)
         .setAllowNull(true)
         .setAllowExpression(true)
         .build();

   static final SimpleAttributeDefinition MINOR_VERSION = new SimpleAttributeDefinitionBuilder(ModelKeys.MINOR_VERSION, ModelType.INT)
         .setAllowNull(true)
         .setAllowExpression(true)
         .build();

   static final SimpleAttributeDefinition MODULE_NAME = new SimpleAttributeDefinitionBuilder(ModelKeys.MODULE, ModelType.STRING)
         .setAllowExpression(true)
         .build();


   static final SimpleAttributeDefinition MODULE_SLOT = new SimpleAttributeDefinitionBuilder(ModelKeys.MODULE_SLOT, ModelType.STRING)
         .setAllowExpression(true)
         .setAllowNull(true)
         .build();

   static final SimpleAttributeDefinition DRIVER_CLASS = new SimpleAttributeDefinitionBuilder(ModelKeys.DRIVER_CLASS, ModelType.STRING)
         .setAllowNull(true)
         .setAllowExpression(true)
         .build();

   static final SimpleAttributeDefinition DATASOURCE_CLASS = new SimpleAttributeDefinitionBuilder(ModelKeys.DATASOURCE_CLASS, ModelType.STRING)
         .setAllowNull(true)
         .setAllowExpression(true)
         .build();

   static final SimpleAttributeDefinition XA_DATASOURCE_CLASS = new SimpleAttributeDefinitionBuilder(ModelKeys.XA_DATASOURCE_CLASS, ModelType.STRING)
         .setAllowNull(true)
         .setAllowExpression(true)
         .build();


   static final SimpleAttributeDefinition JDBC_COMPLIANT = SimpleAttributeDefinitionBuilder.create(ModelKeys.JDBC_COMPLIANT, ModelType.BOOLEAN)
         .setAllowNull(true)
         .setAllowExpression(true)
         .build();

   static final SimpleAttributeDefinition[] JDBC_DRIVER_ATTRIBUTES = {
         NAME,
         DRIVER_CLASS,
         DATASOURCE_CLASS,
         MAJOR_VERSION,
         MODULE_NAME,
         MODULE_SLOT,
         MINOR_VERSION,
         XA_DATASOURCE_CLASS,
         JDBC_COMPLIANT
   };

   public JdbcDriverResource(PathElement pathElement, ResourceDescriptionResolver descriptionResolver, OperationStepHandler addHandler, OperationStepHandler removeHandler) {
      super(pathElement, descriptionResolver, addHandler, removeHandler);
   }
}
