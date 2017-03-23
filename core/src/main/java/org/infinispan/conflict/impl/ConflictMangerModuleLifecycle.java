package org.infinispan.conflict.impl;

import org.infinispan.configuration.cache.Configuration;
import org.infinispan.conflict.ConflictManager;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.lifecycle.AbstractModuleLifecycle;
import org.infinispan.lifecycle.ModuleLifecycle;
import org.kohsuke.MetaInfServices;

/**
 * @author Ryan Emerson
 * @since 9.1
 */
@SuppressWarnings("unused")
@MetaInfServices(value = ModuleLifecycle.class)
public class ConflictMangerModuleLifecycle extends AbstractModuleLifecycle {
   @Override
   public void cacheStarting(ComponentRegistry cr, Configuration configuration, String cacheName) {
      if (configuration.clustering().cacheMode().isClustered()) {
         ConflictManager conflictManager = cr.getComponent(ConflictManager.class);
         if (conflictManager == null) {
            conflictManager = new DefaultConflictManager();
            cr.registerComponent(conflictManager, ConflictManager.class);
         }
      }
   }
}
