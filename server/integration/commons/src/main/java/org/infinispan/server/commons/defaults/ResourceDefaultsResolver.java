package org.infinispan.server.commons.defaults;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.infinispan.commons.configuration.defaults.DefaultsResolver;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;

/**
 * @author Ryan Emerson
 */
public class ResourceDefaultsResolver implements DefaultsResolver {


   @Override
   public boolean isValidClass(String className) {
      return className.endsWith("Resource.class") && !className.contains("$");
   }

   @Override
   public Map<String, String> extractDefaults(Set<Class> classes, String separator) throws Exception {
      Map<String, String> map = new HashMap<>();
      for (Class clazz : classes) {
         getSimpleAttributeDefinitions(clazz)
               .filter(definition -> definition.getDefaultValue() != null)
               .forEach(definition -> map.put(getOutputKey(clazz, definition, separator), getOutputValue(definition)));
      }
      return map;
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

   private String getOutputKey(Class clazz, AttributeDefinition attribute, String seperator) {
      String className = clazz.getSimpleName().replace("Configuration", "").replace("Resource", "");
      return "Server." + className + seperator + attribute.getName();
   }
}
