package org.infinispan.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.infinispan.commands.ReplicableCommand;
import org.infinispan.commands.module.ModuleCommandExtensions;
import org.infinispan.commands.module.ModuleCommandFactory;
import org.infinispan.commands.module.ModuleCommandInitializer;
import org.infinispan.commons.util.ServiceFinder;
import org.infinispan.factories.components.ModuleMetadataFileFinder;
import org.infinispan.lifecycle.ModuleLifecycle;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * The <code>ModuleProperties</code> class represents Infinispan's module service extensions.
 *
 * @author Vladimir Blagojevic
 * @author Sanne Grinovero
 * @author Galder Zamarreño
 * @since 4.0
 */
public final class ModuleProperties {

   private static final Log log = LogFactory.getLog(ModuleProperties.class);

   private Map<Class<? extends ReplicableCommand>, ModuleCommandFactory> commandFactories;
   private Map<Class<? extends ReplicableCommand>, ModuleCommandInitializer> commandInitializers;

   public static Collection<ModuleLifecycle> resolveModuleLifecycles(ClassLoader cl) {
      return ServiceFinder.load(ModuleLifecycle.class, cl);
   }

   /**
    * Retrieves an Iterable containing metadata file finders declared by each module.
    * @param cl class loader to use
    * @return an Iterable of ModuleMetadataFileFinders
    */
   public static Iterable<ModuleMetadataFileFinder> getModuleMetadataFiles(ClassLoader cl) {
      return ServiceFinder.load(ModuleMetadataFileFinder.class, cl);
   }

   public void loadModuleCommandHandlers(ClassLoader cl) {
      Collection<ModuleCommandExtensions> moduleCmdExtLoader = ServiceFinder.load(ModuleCommandExtensions.class, cl);

      if (moduleCmdExtLoader.iterator().hasNext()) {
         commandFactories = new HashMap<>(1);
         commandInitializers = new HashMap<>(1);
         for (ModuleCommandExtensions extension : moduleCmdExtLoader) {
            log.debugf("Loading module command extension SPI class: %s", extension);
            ModuleCommandFactory cmdFactory = extension.getModuleCommandFactory();
            Objects.requireNonNull(cmdFactory);
            ModuleCommandInitializer cmdInitializer = extension.getModuleCommandInitializer();
            Objects.requireNonNull(cmdInitializer);
            for (Class<? extends ReplicableCommand> command : cmdFactory.getModuleCommandSet()) {
               commandFactories.put(command, cmdFactory);
               commandInitializers.put(command, cmdInitializer);
            }
         }
      } else {
         log.debug("No module command extensions to load");
         commandInitializers = Collections.emptyMap();
         commandFactories = Collections.emptyMap();
      }
   }

   public Collection<Class<? extends ReplicableCommand>> moduleCommands() {
      return commandFactories.keySet();
   }

   public Map<Class<? extends ReplicableCommand>, ModuleCommandFactory> moduleCommandFactories() {
      return commandFactories;
   }

   public Map<Class<? extends ReplicableCommand>, ModuleCommandInitializer> moduleCommandInitializers() {
      return commandInitializers;
   }
}
