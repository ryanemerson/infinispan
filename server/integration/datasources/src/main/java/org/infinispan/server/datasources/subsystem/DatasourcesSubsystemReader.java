/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011-2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.infinispan.server.datasources.subsystem;

import static org.infinispan.server.datasources.DatasourcesLogger.ROOT_LOGGER;

import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * @author Ryan Emerson
 * @since 9.2
 */
class DatasourcesSubsystemReader implements XMLStreamConstants, XMLElementReader<List<ModelNode>> {
   private final Namespace namespace;

   DatasourcesSubsystemReader(Namespace namespace) {
      this.namespace = namespace;

      if (namespace.isLegacy())
         ROOT_LOGGER.deprecatedNamespace(namespace, Namespace.CURRENT);
   }

   @Override
   public void readElement(final XMLExtendedStreamReader reader, final List<ModelNode> operations)
         throws XMLStreamException {

      PathAddress subsystemAddress = PathAddress.pathAddress(DatasourcesSubsystemRootResource.PATH);
      ModelNode subsystem = Util.createAddOperation(subsystemAddress);

      operations.add(subsystem);

      while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
         Element element = Element.forName(reader.getLocalName());
//         if (element.isDeprecated()) {
            // TODO log that element no longer does anything
//            continue;
//         }

         switch (element) {
            case DATASOURCES: {
               parseDatasources(reader, subsystemAddress, operations);
               break;
            }
            default: {
               throw ParseUtils.unexpectedElement(reader);
            }
         }
      }
   }

   private void parseDatasources(XMLExtendedStreamReader reader, PathAddress subsystemAddress,
                                 List<ModelNode> operations) throws XMLStreamException {

   }
}
