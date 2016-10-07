package org.infinispan.tools.defaults;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.infinispan.commons.configuration.attributes.Attribute;
import org.infinispan.commons.configuration.attributes.AttributeDefinition;
import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.infinispan.configuration.cache.AbstractStoreConfiguration;
import org.infinispan.configuration.cache.AsyncStoreConfiguration;
import org.infinispan.configuration.cache.BackupConfiguration;
import org.infinispan.configuration.cache.ClusterLoaderConfiguration;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.CustomStoreConfiguration;
import org.infinispan.configuration.cache.SingleFileStoreConfiguration;
import org.infinispan.configuration.cache.SingletonStoreConfiguration;
import org.infinispan.configuration.global.GlobalJmxStatisticsConfiguration;
import org.infinispan.configuration.global.GlobalStateConfiguration;
import org.infinispan.configuration.global.SerializationConfiguration;
import org.infinispan.configuration.global.TransportConfiguration;
import org.infinispan.persistence.jdbc.configuration.AbstractJdbcStoreConfiguration;
import org.infinispan.persistence.jdbc.configuration.JdbcBinaryStoreConfiguration;
import org.infinispan.persistence.jdbc.configuration.JdbcMixedStoreConfiguration;
import org.infinispan.persistence.jdbc.configuration.JdbcStringBasedStoreConfiguration;
import org.infinispan.persistence.jpa.configuration.JpaStoreConfiguration;
import org.infinispan.persistence.leveldb.configuration.LevelDBStoreConfiguration;
import org.infinispan.persistence.remote.configuration.RemoteStoreConfiguration;
import org.infinispan.persistence.rest.configuration.RestStoreConfiguration;

/**
 * @author Ryan Emerson
 */
public class DefaultAttributes {

   private final String separator;
   private final Configuration config;
   private final Map<String, String> defaultsMap;

   public DefaultAttributes(Configuration config, String separator) throws Exception {
      this.config = config;
      this.separator = separator;
      this.defaultsMap = new HashMap<>();

      extractDefaultsFromConfiguration(defaultsMap, config, null);
      extractGlobalDefaults(defaultsMap);
      extractStoreDefaults(defaultsMap);
      extractOtherConfigurations(defaultsMap);
   }

   public Map<String, String> getDefaultsMap() {
      return defaultsMap;
   }

   private void extractDefaultsFromConfiguration(Map<String, String> map, Object config, String path) throws Exception {
      Class configClass = config.getClass();
      String attrPath = path != null ? path + separator + configClass.getSimpleName().replace("Configuration", "") : "Config";

      // Add attributes in the current configuration object to the map
      AttributeSet attributeSet = getAttributeSet(configClass);
      addAttributeSetToMap(attributeSet, map, attrPath);

      // Recursively process all sub configurations in the current configuration object
      Field[] fields = config.getClass().getDeclaredFields();
      for (Field field : fields) {
         if (field.getName().toLowerCase().contains("configuration")) {
            field.setAccessible(true);
            try {
               extractDefaultsFromConfiguration(map, field.get(config), attrPath);
            } catch (IllegalAccessException ignore) {
            }
         }
      }
   }

   private void extractStoreDefaults(Map<String, String> map) throws Exception {
      List<Class> configurations = new ArrayList<>();
      configurations.add(AbstractStoreConfiguration.class);
      configurations.add(AbstractJdbcStoreConfiguration.class);
      configurations.add(JdbcStringBasedStoreConfiguration.class);
      configurations.add(JdbcBinaryStoreConfiguration.class);
      configurations.add(JdbcMixedStoreConfiguration.class);
      configurations.add(ClusterLoaderConfiguration.class);
      configurations.add(JpaStoreConfiguration.class);
      configurations.add(LevelDBStoreConfiguration.class);
      configurations.add(RemoteStoreConfiguration.class);
      configurations.add(RestStoreConfiguration.class);
      configurations.add(AsyncStoreConfiguration.class);
      configurations.add(CustomStoreConfiguration.class);
      configurations.add(SingleFileStoreConfiguration.class);
      configurations.add(SingletonStoreConfiguration.class);
      configurations.add(BackupConfiguration.class);

      extractDefaultsFromClasses(map, "Config" + separator + "Persistence" + separator, configurations);
   }

   private void extractOtherConfigurations(Map<String, String> map) throws Exception {
   }

   private void extractGlobalDefaults(Map<String, String> map) throws Exception {
      List<Class> globalConfigClasses = new ArrayList<>();
      globalConfigClasses.add(GlobalStateConfiguration.class);
      globalConfigClasses.add(GlobalJmxStatisticsConfiguration.class);
      globalConfigClasses.add(SerializationConfiguration.class);
      globalConfigClasses.add(TransportConfiguration.class);
      extractDefaultsFromClasses(map, "Global" + separator, globalConfigClasses);
   }

   private void extractDefaultsFromClasses(Map<String, String> map, String path, List<Class> classes) throws Exception {
      for (Class clazz : classes)
         extractDefaultFromClass(map, path, clazz);
   }

   private void extractDefaultFromClass(Map<String, String> map, String path, Class clazz) throws Exception {
      String attrPath = path + clazz.getSimpleName().replace("Configuration", "");
      addAttributeSetToMap(getAttributeSet(clazz), map, attrPath);
   }

   private void addAttributeSetToMap(AttributeSet attributeSet, Map<String, String> map, String path) {
      if (attributeSet == null)
         return;

      attributeSet.attributes().stream()
            .map(Attribute::getAttributeDefinition)
            .filter(definition -> definition.getDefaultValue() != null)
            .forEach(definition -> map.put(path + separator + definition.name(), getOutputValue(definition)));
   }

   private AttributeSet getAttributeSet(Class clazz) throws Exception {
      try {
         Method method = clazz.getDeclaredMethod("attributeDefinitionSet");
         method.setAccessible(true);
         return (AttributeSet) method.invoke(null);
      } catch (NoSuchMethodException e) {
         return null;
      }
   }

   private String getOutputValue(AttributeDefinition definition) {
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
      for (int i = 1; i < args.length; i++) {
         if ("-asciidoctor".equals(args[i])) {
            elementSeparator = "-";
            createPropsFile = false;
            break;
         }
      }

      Configuration config = new ConfigurationBuilder().build();
      DefaultAttributes defaults = new DefaultAttributes(config, elementSeparator);
      writeMapToFile(defaults.getDefaultsMap(), args[0], createPropsFile);
   }

   private static void writeMapToFile(Map<String, String> map, String path, boolean propsFile) throws FileNotFoundException {
      Function<Map.Entry, String> outputString = propsFile ?
            entry -> entry.getKey() + " = " + entry.getValue() :
            entry -> ":" + entry.getKey() + ": " + entry.getValue();

      try (PrintWriter printWriter = new PrintWriter(path)) {
         map.entrySet().stream()
               .map(outputString)
               .sorted()
               .forEach(printWriter::println);
      }
   }
}
