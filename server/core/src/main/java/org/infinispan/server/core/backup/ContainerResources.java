package org.infinispan.server.core.backup;

import static org.infinispan.server.core.BackupManager.ContainerResources.Type.CACHES;
import static org.infinispan.server.core.BackupManager.ContainerResources.Type.CACHE_CONFIGURATIONS;
import static org.infinispan.server.core.BackupManager.ContainerResources.Type.COUNTERS;
import static org.infinispan.server.core.BackupManager.ContainerResources.Type.PROTO_SCHEMAS;
import static org.infinispan.server.core.BackupManager.ContainerResources.Type.SCRIPTS;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.infinispan.server.core.BackupManager;

/**
 * @author Ryan Emerson
 * @since 12.0
 */
public class ContainerResources implements BackupManager.ContainerResources {

   final Map<Type, Set<String>> resources;

   public ContainerResources(Map<Type, Set<String>> resources) {
      this.resources = resources;
   }

   @Override
   public Set<Type> includeTypes() {
      return resources.keySet();
   }

   @Override
   public Set<String> getQualifiedResources(Type type) {
      Set<String> qualified = resources.get(type);
      return qualified.isEmpty() ? null : qualified;
   }

   public static class Builder {
      final Map<Type, Set<String>> resources = new HashMap<>();

      public Builder importAll() {
         return importAll(Type.values());
      }

      public Builder importAll(Type... resources) {
         for (Type resource : resources)
            addResources(resource);
         return this;
      }

      public Builder ignore(Type... resources) {
         for (Type resource : resources)
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

      private Builder addResources(Type resource, String... resources) {
         this.resources.compute(resource, (k, v) -> {
            Set<String> set = v == null ? new HashSet<>() : v;
            Collections.addAll(set, resources);
            return set;
         });
         return this;
      }

      public ContainerResources build() {
         return new ContainerResources(resources);
      }
   }
}
