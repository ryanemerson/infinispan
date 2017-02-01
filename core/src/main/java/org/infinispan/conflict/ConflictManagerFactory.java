package org.infinispan.conflict;

import java.util.Objects;

import org.infinispan.AdvancedCache;
import org.infinispan.security.AuthorizationManager;
import org.infinispan.security.AuthorizationPermission;

/**
 * A {@link ConflictManager} factory for cache instances.
 *
 * @author Ryan Emerson
 * @since 9.0
 */
final public class ConflictManagerFactory {
   @SuppressWarnings("unchecked")
   public static <K,V> ConflictManager<K,V> get(AdvancedCache<K, V> cache) {
      AuthorizationManager authzManager = cache.getAuthorizationManager();
      if (authzManager != null) {
         authzManager.checkPermission(AuthorizationPermission.ALL_READ);
         authzManager.checkPermission(AuthorizationPermission.ALL_WRITE);
      }

      return Objects.requireNonNull(cache, "Cache cannot be null")
            .getAdvancedCache()
            .getComponentRegistry()
            .getComponent(ConflictManager.class);
   }
}
