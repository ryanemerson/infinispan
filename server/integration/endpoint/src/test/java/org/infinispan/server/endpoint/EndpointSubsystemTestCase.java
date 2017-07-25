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
package org.infinispan.server.endpoint;

import java.util.Arrays;
import java.util.Collection;

import org.infinispan.server.commons.subsystem.ClusteringSubsystemTest;
import org.infinispan.server.endpoint.subsystem.EndpointExtension;
import org.jboss.as.controller.PathElement;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;




/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
@RunWith(value = Parameterized.class)
public class EndpointSubsystemTestCase extends ClusteringSubsystemTest {

   public EndpointSubsystemTestCase(String xmlFile, int operations, String xsdPath, String[] templates) {
      super(Constants.SUBSYSTEM_NAME, operations, xsdPath, new EndpointExtension(), xmlFile, templates);
   }

   @Parameters
   public static Collection<Object[]> data() {
      Object[][] data = new Object[][] {
            { "endpoint-7.2.xml", 16, "schema/jboss-infinispan-endpoint_7_2.xsd", null },
            { "endpoint-8.0.xml", 16, "schema/jboss-infinispan-endpoint_8_0.xsd", null },
            { "endpoint-9.0.xml", 38, "schema/jboss-infinispan-endpoint_9_0.xsd", null },
            { "endpoint-9.2.xml", 38, "schema/jboss-infinispan-endpoint_9_2.xsd", new String[] { "/subsystem-templates/infinispan-endpoint.xml"} },
      };
      return Arrays.asList(data);
   }

   @Override
   protected PathElement getSubsystemPath() {
      return Constants.SUBSYSTEM_PATH;
   }
}
