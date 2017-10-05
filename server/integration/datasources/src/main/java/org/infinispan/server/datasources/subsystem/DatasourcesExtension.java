package org.infinispan.server.datasources.subsystem;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.kohsuke.MetaInfServices;

/**
 * @author Ryan Emerson
 * @since 9.2
 */
@MetaInfServices
class DatasourcesExtension implements Extension {
   static final String SUBSYSTEM_NAME = "datagrid-infinispan-datasources";
   private static final String RESOURCE_NAME = DatasourcesExtension.class.getPackage().getName() + ".LocalDescriptions";

   static ResourceDescriptionResolver getResourceDescriptionResolver(String keyPrefix) {
      return new StandardResourceDescriptionResolver(keyPrefix, RESOURCE_NAME, DatasourcesExtension.class.getClassLoader(), true, true);
   }

   @Override
   public void initialize(ExtensionContext context) {
      SubsystemRegistration registration = context.registerSubsystem(SUBSYSTEM_NAME, DatasourcesModelVersion.CURRENT.getVersion());

      registration.registerSubsystemModel(new DatasourcesSubsystemRootResource());
//      TODO
//      registration.registerXMLElementWriter(new DatasourcesSubsystemWriter());
   }

   @Override
   public void initializeParsers(ExtensionParsingContext context) {
      for (Namespace namespace: Namespace.values())
         context.setSubsystemXmlMapping(SUBSYSTEM_NAME, namespace.toString(), new DatasourcesSubsystemReader(namespace));
   }
}
