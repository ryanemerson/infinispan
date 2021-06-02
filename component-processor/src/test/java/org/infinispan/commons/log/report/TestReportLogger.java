/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.infinispan.commons.log.report;

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
