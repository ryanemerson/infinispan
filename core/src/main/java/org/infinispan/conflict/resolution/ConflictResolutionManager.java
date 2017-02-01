package org.infinispan.conflict.resolution;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.container.entries.InternalCacheValue;
import org.infinispan.factories.scopes.Scope;
import org.infinispan.factories.scopes.Scopes;
import org.infinispan.remoting.transport.Address;
import org.infinispan.topology.CacheTopology;

/**
 * @author Ryan Emerson
 * @since 9.0
 */
@Scope(Scopes.NAMED_CACHE)
public interface ConflictResolutionManager<K, V> {

   void onTopologyUpdate(CacheTopology cacheTopology, boolean isRebalance);

   /**
    * Get all CacheEntry's that exists for a given key. Note, concurrent calls to this method for the same key will utilise
    * the same CompletableFuture inside this method and consequently return the same results as all other invocations.
    *
    * @param key the key for which associated entries are to be returned
    * @return a map of an address and it's associated CacheEntry
    * @throws org.infinispan.commons.CacheException if one or more versions of a key cannot be retrieved. Also thrown
    * if a rebalance is initiated while attempting to retrieve all versions of the key.
    * @throws IllegalStateException if called whilst state transfer is in progress.
    */
   Map<Address, InternalCacheValue<V>> getAllVersions(K key);

   /**
    * @return a stream of Map<Address, InternalCacheValue>> for all conflicts detected throughout this cache.
    * @throws org.infinispan.commons.CacheException if a rebalance is initiated while checking for conflicts.
    * @throws IllegalStateException if called whilst state transfer is in progress.
    */
   Stream<Map<Address, InternalCacheEntry<K, V>>> getConflicts();

   /**
    * It is possible to check for all conflicts in a cache if State transfer is not in progress for the given cache.
    *
    * @return true if it's possible to check for conflicts on this cache, otherwise false.
    */
   boolean isResolutionPossible() throws Exception;
}
