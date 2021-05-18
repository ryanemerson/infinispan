package org.infinispan.logging.processor;

import org.infinispan.logging.annotations.Description;
import org.jboss.logging.Logger.Level;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * Used for validating the XML for a {@code resolutionUrl} attribute.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "RPTL", length = 5)
public interface TestReportLogger {

    @LogMessage(level = Level.INFO)
    @Message(id = 1, value = "Test containsUrl")
    @Description(value = "This is test description for containsUrl.")
    void containsUrl();

    @LogMessage(level = Level.INFO)
    @Message("Test message")
    @Description(value = "This is test description for noUrl.")
    void noUrl();

    @LogMessage(level = Level.INFO)
    @Message(id = 2, value = "Test defaultResolutionUrl")
    @Description(value = "This is test description for defaultResolutionUrl.")
    void defaultResolutionUrl();
}
