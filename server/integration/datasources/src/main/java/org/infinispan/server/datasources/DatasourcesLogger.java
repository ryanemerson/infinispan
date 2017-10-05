package org.infinispan.server.datasources;

import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

import java.sql.Driver;

import javax.xml.stream.XMLStreamException;

import org.infinispan.server.datasources.subsystem.Attribute;
import org.infinispan.server.datasources.subsystem.Element;
import org.infinispan.server.datasources.subsystem.Namespace;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * @author Ryan Emerson
 * @since 9.2
 */
@MessageLogger(projectCode = "DGDATA", length = 4)
public interface DatasourcesLogger extends BasicLogger {

   String ROOT_LOGGER_CATEGORY = DatasourcesLogger.class.getPackage().getName();
   DatasourcesLogger ROOT_LOGGER = Logger.getMessageLogger(DatasourcesLogger.class, ROOT_LOGGER_CATEGORY);

   @Message(id = 1, value = "%s is null")
   String nullVar(String name);

   @Message(id = 2, value = "Service not started")
   IllegalStateException serviceNotStarted();

   @LogMessage(level = INFO)
   @Message(id = 3, value = "Started Driver service with driver-name = %s")
   void startedDriverService(String driverName);

   @LogMessage(level = INFO)
   @Message(id = 4, value = "Stopped Driver service with driver-name = %s")
   void stoppedDriverService(String driverName);

   @LogMessage(level = WARN)
   @Message(id = 5, value = "Domain '%s' has been deprecated, please migrate to '%s'")
   void deprecatedNamespace(Namespace deprecated, Namespace current);

   @Message(id = 6, value = "the attribute driver-name (%s) cannot be different from driver resource name (%s)")
   OperationFailedException driverNameAndResourceNameNotEquals(String driverName, String resourceName);

   @Message(id = 7, value = "Failed to load module for driver [%s]")
   String failedToLoadModuleDriver(String moduleName);

   @Message(id = 8, value = "Specified driver version doesn't match with actual driver version")
   IllegalStateException driverVersionMismatch();

   @LogMessage(level = WARN)
   @Message(id = 9, value = "Unable to find driver class name in \"%s\" jar")
   void cannotFindDriverClassName(String driverName);

   @LogMessage(level = WARN)
   @Message(id = 10, value = "Unable to instantiate driver class \"%s\": %s")
   void cannotInstantiateDriverClass(String driverClassName, Throwable reason);

   @Message(id = 11, value = "Unable to instantiate driver class \"%s\". See log (WARN) for more details")
   String cannotInstantiateDriverClass(String driverClassName);

   @LogMessage(level = INFO)
   @Message(id = 12, value = "Deploying JDBC-compliant driver %s (version %d.%d)")
   void deployingCompliantJdbcDriver(Class<? extends Driver> driver, int majorVersion, int minorVersion);

   @LogMessage(level = INFO)
   @Message(id = 13, value = "Deploying non-JDBC-compliant driver %s (version %d.%d)")
   void deployingNonCompliantJdbcDriver(Class<? extends Driver> driver, int majorVersion, int minorVersion);

   @Message(id = 14, value = "no metrics available")
   String noMetricsAvailable();

   @Message(id = 15, value = "'%s' element has been removed, please use '%s' instead")
   XMLStreamException removedElement(Element deprecated, Element current);

   @Message(id = 16, value = "Jndi name is required")
   OperationFailedException jndiNameRequired();

   @Message(id = 17, value = "Jndi name have to start with java:/ or java:jboss/")
   OperationFailedException jndiNameInvalidFormat();

   @Message(id = 18, value = "Jndi name shouldn't include '//' or end with '/'")
   OperationFailedException jndiNameShouldValidate();

   @LogMessage(level = WARN)
   @Message(id = 19, value = "Ignoring deprecated attribute '%s'")
   void ignoringDeprecatedAttribute(Attribute attribute);

   @LogMessage(level = WARN)
   @Message(id = 20, value = "Ignoring deprecated element '%s'")
   void ignoringDeprecatedElement(Element attribute);
}
