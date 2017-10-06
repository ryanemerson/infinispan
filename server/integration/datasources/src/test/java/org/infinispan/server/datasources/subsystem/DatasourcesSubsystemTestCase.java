package org.infinispan.server.datasources.subsystem;

import java.util.Arrays;
import java.util.Collection;

import org.infinispan.server.commons.subsystem.ClusteringSubsystemTest;
import org.jboss.as.controller.PathElement;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * @author Ryan Emerson
 * @since 9.2
 */
@RunWith(value = Parameterized.class)
public class DatasourcesSubsystemTestCase extends ClusteringSubsystemTest {

   public DatasourcesSubsystemTestCase(String xmlFile, int operations, String xsdPath, String[] templates) {
      super(DataSourcesExtension.SUBSYSTEM_NAME, operations, xsdPath, new DataSourcesExtension(), xmlFile, templates);
   }

   @Parameterized.Parameters
   public static Collection<Object[]> data() {
      Object[][] data = new Object[][] {
            { "wildfly-datasources-4.0.xml", 38, "schema/wildfly-datasources_4_0.xsd", new String[] { "subsystem-templates/infinispan-datasources.xml"} },
            { "datasources-9.2.xml", 38, "schema/jboss-infinispan-datasources_9_2.xsd", new String[] { "subsystem-templates/infinispan-datasources.xml"} },
      };
      return Arrays.asList(data);
   }

   @Override
   protected PathElement getSubsystemPath() {
      return DataSourcesSubsystemRootDefinition.PATH;
   }
}
