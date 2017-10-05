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
import static org.infinispan.server.datasources.subsystem.JdbcDriverResource.MODULE_NAME;
import static org.infinispan.server.datasources.subsystem.JdbcDriverResource.MODULE_SLOT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.infinispan.commons.util.ByRef;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
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
   }

   @Override
   public void readElement(final XMLExtendedStreamReader reader, final List<ModelNode> operations)
         throws XMLStreamException {

      PathAddress subsystemAddress = PathAddress.pathAddress(DatasourcesSubsystemRootResource.PATH);
      ModelNode subsystem = Util.createAddOperation(subsystemAddress);
      operations.add(subsystem);

      while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
         Element element = Element.forName(reader.getLocalName());
         switch (element) {
            case DATASOURCES:
               parseDatasources(reader, subsystemAddress, operations);
               break;
            default:
               throw ParseUtils.unexpectedElement(reader);
            }
      }
   }

   private void parseDatasources(XMLExtendedStreamReader reader, PathAddress subsystemAddress,
                                 List<ModelNode> operations) throws XMLStreamException {

      while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
         Element element = Element.forName(reader.getLocalName());
         switch (element) {
            case XA_DATASOURCE:
               throw namespace.isLegacy() ?
                     ROOT_LOGGER.removedElement(Element.XA_DATASOURCE, Element.DATA_SOURCE) :
                     ParseUtils.unexpectedElement(reader);
            case DATASOURCE:
               parseDatasource(reader, subsystemAddress, operations);
               break;
            case DRIVERS:
               parseDrivers(reader, subsystemAddress, operations);
               break;
            default:
               throw ParseUtils.unexpectedElement(reader);
         }
      }
   }

   private void parseDatasource(XMLExtendedStreamReader reader, PathAddress subsystemAddress,
                                List<ModelNode> operations) throws XMLStreamException {
      String poolName = null;
      ModelNode datasource = Util.getEmptyOperation(ADD, null);

      // TODO provide a way for HikariCP properties file to be read

      for (int i = 0; i < reader.getAttributeCount(); i++) {
         ParseUtils.requireNoNamespaceAttribute(reader, i);
         String value = reader.getAttributeValue(i);
         Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
         switch (attribute) {
            case JNDI_NAME:
               DatasourceResource.JNDI_NAME.parseAndSetParameter(value, datasource, reader);
               break;
            case POOL_NAME:
               poolName = value;
               break;
            case STATISTICS_ENABLED:
               DatasourceResource.STATISTICS_ENABLED.parseAndSetParameter(value, datasource, reader);
               break;
            case USE_JAVA_CONTEXT:
               if (value != null)
                  DatasourceResource.USE_JAVA_CONTEXT.parseAndSetParameter(value, datasource, reader);
               break;
            // Ignored deprecated attributes
            case CONNECTABLE:
            case ENABLED:
            case ENLISTMENT_TRACE:
            case JTA:
            case MCP:
            case SPY:
            case TRACKING:
            case USE_CCM:
               if (namespace.isLegacy()) {
                  ROOT_LOGGER.ignoringDeprecatedAttribute(attribute);
                  break;
               }
            default:
               throw ParseUtils.unexpectedAttribute(reader, i);
         }
      }


      while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
         Element element = Element.forName(reader.getLocalName());
         String value = element != Element.SECURITY ? reader.getElementText() : null;
         switch (element) {
            case CONNECTION_PROPERTY:
               // TODO
            case CONNECTION_URL:
               DatasourceResource.CONNECTION_URL.parseAndSetParameter(value, datasource, reader);
               break;
            case DRIVER_CLASS:
               DatasourceResource.DRIVER.parseAndSetParameter(value, datasource, reader);
               break;
            case DATASOURCE_CLASS:
               JdbcDriverResource.DATASOURCE_CLASS.parseAndSetParameter(value, datasource, reader);
               break;
            case DRIVER:
               DatasourceResource.DRIVER.parseAndSetParameter(value, datasource, reader);
               break;
            case POOL:
               // TODO parse these as HikariProperties
               // parsePool(...)
               break;
            case SECURITY:
               parseDsSecurity(reader, datasource);
               break;

            // TODO parse these as HikariProperties
            case NEW_CONNECTION_SQL:
            case TRANSACTION_ISOLATION:
               break;

            case STATEMENT:
            case TIMEOUT:
            case URL_DELIMITER:
            case URL_PROPERTY:
            case URL_SELECTOR_STRATEGY_CLASS_NAME:
            case VALIDATION:
               if (namespace.isLegacy()) {
                  ROOT_LOGGER.ignoringDeprecatedElement(element);
                  break;
               }
            default:
               throw ParseUtils.unexpectedElement(reader);
         }
      }

      PathAddress datasourceAddress = subsystemAddress.append(PathElement.pathElement(ModelKeys.DATA_SOURCE, poolName));
      datasource.get(OP_ADDR).set(datasourceAddress.toModelNode());
      operations.add(datasource);
   }

   private void parseDsSecurity(XMLExtendedStreamReader reader, ModelNode operation) throws XMLStreamException {
      while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
         Element element = Element.forName(reader.getLocalName());
         String value = reader.getElementText();
         switch (element) {
            case USERNAME:
               DatasourceResource.USERNAME.parseAndSetParameter(value, operation, reader);
               break;
            case PASSWORD:
               DatasourceResource.PASSWORD.parseAndSetParameter(value, operation, reader);
               break;
            case SECURITY_DOMAIN:
            case REAUTH_PLUGIN:
               if (namespace.isLegacy()) {
                  ROOT_LOGGER.ignoringDeprecatedElement(element);
                  break;
               }
            default:
               throw ParseUtils.unexpectedElement(reader);
         }
      }
   }

   private void parseDrivers(XMLExtendedStreamReader reader, PathAddress subsystemAddress,
                             List<ModelNode> operations) throws XMLStreamException {
      while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
         Element element = Element.forName(reader.getLocalName());
         switch (element) {
            case DRIVER:
               parseDriver(reader, subsystemAddress, operations);
               break;
            default:
               throw ParseUtils.unexpectedElement(reader);
         }
      }
   }

   private void parseDriver(XMLExtendedStreamReader reader, PathAddress subsystemAddress,
                            List<ModelNode> operations) throws XMLStreamException {

      String driverName = null;
      ModelNode driver = Util.getEmptyOperation(ADD, null);
      for (int i = 0; i < reader.getAttributeCount(); i++) {
         ParseUtils.requireNoNamespaceAttribute(reader, i);
         String value = reader.getAttributeValue(i);
         Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
         switch (attribute) {
            case NAME:
               driverName = value;
               JdbcDriverResource.NAME.parseAndSetParameter(value, driver, reader);
               break;
            case MAJOR_VERSION:
               JdbcDriverResource.MAJOR_VERSION.parseAndSetParameter(value, driver, reader);
               break;
            case MINOR_VERSION:
               JdbcDriverResource.MINOR_VERSION.parseAndSetParameter(value, driver, reader);
               break;
            case MODULE:
               String moduleName = value;
               if (moduleName.contains(":")) {
                  String slot = moduleName.substring(moduleName.indexOf(":") + 1);
                  moduleName = moduleName.substring(0, moduleName.indexOf(":"));
                  if (!slot.isEmpty())
                     MODULE_SLOT.parseAndSetParameter(slot, driver, reader);
               }
               MODULE_NAME.parseAndSetParameter(moduleName, driver, reader);
               break;
            default:
               throw ParseUtils.unexpectedAttribute(reader, i);
         }
      }

      ByRef<Boolean> classSpecified = new ByRef<>(false);
      while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
         Element element = Element.forName(reader.getLocalName());
         String value = reader.getElementText();
         switch (element) {
            case DRIVER_CLASS:
               addDatasourceClass(JdbcDriverResource.DRIVER_CLASS, classSpecified, value, driver, reader);
               break;
            case DATASOURCE_CLASS:
               addDatasourceClass(JdbcDriverResource.DATASOURCE_CLASS, classSpecified, value, driver, reader);
               break;
            case XA_DATASOURCE_CLASS:
               throw namespace.isLegacy() ?
                     ROOT_LOGGER.removedElement(Element.XA_DATASOURCE_CLASS, Element.DATASOURCE_CLASS) :
                     ParseUtils.unexpectedElement(reader);
            default:
               throw ParseUtils.unexpectedElement(reader);
         }
      }

      PathAddress driverAddress = subsystemAddress.append(PathElement.pathElement(ModelKeys.JDBC_DRIVER, driverName));
      driver.get(OP_ADDR).set(driverAddress.toModelNode());
      operations.add(driver);
   }

   private void addDatasourceClass(SimpleAttributeDefinition definition, ByRef<Boolean> classExists, String value,
                                   ModelNode op, XMLExtendedStreamReader reader) throws XMLStreamException {
      if (classExists.get())
         throw ParseUtils.unexpectedElement(reader);
      definition.parseAndSetParameter(value, op, reader);
      classExists.set(true);
   }
}
