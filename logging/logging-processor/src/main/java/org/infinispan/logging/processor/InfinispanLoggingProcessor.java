package org.infinispan.logging.processor;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.StandardLocation;
import org.infinispan.logging.processor.report.XmlReportWriter;
import org.jboss.logging.Logger;
import org.jboss.logging.processor.apt.ProcessingException;
import org.kohsuke.MetaInfServices;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes({InfinispanLoggingProcessor.DESCRIPTION,
                           InfinispanLoggingProcessor.MESSAGE,
                           InfinispanLoggingProcessor.LOGMESSAGE})
@MetaInfServices(Processor.class)
public class InfinispanLoggingProcessor extends AbstractProcessor {
   private static final String EXTN_XML = ".xml";
   static final String DESCRIPTION = "org.infinispan.logging.annotations.Description";
   static final String MESSAGE = "org.jboss.logging.annotations.Message";
   static final String LOGMESSAGE = "org.jboss.logging.annotations.LogMessage";

   private static final Logger LOG = Logger.getLogger(InfinispanLoggingProcessor.class.getName());

   @Override
   public synchronized void init(ProcessingEnvironment processingEnv) {
      super.init(processingEnv);
   }

   @Override
   public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
      for (TypeElement annotation : annotations) {
         try {
            createReportXml(roundEnv, annotation);
         } catch (Throwable t) {
            LOG.error("Erro while creating the report.", t);
            return false;
         }
      }
      return true;
   }

   private void createReportXml(RoundEnvironment roundEnv, TypeElement annotation) throws IOException {
      String packageName = null;
      String simpleName = null;
      BufferedWriter bufferedWriter = null;
      XmlReportWriter reportWriter = null;
      try {
         if (DESCRIPTION.equals(annotation.toString())) {
            Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(annotation);
            Element element = annotatedElements.stream().findFirst().get();
            Element enclosingElement = element.getEnclosingElement();
            String qualifiedName = enclosingElement.toString();
            final int lastDot = qualifiedName.lastIndexOf(".");
            packageName = qualifiedName.substring(0, lastDot);
            simpleName = qualifiedName.substring(lastDot + 1);
            bufferedWriter = createWriter(packageName, simpleName + EXTN_XML);
            reportWriter = new XmlReportWriter(bufferedWriter);
            reportWriter.writeHeader(qualifiedName);
            for (Element interfaceElement : annotatedElements) {
               try {
                  reportWriter.writeDetail(interfaceElement);
               } catch (ProcessingException e) {
                  final AnnotationMirror a = e.getAnnotation();
                  final AnnotationValue value = e.getAnnotationValue();
                  LOG.error("Error while processing the report: for annotation" + a.toString() + " for value: "
                        + value.toString(), e);
               }
            }
            reportWriter.writeFooter();
         }
      } catch (Exception e) {
         LOG.error("Error while processing the xml file.",e );
      } finally {
         if (reportWriter != null) {
            reportWriter.close();
         }
         if (bufferedWriter != null) {
            bufferedWriter.close();
         }
      }
   }

   private BufferedWriter createWriter(final String packageName, final String fileName) throws IOException {
      return new BufferedWriter(
            processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, packageName, fileName).openWriter());
   }
}
