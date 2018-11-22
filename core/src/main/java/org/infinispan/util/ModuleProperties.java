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

   private Map<Byte, ModuleCommandFactory> commandFactories;
   private Map<Byte, ModuleCommandInitializer> commandInitializers;

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
            for (Map.Entry<Byte, Class<? extends ReplicableCommand>> command : cmdFactory.getModuleCommands().entrySet()) {
               byte id = command.getKey();
               if (commandFactories.containsKey(id))
                  throw new IllegalArgumentException(String.format(
                        "Cannot use id %d for commands, as it is already in use by %s",
                        id, commandFactories.get(id).getClass().getName()));

               commandFactories.put(id, cmdFactory);
               commandInitializers.put(id, cmdInitializer);
            }
         }
      } else {
         log.debug("No module command extensions to load");
         commandInitializers = Collections.emptyMap();
         commandFactories = Collections.emptyMap();
      }
   }

   public Map<Byte, ModuleCommandFactory> moduleCommandFactories() {
      return commandFactories;
   }

   public Map<Byte, ModuleCommandInitializer> moduleCommandInitializers() {
      return commandInitializers;
   }
}
