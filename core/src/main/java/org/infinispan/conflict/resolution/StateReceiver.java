package org.infinispan.conflict.resolution;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.infinispan.container.entries.CacheEntry;
import org.infinispan.container.entries.InternalCacheValue;
import org.infinispan.factories.scopes.Scope;
import org.infinispan.factories.scopes.Scopes;
import org.infinispan.remoting.transport.Address;
import org.infinispan.statetransfer.StateChunk;

/**
 * @author Ryan Emerson
 * @since 9.0
 */
@Scope(Scopes.NAMED_CACHE)
public interface StateReceiver<V> {

   /**
    * Return all replicas of a cache entry for a given segment
    */
   CompletableFuture<List<Map<Address, InternalCacheValue<V>>>> getAllReplicasForSegment(int segmentId);

   void receiveState(Address sender, Collection<StateChunk> stateChunks);
}
