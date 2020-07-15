package org.infinispan.server.core.backup;

import static org.infinispan.server.core.BackupManager.ResourceType.CACHES;
import static org.infinispan.server.core.BackupManager.ResourceType.CACHE_CONFIGURATIONS;
import static org.infinispan.server.core.BackupManager.ResourceType.COUNTERS;
import static org.infinispan.server.core.BackupManager.ResourceType.PROTO_SCHEMAS;
import static org.infinispan.server.core.BackupManager.ResourceType.SCRIPTS;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.infinispan.server.core.BackupManager;

/**
 * @author Ryan Emerson
 * @since 11.0
 */
public class BackupParametersImpl implements BackupManager.Parameters {

   final String name;
   final Map<BackupManager.ResourceType, Set<String>> resources;

   public BackupParametersImpl(String name, Map<BackupManager.ResourceType, Set<String>> resources) {
      this.name = name;
      this.resources = resources;
   }

   @Override
   public String name() {
      return name;
   }

   @Override
   public Set<BackupManager.ResourceType> includedResourceTypes() {
      return resources.keySet();
   }

   @Override
   public Set<String> getQualifiedResources(BackupManager.ResourceType type) {
      Set<String> qualified = resources.get(type);
      return qualified.isEmpty() ? null : qualified;
   }

   public static class Builder {
      final Map<BackupManager.ResourceType, Set<String>> resources = new HashMap<>();
      String name;

      public Builder name(String name) {
         this.name = name;
         return this;
      }

      public Builder importAll() {
         return importAll(BackupManager.ResourceType.values());
      }

      public Builder importAll(BackupManager.ResourceType... resources) {
         for (BackupManager.ResourceType resource : resources)
            addResources(resource);
         return this;
      }

      public Builder ignore(BackupManager.ResourceType... resources) {
         for (BackupManager.ResourceType resource : resources)
            this.resources.remove(resource);
         return this;
      }

      public Builder addCaches(String... caches) {
         return addResources(CACHES, caches);
      }

      public Builder addCacheConfigurations(String... configs) {
         return addResources(CACHE_CONFIGURATIONS, configs);
      }

      public Builder addCounters(String... counters) {
         return addResources(COUNTERS, counters);
      }

      public Builder addProtoSchemas(String... schemas) {
         return addResources(PROTO_SCHEMAS, schemas);
      }

      public Builder addScripts(String... scripts) {
         return addResources(SCRIPTS, scripts);
      }

      private Builder addResources(BackupManager.ResourceType resource, String... resources) {
         this.resources.compute(resource, (k, v) -> {
            Set<String> set = v == null ? new HashSet<>() : v;
            Collections.addAll(set, resources);
            return set;
         });
         return this;
      }

      public BackupParametersImpl build() {
         return new BackupParametersImpl(name, resources);
      }
   }
}
