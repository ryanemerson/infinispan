package org.infinispan.server.datasources.subsystem;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.kohsuke.MetaInfServices;

/**
 * @author Ryan Emerson
 * @since 9.2
 */
@MetaInfServices
public class DataSourcesExtension implements Extension {
   static final String SUBSYSTEM_NAME = "datagrid-infinispan-datasources";
   private static final String RESOURCE_NAME = DataSourcesExtension.class.getPackage().getName() + ".LocalDescriptions";

   static StandardResourceDescriptionResolver getResourceDescriptionResolver(final String... keyPrefix) {
      StringBuilder prefix = new StringBuilder(SUBSYSTEM_NAME);
      for (String kp : keyPrefix) {
         prefix.append('.').append(kp);
      }
      return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME, DataSourcesExtension.class.getClassLoader(), true, false);
   }

   @Override
   public void initialize(ExtensionContext context) {
      boolean registerRuntimeOnly = context.isRuntimeOnlyRegistrationValid();
      SubsystemRegistration registration = context.registerSubsystem(SUBSYSTEM_NAME, DatasSourcesModelVersion.CURRENT.getVersion());

      registration.registerSubsystemModel(DataSourcesSubsystemRootDefinition.createInstance(registerRuntimeOnly));
//      registration.registerXMLElementWriter(new DatasourcesSubsystemWriter());

      if (registerRuntimeOnly)
         registration.registerDeploymentModel(DataSourcesSubsystemRootDefinition.createDeployedInstance());
   }

   @Override
   public void initializeParsers(ExtensionParsingContext context) {
      for (Namespace namespace: Namespace.values())
         context.setSubsystemXmlMapping(SUBSYSTEM_NAME, namespace.toString(), new DataSourcesSubsystemReader(namespace));
   }
}
