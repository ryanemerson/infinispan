package org.infinispan.server.datasources.subsystem;

import static org.infinispan.server.datasources.DatasourcesLogger.ROOT_LOGGER;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Ryan Emerson
 * @since 9.2
 */
class DatasourceResource {

   private static final SensitivityClassification DS_SECURITY =
         new SensitivityClassification(DatasourcesExtension.SUBSYSTEM_NAME, "data-source-security", false, true, true);
   private static final SensitiveTargetAccessConstraintDefinition DS_SECURITY_DEF = new SensitiveTargetAccessConstraintDefinition(DS_SECURITY);

   static SimpleAttributeDefinition CONNECTION_URL = new SimpleAttributeDefinitionBuilder(ModelKeys.CONNECTION_URL, ModelType.STRING, false)
         .setAllowExpression(true)
         .build();

   static SimpleAttributeDefinition DRIVER = new SimpleAttributeDefinitionBuilder(ModelKeys.DRIVER, ModelType.STRING, false)
         .setAllowExpression(true)
         .build();

   static SimpleAttributeDefinition JNDI_NAME = new SimpleAttributeDefinitionBuilder(ModelKeys.JNDI_NAME, ModelType.STRING, false)
         .setAllowExpression(true)
         .setValidator(new ParameterValidator() {
            @Override
            public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
               if (value.isDefined()) {
                  if (value.getType() != ModelType.EXPRESSION) {
                     String str = value.asString();
                     if (!str.startsWith("java:/") && !str.startsWith("java:jboss/")) {
                        throw ROOT_LOGGER.jndiNameInvalidFormat();
                     } else if (str.endsWith("/") || str.contains("//")) {
                        throw ROOT_LOGGER.jndiNameShouldValidate();
                     }
                  }
               } else {
                  throw ROOT_LOGGER.jndiNameRequired();
               }
            }

            @Override
            public void validateResolvedParameter(String parameterName, ModelNode value) throws OperationFailedException {
               validateParameter(parameterName, value.resolve());
            }
         })
         .build();

   public static SimpleAttributeDefinition PASSWORD = new SimpleAttributeDefinitionBuilder(ModelKeys.PASSWORD, ModelType.STRING)
         .setAllowExpression(true)
         .setAllowNull(true)
         .setRequires(ModelKeys.USERNAME)
         .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.CREDENTIAL)
         .addAccessConstraint(DS_SECURITY_DEF)
         .build();

   static SimpleAttributeDefinition STATISTICS_ENABLED = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.STATISTICS_ENABLED, ModelType.BOOLEAN)
         .setDefaultValue(new ModelNode(false))
         .setAllowNull(true)
         .setAllowExpression(true)
         .build();

   static SimpleAttributeDefinition USE_JAVA_CONTEXT = new SimpleAttributeDefinitionBuilder(ModelKeys.USE_JAVA_CONTEXT, ModelType.BOOLEAN, true)
         .setDefaultValue(new ModelNode(true))
         .setAllowExpression(true)
         .build();

   public static SimpleAttributeDefinition USERNAME = new SimpleAttributeDefinitionBuilder(ModelKeys.USERNAME, ModelType.STRING)
         .setAllowExpression(true)
         .setAllowNull(true)
         .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.CREDENTIAL)
         .addAccessConstraint(DS_SECURITY_DEF)
         .build();

}
