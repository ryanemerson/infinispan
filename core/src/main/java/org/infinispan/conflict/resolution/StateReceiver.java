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
import org.infinispan.topology.CacheTopology;

/**
 * @author Ryan Emerson
 * @since 9.0
 */
@Scope(Scopes.NAMED_CACHE)
public interface StateReceiver<V> {

   boolean isTransferInProgress();

   /**
    * Cancels all ongoing replica requests.
    * This is executed when the cache is shutting down.
    */
   void stop();

   /**
    * Return all replicas of a cache entry for a given segment
    *
    * @throws IllegalStateException if this method is invoked whilst a previous request for Replicas is still executing
    */
   CompletableFuture<List<Map<Address, InternalCacheValue<V>>>> getAllReplicasForSegment(int segmentId);

   void receiveState(Address sender, int topologyId, Collection<StateChunk> stateChunks);

   void onTopologyUpdate(CacheTopology cacheTopology, boolean isRebalance);
}
