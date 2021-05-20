/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.infinispan.logging.processor.report;

import java.io.BufferedWriter;
import java.io.IOException;

import javax.lang.model.element.Element;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.infinispan.logging.annotations.Description;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;

/**
 * XmlReportWriter.
 * @author Durgesh Anaokar
 * @since 5.3
 */
public class XmlReportWriter {
    private final XMLStreamWriter xmlWriter;

    public XmlReportWriter(final BufferedWriter writer) throws XMLStreamException {
        final XMLOutputFactory factory = XMLOutputFactory.newInstance();
        xmlWriter = new IndentingXmlWriter(factory.createXMLStreamWriter(writer));
    }

    public void writeHeader(final String title) throws IOException {
        try {
            xmlWriter.writeStartDocument();
            xmlWriter.writeStartElement("report");
            if (title != null) {
                xmlWriter.writeAttribute("class", title);
            }
            xmlWriter.writeStartElement("logs");
            /* xmlWriter.writeAttribute("interface", messageInterface.name()); */
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

    public void writeDetail(final Element element) throws IOException {
        try {
           Description description = element.getAnnotation(Description.class);
           Message message = element.getAnnotation(Message.class);
           LogMessage logMessage = element.getAnnotation(LogMessage.class);
            xmlWriter.writeStartElement("log");
            writeCharacters("id", String.valueOf(message.id()));
            writeCharacters("message", message.value());
            writeCharacters("description", description.value());
            writeCharacters("level", logMessage.level().name());
            xmlWriter.writeEndElement();
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

   private void writeCharacters(String elementName, String elementeValue) throws XMLStreamException {
      xmlWriter.writeStartElement(elementName);
      xmlWriter.writeCharacters(elementeValue);
      xmlWriter.writeEndElement();
   }

    public void writeFooter() throws IOException {
        try {
            xmlWriter.writeEndElement(); // end <logs/>
            xmlWriter.writeEndElement(); // end <report/>
            xmlWriter.writeEndDocument();
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }
    public void close() throws IOException {
        try {
            if (xmlWriter != null) xmlWriter.close();
        } catch (XMLStreamException ignore) {
        }
    }

}
