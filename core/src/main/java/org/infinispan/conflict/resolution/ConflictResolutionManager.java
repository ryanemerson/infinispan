package org.infinispan.conflict.resolution;

import java.util.Map;
import java.util.stream.Stream;

import org.infinispan.container.entries.InternalCacheEntry;
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
   Stream<Map<Address, InternalCacheEntry<K, V>>> getConflicts();

   /**
    * It is possible to check for all conflicts in a cache if State transfer is not in progress for the given cache.
    *
    * @return true if it's possible to check for conflicts on this cache, otherwise false.
    */
   boolean isResolutionPossible() throws Exception;

   /**
    * It is is possible to check for conflicts on the given key if State transfer is not in progress for the given key.
    * 
    * @param key the key object to check for conflicts
    * @return true if it's possible to check for conflicts on this cache, otherwise false.
    */
   boolean isResolutionPossible(Object key) throws Exception;
}
