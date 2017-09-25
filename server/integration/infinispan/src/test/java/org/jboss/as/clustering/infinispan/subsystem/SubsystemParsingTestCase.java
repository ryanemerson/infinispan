/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
* as indicated by the @author tags. See the copyright.txt file in the
* distribution for a full listing of individual contributors.
*
* This is free software; you can redistribute it and/or modify it
* under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2.1 of
* the License, or (at your option) any later version.
*
* This software is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;

import org.infinispan.server.commons.subsystem.ClusteringSubsystemTest;
import org.infinispan.server.jgroups.subsystem.JGroupsSubsystemResourceDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.dmr.ModelNode;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests parsing / booting / marshalling of Infinispan configurations.
 *
 * The current XML configuration is tested, along with supported legacy configurations.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author Richard Achmatowicz (c) 2013 Red Hat Inc.
 */
@RunWith(value = Parameterized.class)
public class SubsystemParsingTestCase extends ClusteringSubsystemTest {

    public SubsystemParsingTestCase(Namespace schema, int operations, String xsdPath, String[] templates) {
        super(InfinispanExtension.SUBSYSTEM_NAME, operations, xsdPath, new InfinispanExtension(), schema.format("subsystem-infinispan_%d_%d.xml"), templates);
    }

    @Parameters
    public static Collection<Object[]> data() {
      Object[][] data = new Object[][] {
                                         { Namespace.INFINISPAN_SERVER_6_0, 106, "schema/jboss-infinispan-core_6_0.xsd", null },
                                         { Namespace.INFINISPAN_SERVER_7_0, 129, "schema/jboss-infinispan-core_7_0.xsd", null },
                                         { Namespace.INFINISPAN_SERVER_7_1, 129, "schema/jboss-infinispan-core_7_1.xsd", null },
                                         { Namespace.INFINISPAN_SERVER_7_2, 129, "schema/jboss-infinispan-core_7_2.xsd", null },
                                         { Namespace.INFINISPAN_SERVER_8_0, 141, "schema/jboss-infinispan-core_8_0.xsd", null },
                                         { Namespace.INFINISPAN_SERVER_8_1, 142, "schema/jboss-infinispan-core_8_1.xsd", null },
                                         { Namespace.INFINISPAN_SERVER_8_2, 142, "schema/jboss-infinispan-core_8_2.xsd", null },
                                         { Namespace.INFINISPAN_SERVER_9_0, 142, "schema/jboss-infinispan-core_9_0.xsd", null },
                                         { Namespace.INFINISPAN_SERVER_9_1, 144, "schema/jboss-infinispan-core_9_1.xsd", new String[] { "/subsystem-templates/infinispan-core.xml" }},
                                         { Namespace.INFINISPAN_SERVER_9_2, 144, "schema/jboss-infinispan-core_9_2.xsd", new String[] { "/subsystem-templates/infinispan-core.xml" }},
      };
      return Arrays.asList(data);
    }

    @Override
    protected PathElement getSubsystemPath() {
        return InfinispanSubsystemRootResource.PATH;
    }

    @Override
    protected Properties getResolvedProperties() {
        Properties properties = new Properties();
        properties.setProperty("java.io.tmpdir", System.getProperty("java.io.tmpdir"));

        return properties;
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return new org.infinispan.server.jgroups.subsystem.JGroupsSubsystemInitialization();
    }

    @Override
    protected void compare(ModelNode model1, ModelNode model2) {
        purgeJGroupsModel(model1);
        purgeJGroupsModel(model2);
        super.compare(model1, model2);
    }

    private static void purgeJGroupsModel(ModelNode model) {
        model.get(JGroupsSubsystemResourceDefinition.PATH.getKey()).remove(JGroupsSubsystemResourceDefinition.PATH.getValue());
    }
}
