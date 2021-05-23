package org.infinispan.logging.processor;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;

import org.infinispan.logging.processor.report.XmlReportWriter;
import org.jboss.logging.processor.apt.ProcessingException;
import org.kohsuke.MetaInfServices;
/**
 * InfinispanLoggingProcessor.
 * @author Durgesh Anaokar
 * @Since 13.0
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes({InfinispanLoggingProcessor.DESCRIPTION})
@MetaInfServices(Processor.class)
public class InfinispanLoggingProcessor extends AbstractProcessor {
   static final String DESCRIPTION = "org.infinispan.logging.annotations.Description";
   static final String MESSAGE = "org.jboss.logging.annotations.Message";
   static final String LOGMESSAGE = "org.jboss.logging.annotations.LogMessage";

   @Override
   public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
      for (TypeElement annotation : annotations) {
         try {
            createReportXml(roundEnv, annotation);
         } catch (Throwable t) {
            uncaughtException(t);
            t.printStackTrace();
            return false;
         }
      }
      return true;
   }

   private void createReportXml(RoundEnvironment roundEnv, TypeElement annotation) throws IOException {
      try {
         Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(annotation);
         Map<String, List<Element>> mapWithQualifiedName = getMapWithQualifiedName(annotatedElements);
         for (Map.Entry<String, List<Element>> entry : mapWithQualifiedName.entrySet()) {

            String qualifiedName = entry.getKey();
            final int lastDot = qualifiedName.lastIndexOf(".");
            String packageName = qualifiedName.substring(0, lastDot);
            String simpleName = qualifiedName.substring(lastDot + 1);
            BufferedWriter bufferedWriter = createWriter(packageName, simpleName + ".xml");
            XmlReportWriter reportWriter = new XmlReportWriter(bufferedWriter);
            reportWriter.writeHeader(qualifiedName);
            List<Element> list = entry.getValue();
            for (Element element : list) {
               try {
                  reportWriter.writeDetail(element);
               } catch (ProcessingException e) {
                  final AnnotationMirror a = e.getAnnotation();
                  final AnnotationValue value = e.getAnnotationValue();
                  error(element, "Error while processing the report: for annotation" + a.toString() + " for value: "
                        + value.toString(), e);
               }
            }
            reportWriter.writeFooter();
            reportWriter.close();
            bufferedWriter.close();
         }
      } catch (Exception e) {
         error(null, "Error while processing the xml file.", e);
      }
   }

   private BufferedWriter createWriter(final String packageName, final String fileName) throws IOException {
      return new BufferedWriter(
            processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, packageName, fileName).openWriter());
   }

   private void uncaughtException(Throwable t) {
      String stackTrace = Stream.of(t.getStackTrace()).map(Object::toString)
            .collect(Collectors.joining("\n\tat ", "\tat ", ""));
      error(null, "InfinispanLoggingProcessor unexpected error: %s\n%s", t, stackTrace);
   }

   private void error(Element e, String format, Object... params) {
      log(Diagnostic.Kind.ERROR, e, format, params);
   }

   private void log(Diagnostic.Kind level, Element e, String format, Object... params) {
      String formatted = String.format(format, params);
      if (e != null) {
         processingEnv.getMessager().printMessage(level, formatted, e);
      } else {
         processingEnv.getMessager().printMessage(level, formatted);
      }
   }

   private Map<String, List<Element>> getMapWithQualifiedName(Set<? extends Element> annotatedElements) {
      Map<String, List<Element>> elementMap = new HashMap<String, List<Element>>();
      for (Element interfaceElement : annotatedElements) {
         List<Element> list = elementMap.get(interfaceElement.getEnclosingElement().toString());
         if (list != null) {
            list.add(interfaceElement);
         } else {
            list = new LinkedList<Element>();
            list.add(interfaceElement);
            elementMap.put(interfaceElement.getEnclosingElement().toString(), list);
         }
      }
      return elementMap;
   }
}
