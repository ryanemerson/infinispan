package org.infinispan.logging.processor.report;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.infinispan.logging.annotations.Description;
import org.jboss.logging.processor.apt.report.ReportType;
import org.jboss.logging.processor.apt.report.ReportWriter;
import org.jboss.logging.processor.model.MessageInterface;
import org.jboss.logging.processor.model.MessageMethod;

public class ISPNXmlReportWriter implements Closeable {
   private static final String NAMESPACE = "urn:jboss:logging:report:1.0";
   final MessageInterface messageInterface;
   private final XMLStreamWriter xmlWriter;
   ReportWriter reportWriter;

   public ISPNXmlReportWriter(MessageInterface messageInterface, BufferedWriter writer) throws XMLStreamException {
      this.messageInterface = messageInterface;
      final XMLOutputFactory factory = XMLOutputFactory.newInstance();
      this.xmlWriter = new IndentingXmlWriter(factory.createXMLStreamWriter(writer));
      this.reportWriter = ReportWriter.of(ReportType.XML, messageInterface, writer);
   }

   public void writeHeader(final String title) throws IOException {
      try {
         xmlWriter.writeStartDocument();
         xmlWriter.setDefaultNamespace(NAMESPACE);
         xmlWriter.writeStartElement("report");
         xmlWriter.writeNamespace(null, NAMESPACE);
         if (title != null) {
            xmlWriter.writeAttribute("title", title);
         }
         xmlWriter.writeStartElement("messages");
         xmlWriter.writeAttribute("interface", messageInterface.name());
      } catch (XMLStreamException e) {
         throw new IOException(e);
      }
   }

   public void writeDetail(final MessageMethod messageMethod)
         throws IOException, NoSuchMethodException, SecurityException, IllegalArgumentException, IllegalAccessException,
         NoSuchFieldException, InvocationTargetException {
      try {
         xmlWriter.writeStartElement("message");
         Class cls = reportWriter.getClass();
         final MessageMethod.Message msg = messageMethod.message();
         if (msg.hasId()) {
            final String id = String.format((String) cls.getField("messageIdFormat").get(cls), msg.id());
            xmlWriter.writeAttribute("id", id);
         }
         Description description = messageMethod.getAnnotation(Description.class);
         System.out.println("*****************************************"+description.value()+"*****************************************");
         xmlWriter.writeAttribute("description", description.value());
         Method declaredMethod = cls.getDeclaredMethod("getLogLevel", MessageMethod.class);
         if (messageMethod.isLoggerMethod()) {
            xmlWriter.writeAttribute("logLevel", (String) declaredMethod.invoke(cls, messageMethod));
         } else {
            xmlWriter.writeAttribute("returnType", messageMethod.returnType().name());
         }
         xmlWriter.writeCharacters(msg.value());
         xmlWriter.writeEndElement();
      } catch (XMLStreamException e) {
         throw new IOException(e);
      }
   }

   public void writeFooter() throws IOException {
      try {
         xmlWriter.writeEndElement(); // end <messages/>
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
