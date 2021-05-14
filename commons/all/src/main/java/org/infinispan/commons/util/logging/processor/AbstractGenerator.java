package org.infinispan.commons.util.logging.processor;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.element.TypeElement;

import org.jboss.logging.processor.apt.ToolLogger;
import org.jboss.logging.processor.model.MessageInterface;

public abstract class AbstractGenerator {

   private final ToolLogger logger;

   final ProcessingEnvironment processingEnv;

   /**
    * Constructs a new processor.
    *
    * @param processingEnv the processing environment.
    */
   AbstractGenerator(final ProcessingEnvironment processingEnv) {
       this.logger = ToolLogger.getLogger(processingEnv);
       this.processingEnv = processingEnv;
   }

   /**
    * Processes a type element.
    *
    * @param annotation       the annotation who trigger the processing
    * @param element          the element that contains the methods.
    * @param messageInterface the message interface to implement.
    */
   public abstract void processTypeElement(final TypeElement annotation, final TypeElement element, final MessageInterface messageInterface);


   /**
    * Returns the logger to log messages with.
    *
    * @return the logger to log messages with.
    */
   final ToolLogger logger() {
       return logger;
   }

   /**
    * Returns the name of the processor.
    *
    * @return the name of the processor.
    */
   public final String getName() {
       return this.getClass().getSimpleName();
   }

   /**
    * Returns the supported options set.
    *
    * @return the supported options set or empty set if none
    */
   public final Set<String> getSupportedOptions() {
       SupportedOptions options = this.getClass().getAnnotation(SupportedOptions.class);
       if (options != null) {
           return new HashSet<>(Arrays.asList(options.value()));
       }

       return Collections.emptySet();
   }

}
