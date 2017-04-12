package org.infinispan.conflict.impl;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.distribution.ch.ConsistentHash;
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
public interface StateReceiver<K, V> {

   /**
    * Cancels all ongoing replica requests.
    * This is executed when the cache is shutting down.
    */
   void stop();

   /**
    * Return all replicas of a cache entry for a given segment. We require the ConsitentHash to be passed here, as it is
    * necessary for the hash of the last stable topology to be utilised during an automatic merge, before a
    * new merged topology is installed.
    *
    * @throws IllegalStateException if this method is invoked whilst a previous request for Replicas is still executing
    */
   CompletableFuture<List<Map<Address, InternalCacheEntry<K, V>>>> getAllReplicasForSegment(int segmentId, ConsistentHash hash);

   void receiveState(Address sender, int topologyId, Collection<StateChunk> stateChunks);
}
