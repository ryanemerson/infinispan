package org.infinispan.logging.annotations;


import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Descriptions should provide additional detail for each message and suggested user action.
 * Generate HTML reference for log messages and descriptions. 
 * Description.
 * 
 * @author Durgesh Anaokar
 * @since 5.3
 */

@Retention(CLASS)
@Target(METHOD)
public @interface Description {

   /**
    * The default format string of this message.
    * <p>
    * Expressions in the form of {@code ${property.key:default-value}} can be used for the value. If the property key is
    * prefixed with {@code sys.} a {@linkplain System#getProperty(String) system property} will be used. If the key is
    * prefixed with {@code env.} an {@linkplain System#getenv(String) environment variable} will be used. In all other cases
    * the {@code org.jboss.logging.tools.expressionProperties} processor argument is used to specify the path the properties
    * file which contains the values for the expressions.
    * </p>
    *
    * @return the format string
    */
   String value();

   /**
    * The format type of this method (defaults to {@link Format#PRINTF}).
    *
    * @return the format type
    */
   Format format() default Format.NO_FORMAT;

   /**
    * The possible format types.
    */
   enum Format {

       /**
        * A {@link java.text.MessageFormat}-type format string.
        */
       MESSAGE_FORMAT,

       /**
        * Indicates the message should not be formatted.
        */
       NO_FORMAT,
   }
}
