package org.infinispan.server.datasources.subsystem;

import org.jboss.as.controller.ObjectListAttributeDefinition;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.NonResolvingResourceDescriptionResolver;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceName;

/**
 * @author Ryan Emerson
 * @since 9.2
 */
class JdbcDriverResource {

   static ServiceName createDriverServiceName(String driverName) {
      return ServiceName.JBOSS.append(ModelKeys.JDBC_DRIVER, driverName.replaceAll("\\.", "_"));
   }

   static final SimpleAttributeDefinition DEPLOYMENT_NAME = SimpleAttributeDefinitionBuilder.create("deployment-name", ModelType.STRING)
         .setAllowExpression(true)
         .build();

   static final SimpleAttributeDefinition NAME = new SimpleAttributeDefinitionBuilder(ModelKeys.NAME, ModelType.STRING, true)
         .build();

   static final SimpleAttributeDefinition MAJOR_VERSION = new SimpleAttributeDefinitionBuilder(ModelKeys.MAJOR_VERSION, ModelType.INT, true)
         .setAllowExpression(true)
         .build();

   static final SimpleAttributeDefinition MINOR_VERSION = new SimpleAttributeDefinitionBuilder(ModelKeys.MINOR_VERSION, ModelType.INT, true)
         .setAllowExpression(true)
         .build();

   static final SimpleAttributeDefinition MODULE_NAME = new SimpleAttributeDefinitionBuilder(ModelKeys.MODULE, ModelType.STRING)
         .setAllowExpression(true)
         .build();

   static final SimpleAttributeDefinition MODULE_SLOT = new SimpleAttributeDefinitionBuilder(ModelKeys.MODULE_SLOT, ModelType.STRING, true)
         .setAllowExpression(true)
         .build();

   static final SimpleAttributeDefinition DRIVER_CLASS = new SimpleAttributeDefinitionBuilder(ModelKeys.DRIVER_CLASS, ModelType.STRING, true)
         .setAllowExpression(true)
         .build();

   static final SimpleAttributeDefinition DATASOURCE_CLASS = new SimpleAttributeDefinitionBuilder(ModelKeys.DATASOURCE_CLASS, ModelType.STRING, true)
         .setAllowExpression(true)
         .build();

   static final SimpleAttributeDefinition JDBC_COMPLIANT = SimpleAttributeDefinitionBuilder.create(ModelKeys.JDBC_COMPLIANT, ModelType.BOOLEAN, true)
         .setAllowExpression(true)
         .build();

   static final SimpleAttributeDefinition[] JDBC_DRIVER_ATTRIBUTES = {
         DRIVER_CLASS,
         DATASOURCE_CLASS,
         JDBC_COMPLIANT,
         MAJOR_VERSION,
         MINOR_VERSION,
         MODULE_NAME,
         MODULE_SLOT,
         NAME
   };

   private static final ObjectTypeAttributeDefinition INSTALLED_DRIVER = ObjectTypeAttributeDefinition.Builder.of(ModelKeys.INSTALLED_DRIVER, JDBC_DRIVER_ATTRIBUTES).build();
   static final ObjectListAttributeDefinition INSTALLED_DRIVERS = ObjectListAttributeDefinition.Builder.of(ModelKeys.INSTALLED_DRIVERS, INSTALLED_DRIVER)
         .setResourceOnly().setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
         .build();

   static final SimpleOperationDefinition INSTALLED_DRIVERS_LIST = new SimpleOperationDefinitionBuilder(ModelKeys.INSTALLED_DRIVER_LIST, new NonResolvingResourceDescriptionResolver())
         .setRuntimeOnly()
         .setReplyType(ModelType.LIST)
         .setReplyParameters(JDBC_DRIVER_ATTRIBUTES)
         .build();

   static final SimpleOperationDefinition GET_INSTALLED_DRIVER = new SimpleOperationDefinitionBuilder(ModelKeys.GET_INSTALLED_DRIVER, DataSourcesExtension.getResourceDescriptionResolver())
         .setRuntimeOnly()
         .setParameters(NAME)
         .setReplyParameters(MINOR_VERSION, MAJOR_VERSION, DEPLOYMENT_NAME, NAME, JDBC_COMPLIANT, MODULE_SLOT, DRIVER_CLASS, MODULE_NAME)
         .setAttributeResolver(DataSourcesExtension.getResourceDescriptionResolver(ModelKeys.JDBC_DRIVER))
         .build();
}
