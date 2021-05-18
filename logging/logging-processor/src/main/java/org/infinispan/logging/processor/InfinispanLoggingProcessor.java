package org.infinispan.logging.processor;

import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;

import org.kohsuke.MetaInfServices;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes({InfinispanLoggingProcessor.DESCRIPTION})
@MetaInfServices(Processor.class)
public class InfinispanLoggingProcessor extends AbstractProcessor {
   static final String DESCRIPTION = "org.infinispan.logging.annotations.Description";

   @Override
   public synchronized void init(ProcessingEnvironment processingEnv) {
      super.init(processingEnv);
      // TODO
   }

   @Override
   public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
      System.err.println("HERE!");
      throw new IllegalStateException("BLAH!");
   }
}
