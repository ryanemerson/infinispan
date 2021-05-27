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
import org.jboss.logging.annotations.MessageLogger;

/**
 * XmlReportWriter.
 * @author Durgesh Anaokar
 * @Since 13.0
 */
public class XmlReportWriter implements AutoCloseable {
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
         xmlWriter.writeComment("DescriptionDocumentation");
         xmlWriter.writeStartElement("logs");
      } catch (XMLStreamException e) {
         throw new IOException(e);
      }
   }

   public void writeDetail(final Element element) throws IOException {
      try {
         Description description = element.getAnnotation(Description.class);
         Message message = element.getAnnotation(Message.class);
         LogMessage logMessage = element.getAnnotation(LogMessage.class);
         MessageLogger messageLogger = element.getEnclosingElement().getAnnotation(MessageLogger.class);
         String strMsgId = String.valueOf(message.id());
         int padding = messageLogger.length() - strMsgId.length();
         StringBuilder prjCode = new StringBuilder(messageLogger.projectCode());
         for (int i = 0; i < padding; i++) {
            prjCode.append(0);
         }
         prjCode.append(strMsgId);
         xmlWriter.writeStartElement("log");

         writeCharacters("id", prjCode.toString());
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
         if (xmlWriter != null)
            xmlWriter.close();
      } catch (XMLStreamException e) {
         throw new IOException(e);
      }
   }

}
