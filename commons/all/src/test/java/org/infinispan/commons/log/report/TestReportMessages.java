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

import org.infinispan.commons.util.logging.annotations.Description;
import org.jboss.logging.annotations.BaseUrl;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

/**
 * Used for validating the XML for a {@code resolutionUrl} attribute.
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@BaseUrl("http://jboss.org/")
@MessageBundle(projectCode = "RPTM")
public interface TestReportMessages {

    @Message(id = 1, value = "Test message")
    @Description(value = "This is test description for defaultUrl.")
    RuntimeException defaultUrl();

    @Message("Test message")
    @Description(value = "This is test description for noUrl.")
    RuntimeException noUrl();

    @Message(id = 2, value = "Test message")
    @Description(value = "This is test description for containsUrl.")
    RuntimeException containsUrl();
}
