package org.infinispan.marshaller.kryo.logging;

import static org.jboss.logging.Logger.Level.WARN;

import org.infinispan.commons.CacheConfigurationException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * Log abstraction for the Kryo Marshaller bridge. For this module, message ids
 * ranging from 28001 to 29000 inclusively have been reserved.
 *
 * @author Ryan Emerson
 * @since 9.0
 */
@MessageLogger(projectCode = "ISPN")
public interface Log extends BasicLogger {

//   @Message(value = "Name of the failover cluster needs to be specified", id = 1)
//   CacheConfigurationException missingClusterNameDefinition();
//
//   @LogMessage(level = WARN)
//   @Message(value = "Unable to convert property [%s] to an enum! Using default value of %d", id = 2)
//   void unableToConvertStringPropertyToEnum(String value, String defaultValue);
}
