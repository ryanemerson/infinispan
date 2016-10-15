package org.infinispan.server.commons.defaults;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.infinispan.commons.configuration.defaults.DefaultAttributeExtractor;
import org.jboss.as.controller.SimpleAttributeDefinition;

/**
 * @author Ryan Emerson
 */
public class ResourceDefaultsExtractor {

   // TODO make so that common functionality is in the core and it just loads a specified DefaultsExtractor implementation
   // TODO create ConfigurationDefaultsExtractor interface in commons.config.defaults

   private final String separator;
   private final Set<Class> configClasses;
   private final Map<String, String> map;

   public ResourceDefaultsExtractor(Set<Class> configClasses, String separator) throws Exception {
      this.configClasses = configClasses;
      this.separator = separator;
      this.map = new HashMap<>();

      extractDefaultsFromClasses();
   }

   private void extractDefaultsFromClasses() throws Exception {
      for (Class clazz : configClasses) {
         getSimpleAttributeDefinitions(clazz)
               .filter(definition -> definition.getDefaultValue() != null)
               .forEach(definition -> map.put(clazz.getSimpleName() + separator + definition.getName(), getOutputValue(definition)));
      }
   }

   private Stream<SimpleAttributeDefinition> getSimpleAttributeDefinitions(Class clazz) {
      Field[] declaredFields = clazz.getDeclaredFields();
      List<SimpleAttributeDefinition> SimpleAttributeDefinitions = new ArrayList<>();
      for (Field field : declaredFields) {
         if (Modifier.isStatic(field.getModifiers()) && SimpleAttributeDefinition.class.isAssignableFrom(field.getType())) {
            field.setAccessible(true);
            try {
               SimpleAttributeDefinitions.add((SimpleAttributeDefinition) field.get(null));
            } catch (IllegalAccessException ignore) {
               // Shouldn't happen as we have setAccessible == true
            }
         }
      }
      return SimpleAttributeDefinitions.stream();
   }

   private String getOutputValue(SimpleAttributeDefinition definition) {
      // Remove @<hashcode> from toString of classes
      return definition.getDefaultValue().toString().split("@")[0];
   }

   public static void main(String[] args) throws Exception {
      if (args.length == 0) {
         System.err.println("A path must be specified for the resulting defaults file");
         System.exit(0);
      }

      // Assume properties output unless stated otherwise
      String elementSeparator = ".";
      boolean createPropsFile = true;
      List<String> jarNames = new ArrayList<>();
      for (int i = 1; i < args.length; i++) {
         if ("-asciidoctor".equals(args[i])) {
            elementSeparator = "-";
            createPropsFile = false;
            continue;
         }
         if (args[i].endsWith(".jar")) {
            jarNames.add(args[i]);
         }
      }

      URL[] urls = ((URLClassLoader) Thread.currentThread().getContextClassLoader()).getURLs();
      Set<Class> configClasses = new HashSet<>();
      DefaultAttributeExtractor.getClassesFromPackages(urls, configClasses);
      jarNames.forEach(jar -> DefaultAttributeExtractor.getClassesFromJar(jar, urls, configClasses));

      ResourceDefaultsExtractor defaults = new ResourceDefaultsExtractor(configClasses, elementSeparator);
      DefaultAttributeExtractor.writeMapToFile(defaults.map, args[0], createPropsFile);
   }
}
