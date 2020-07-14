package org.infinispan.server.core.backup;

import static org.infinispan.server.core.BackupManager.Resource.CACHES;
import static org.infinispan.server.core.BackupManager.Resource.CACHE_CONFIGURATIONS;
import static org.infinispan.server.core.BackupManager.Resource.COUNTERS;
import static org.infinispan.server.core.BackupManager.Resource.PROTO_SCHEMAS;
import static org.infinispan.server.core.BackupManager.Resource.SCRIPTS;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.infinispan.server.core.BackupManager;

/**
 * @author Ryan Emerson
 * @since 11.0
 */
public class BackupParametersImpl implements BackupManager.Parameters {

   final String name;
   final Map<BackupManager.Resource, Set<String>> resources;

   public BackupParametersImpl(String name, Map<BackupManager.Resource, Set<String>> resources) {
      this.name = name;
      this.resources = resources;
   }

   @Override
   public String name() {
      return name;
   }

   @Override
   public Set<String> get(BackupManager.Resource resource) {
      return resources.get(resource);
   }

   @Override
   public Set<String> computeIfEmpty(BackupManager.Resource resource, Supplier<Set<String>> supplier) {
      return resources.compute(resource, (k, v) -> {
         if (v != null && v.isEmpty())
            return supplier.get();
         return v;
      });
   }

   public static class Builder {
      final Map<BackupManager.Resource, Set<String>> resources = new HashMap<>();
      String name;

      public Builder name(String name) {
         this.name = name;
         return this;
      }

      public Builder importAll() {
         return importAll(BackupManager.Resource.values());
      }

      public Builder importAll(BackupManager.Resource... resources) {
         for (BackupManager.Resource resource : resources)
            addResources(resource);
         return this;
      }

      public Builder ignore(BackupManager.Resource... resources) {
         for (BackupManager.Resource resource : resources)
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

      private Builder addResources(BackupManager.Resource resource, String... resources) {
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
