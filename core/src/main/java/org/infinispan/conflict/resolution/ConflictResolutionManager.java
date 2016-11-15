package org.infinispan.conflict.resolution;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import org.infinispan.container.entries.CacheEntry;
import org.infinispan.container.entries.InternalCacheValue;
import org.infinispan.factories.scopes.Scope;
import org.infinispan.factories.scopes.Scopes;
import org.infinispan.remoting.transport.Address;

/**
 * @author Ryan Emerson
 * @since 9.0
 */
@Scope(Scopes.NAMED_CACHE)
public interface ConflictResolutionManager<K, V> {

   /**
    * Get all CacheEntry's that exists for a given key.
    *
    * @param key the key for which associated entries are to be returned
    * @return a map of an address and it's associated CacheEntry
    */
   Map<Address, InternalCacheValue<V>> getAllVersions(K key);

   /**
    * @return a stream of Map<Address, InternalCacheValue>> for all conflicts detected throughout this cache
    */
   Stream<Map<Address, InternalCacheValue<V>>> getConflicts();

   /**
    * Resolve the conflict on a specified key by applying the configured {@link EntryMergePolicy} and updating all owners
    * using the putIfAbsent/replace/(versioned replace) from the primary node.
    *
    * @param key the key to resolve conflicts on.
    */
   void resolveConflict(K key);

   /**
    * A method which iterates over all conflicts in the cache and applies the configured {@link EntryMergePolicy}
    */
   void resolveConflicts();
}
