package org.infinispan.commons.configuration.defaults;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.infinispan.commons.configuration.attributes.Attribute;
import org.infinispan.commons.configuration.attributes.AttributeDefinition;
import org.infinispan.commons.configuration.attributes.AttributeSet;

/**
 * @author Ryan Emerson
 */
public class InfinispanDefaultsResolver implements DefaultsResolver {

   @Override
   public boolean isValidClass(String className) {
      return className.endsWith("Configuration.class") && !className.contains("$");
   }

   @Override
   public Map<String, String> extractDefaults(Set<Class> classes, String separator) throws Exception {
      Map<String, String> map = new HashMap<>();
      for (Class clazz : classes) {
         AttributeSet attributeSet = getAttributeSet(clazz);
         if (attributeSet == null)
            continue;

         attributeSet.attributes().stream()
               .map(Attribute::getAttributeDefinition)
               .filter(definition -> definition.getDefaultValue() != null)
               .forEach(definition -> map.put(getOutputKey(clazz, definition, separator), getOutputValue(definition)));
      }
      return map;
   }

   private AttributeSet getAttributeSet(Class clazz) throws IllegalAccessException {
      Field[] declaredFields = clazz.getDeclaredFields();
      List<AttributeDefinition> attributeDefinitions = new ArrayList<>();
      for (Field field : declaredFields) {
         if (Modifier.isStatic(field.getModifiers()) && AttributeDefinition.class.isAssignableFrom(field.getType())) {
            field.setAccessible(true);
            attributeDefinitions.add((AttributeDefinition) field.get(null));
         }
      }
      return new AttributeSet(clazz, attributeDefinitions.toArray(new AttributeDefinition[attributeDefinitions.size()]));
   }

   private String getOutputValue(AttributeDefinition definition) {
      // Remove @<hashcode> from toString of classes
      return definition.getDefaultValue().toString().split("@")[0];
   }

   private String getOutputKey(Class clazz, AttributeDefinition attribute, String seperator) {
      String className = clazz.getSimpleName();
      String root = className.startsWith("Configuration") ? className : className.replace("Configuration", "");
      return root + seperator + attribute.name();
   }
}
