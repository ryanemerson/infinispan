package org.infinispan.commons.configuration.defaults;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.infinispan.commons.configuration.attributes.Attribute;
import org.infinispan.commons.configuration.attributes.AttributeDefinition;
import org.infinispan.commons.configuration.attributes.AttributeSet;

/**
 * @author Ryan Emerson
 */
public class DefaultAttributeExtractor {

   // TODO move to commons
   // Create implementation of ConfigurationDefaultsExtractor here as well

   private final String separator;
   private final Set<Class> configClasses;
   private final Map<String, String> map;

   public DefaultAttributeExtractor(Set<Class> configClasses, String separator) throws Exception {
      this.configClasses = configClasses;
      this.separator = separator;
      this.map = new HashMap<>();

      extractDefaultsFromClasses();
   }

   private void extractDefaultsFromClasses() throws Exception {
      for (Class clazz : configClasses) {
         AttributeSet attributeSet = getAttributeSet(clazz);
         if (attributeSet == null)
            continue;

         attributeSet.attributes().stream()
               .map(Attribute::getAttributeDefinition)
               .filter(definition -> definition.getDefaultValue() != null)
               .forEach(definition -> map.put(clazz.getSimpleName() + separator + definition.name(), getOutputValue(definition)));
      }
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
      getClassesFromPackages(urls, configClasses);
      jarNames.forEach(jar -> getClassesFromJar(jar, urls, configClasses));

      DefaultAttributeExtractor defaults = new DefaultAttributeExtractor(configClasses, elementSeparator);
      writeMapToFile(defaults.map, args[0], createPropsFile);
   }

   public static void getClassesFromJar(String jarName, URL[] urls, Set<Class> classes) {
      Optional<String> jarPath = Stream.of(urls).map(URL::getPath)
            .filter(path -> path.endsWith(jarName))
            .findFirst();

      if (jarPath.isPresent()) {
         try {
            ZipInputStream jar = new ZipInputStream(new FileInputStream(jarPath.get()));
            for (ZipEntry entry = jar.getNextEntry(); entry != null; entry = jar.getNextEntry()) {
               String name = entry.getName();
               if (!entry.isDirectory() && (name.endsWith("Configuration.class") || name.endsWith("Resource.class"))) {
                  String className = entry.getName().replace("/", ".");
                  classes.add(Class.forName(className.substring(0, className.length() - 6)));
               }
            }
         } catch (IOException | ClassNotFoundException e) {
            System.err.println("Unable to process jar '" + jarName + "': " + e);
         }
      } else {
         throw new IllegalArgumentException("Jar '" + jarName + "' not found on the classpath");
      }
   }

   public static void getClassesFromPackages(URL[] urls, Set<Class> classes) {
      PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:**/infinispan/**/target/classes");
      List<File> classDirs = Stream.of(urls)
            .map(url -> new File(url.getPath()).toPath())
            .filter(pathMatcher::matches)
            .map(Path::toFile)
            .collect(Collectors.toList());

      for (File packageRoot : classDirs) {
         try {
            getClassesInPackage(packageRoot, "", classes);
         } catch (ClassNotFoundException e) {
            System.err.println("Unable to process package '" + packageRoot + "': " + e);
         }
      }
   }

   private static void getClassesInPackage(File packageDir, String packageName, Set<Class> classes) throws ClassNotFoundException {
      if (packageDir.exists()) {
         File[] files = packageDir.listFiles();
         for (File file : files) {
            String fileName = file.getName();
            if (file.isDirectory()) {
               String subpackage = packageName.isEmpty() ? fileName : packageName + "." + fileName;
               getClassesInPackage(file, subpackage, classes);
            } else if ((fileName.endsWith("Configuration.class") || fileName.endsWith("Resource.class")) && !fileName.contains("$")) {
               String className = fileName.substring(0, fileName.length() - 6);
               classes.add(Class.forName(packageName + "." + className));
            }
         }
      } else {
         throw new IllegalArgumentException("Package '" + packageDir.getName() + "' not found");
      }
   }

   public static void writeMapToFile(Map<String, String> map, String path, boolean propsFile) throws IOException {
      File file = new File(path);
      if (file.getParentFile() != null)
         file.getParentFile().mkdirs();

      try (PrintWriter printWriter = new PrintWriter(file)) {
         map.entrySet().stream()
               .map(entry -> formatOutput(entry, propsFile))
               .sorted()
               .forEach(printWriter::println);
      }
   }

   private static String formatOutput(Map.Entry<String, String> entry, boolean propsFile) {
      String propName = entry.getKey().startsWith("Configuration") ? entry.getKey() : entry.getKey().replace("Configuration", "");
      if (!propsFile) {
         return ":" + propName + ":" + entry.getValue();
      }
      return propName + " = " + entry.getValue();
   }
}
