package org.infinispan.conflict;

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
public interface ConflictManager<K, V> {

   /**
    * Get all CacheEntry's that exists for a given key. Note, concurrent calls to this method for the same key will utilise
    * the same CompletableFuture inside this method and consequently return the same results as all other invocations.
    * If this method is invoked during state transfer it will block until rehashing has completed.  Similarly, if
    * state transfer is initiated during an invocation of this method and rehashing affects the segments of the provided
    * key, the initial requests for the entries versions are cancelled and re-attempted once state transfer has completed.
    *
    * @param key the key for which associated entries are to be returned
    * @return a map of an address and it's associated CacheEntry
    * @throws org.infinispan.commons.CacheException if one or more versions of a key cannot be retrieved.
    */
   Map<Address, InternalCacheValue<V>> getAllVersions(K key);

   /**
    * @return a stream of Map<Address, InternalCacheValue>> for all conflicts detected throughout this cache.
    * @throws org.infinispan.commons.CacheException if state transfer is initiated while checking for conflicts.
    * @throws IllegalStateException if called whilst state transfer is in progress.
    */
   Stream<Map<Address, InternalCacheEntry<K, V>>> getConflicts();

   /**
    * @return true if a state transfer is currently in progress.
    */
   boolean isStateTransferInProgress() throws Exception;
}
