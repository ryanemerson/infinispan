package org.infinispan.server.datasources;

import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

import org.infinispan.server.datasources.subsystem.Namespace;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * @author Ryan Emerson
 * @since 9.2
 */
@MessageLogger(projectCode = "DGDDSRC", length = 4)
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
   @Message(id = 5, value = "Domain '%s' has been deprecated, plese migrate to '%s'")
   void deprecatedNamespace(Namespace deprecated, Namespace current);


}
