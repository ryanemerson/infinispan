package org.infinispan.commons.configuration.defaults;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author Ryan Emerson
 */
public class DefaultAttributeExtractor {

   public static void main(String[] args) throws Exception {
      if (args.length < 2) {
         System.err.println("Expected usage: cmd <extractorImplementationClass> <outputFilePath> [--asciidoctor] [jarList...]");
         System.exit(0);
      }

      // Assume properties output unless stated otherwise
      String elementSeparator = ".";
      boolean createPropsFile = true;
      String outputFilePath = args[0];
      String extractorClass = args[1];
      List<String> jarNames = new ArrayList<>();
      for (int i = 2; i < args.length; i++) {
         if ("--asciidoctor".equals(args[i])) {
            elementSeparator = "-";
            createPropsFile = false;
            continue;
         }
         if (args[i].endsWith(".jar")) {
            jarNames.add(args[i].replace(".jar", ""));
         }
      }

      DefaultsResolver defaultsResolver = (DefaultsResolver) Class.forName(extractorClass).newInstance();
      URL[] urls = ((URLClassLoader) Thread.currentThread().getContextClassLoader()).getURLs();
      Set<Class> configClasses = new HashSet<>();
      getClassesFromPackages(defaultsResolver, urls, configClasses);
      jarNames.forEach(jar -> getClassesFromJar(defaultsResolver, jar, urls, configClasses));

      Map<String, String> defaults = defaultsResolver.extractDefaults(configClasses, elementSeparator);
      writeDefaultsToFile(defaults, outputFilePath, createPropsFile);
   }

   private static void getClassesFromJar(DefaultsResolver extractor, String jarName, URL[] urls, Set<Class> classes) {
      // Ignore version number, necessary as jar is loaded differently when sub module is installed in isolation
      Optional<String> jarPath = Stream.of(urls)
            .filter(url -> url.getFile().contains(jarName))
            .map(URL::getPath)
            .findFirst();

      if (jarPath.isPresent()) {
         try {
            ZipInputStream jar = new ZipInputStream(new FileInputStream(jarPath.get()));
            for (ZipEntry entry = jar.getNextEntry(); entry != null; entry = jar.getNextEntry()) {
               if (!entry.isDirectory() && extractor.isValidClass(entry.getName())) {
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

   private static void getClassesFromPackages(DefaultsResolver extractor, URL[] urls, Set<Class> classes) {
      PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:**/infinispan/**/target/classes");
      List<File> classDirs = Stream.of(urls)
            .map(url -> new File(url.getPath()).toPath())
            .filter(pathMatcher::matches)
            .map(Path::toFile)
            .collect(Collectors.toList());

      for (File packageRoot : classDirs) {
         try {
            getClassesInPackage(extractor, packageRoot, "", classes);
         } catch (ClassNotFoundException e) {
            System.err.println("Unable to process package '" + packageRoot + "': " + e);
         }
      }
   }

   private static void getClassesInPackage(DefaultsResolver extractor, File packageDir, String packageName, Set<Class> classes) throws ClassNotFoundException {
      if (packageDir.exists()) {
         File[] files = packageDir.listFiles();
         for (File file : files) {
            String fileName = file.getName();
            if (file.isDirectory()) {
               String subpackage = packageName.isEmpty() ? fileName : packageName + "." + fileName;
               getClassesInPackage(extractor, file, subpackage, classes);
            } else if (extractor.isValidClass(fileName)) {
               String className = fileName.substring(0, fileName.length() - 6);
               classes.add(Class.forName(packageName + "." + className));
            }
         }
      } else {
         throw new IllegalArgumentException("Package '" + packageDir.getName() + "' not found");
      }
   }

   private static void writeDefaultsToFile(Map<String, String> defaults, String path, boolean propsFile) throws IOException {
      File file = new File(path);
      if (file.getParentFile() != null)
         file.getParentFile().mkdirs();

      try (PrintWriter printWriter = new PrintWriter(new BufferedWriter(new FileWriter(file, true)))) {
         defaults.entrySet().stream()
               .map(entry -> formatOutput(entry, propsFile))
               .sorted()
               .forEach(printWriter::println);
      }
   }

   private static String formatOutput(Map.Entry<String, String> entry, boolean propsFile) {
      if (!propsFile) {
         return ":" + entry.getKey() + ":" + entry.getValue();
      }
      return entry.getKey() + " = " + entry.getValue();
   }
}
