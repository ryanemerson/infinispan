package org.infinispan.conflict.resolution;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.infinispan.Cache;
import org.infinispan.commands.CommandsFactory;
import org.infinispan.commons.CacheException;
import org.infinispan.commons.logging.Log;
import org.infinispan.commons.logging.LogFactory;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.container.DataContainer;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.container.entries.InternalCacheValue;
import org.infinispan.distribution.ch.ConsistentHash;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.remoting.rpc.RpcManager;
import org.infinispan.remoting.transport.Address;
import org.infinispan.statetransfer.InboundTransferTask;
import org.infinispan.statetransfer.StateChunk;
import org.infinispan.topology.CacheTopology;

import net.jcip.annotations.GuardedBy;

/**
 * @author Ryan Emerson
 * @since 9.0
 */
public class StateReceiverImpl<K, V> implements StateReceiver<V> {

   private static Log log = LogFactory.getLog(StateReceiverImpl.class);
   private static boolean trace = log.isTraceEnabled();

   private String cacheName;
   private CommandsFactory commandsFactory;
   private DataContainer<K, V> dataContainer;
   private RpcManager rpcManager;
   private long transferTimeout;

   private final Object lock = new Object();

   @GuardedBy("lock")
   private final Map<K, Map<Address, InternalCacheValue<V>>> keyReplicaMap = new HashMap<>();

   @GuardedBy("lock")
   private final Map<Address, InboundTransferTask> transferTaskMap = new HashMap<>();

   private CompletableFuture<List<Map<Address, InternalCacheValue<V>>>> completableFuture = CompletableFuture.completedFuture(null);

   private CacheTopology cacheTopology;
   private int segmentId;
   private int minTopologyId = 0;

   @Inject
   public void init(Cache cache,
                    CommandsFactory commandsFactory,
                    Configuration configuration,
                    DataContainer<K, V> dataContainer,
                    RpcManager rpcManager) {
      this.cacheName = cache.getName();
      this.commandsFactory = commandsFactory;
      this.dataContainer = dataContainer;
      this.rpcManager = rpcManager;

      this.transferTimeout = configuration.clustering().stateTransfer().timeout();
   }

   @Override
   public boolean isStateTransferInProgress() {
      synchronized (lock) {
         return !completableFuture.isDone();
      }
   }

   @Override
   public CompletableFuture<List<Map<Address, InternalCacheValue<V>>>> getAllReplicasForSegment(int segmentId) {
      synchronized (lock) {
         // TODO improve handling of concurrent invocations
         if (!completableFuture.isDone()) {
            if (trace) log.tracef("Concurrent invocations of getAllReplicasForSegment detected %s", completableFuture);
            throw new IllegalStateException("It is not possible to request multiple segments of a cache simultaneously.");
         }

         this.segmentId = segmentId;
         ConsistentHash hash = cacheTopology.getReadConsistentHash();
         List<Address> replicas = hash.locateOwnersForSegment(segmentId);
         List<CompletableFuture<Void>> completableFutures = new ArrayList<>();
         for (Address replica : replicas) {
            if (replica.equals(rpcManager.getAddress())) {
               dataContainer.forEach(entry -> {
                  K key = entry.getKey();
                  if (hash.getSegment(key) == segmentId) {
                     addKeyToReplicaMap(replica, entry);
                  }
               });
            } else {
               InboundTransferTask transferTask = createTransferTask(segmentId, replica);
               transferTaskMap.put(replica, transferTask);
               completableFutures.add(transferTask.requestSegments());
            }
         }
         completableFuture = CompletableFuture
               .allOf(completableFutures.toArray(new CompletableFuture[completableFutures.size()]))
               .thenApply(aVoid -> getAddressValueMap());

         // If an exception is thrown by any of the inboundTransferTasks, then remove all segment results and cancel all tasks
         completableFuture.exceptionally(throwable -> {
            if (trace) log.tracef(throwable, "Exception when processing InboundTransferTask for cache %s", cacheName);
            cancelAllSegmentRequests(throwable);
            return null;
         });
         return completableFuture;
      }
   }

   @Override
   public void receiveState(Address sender, int topologyId, Collection<StateChunk> stateChunks) {
      synchronized (lock) {
         if (completableFuture.isDone()) {
            if (trace)
               log.tracef("Ignoring received state for cache %s because the associated request has completed %s",
                     cacheName, completableFuture);
            return;
         }

         if (topologyId < minTopologyId) {
            if (trace)
               log.tracef("Discarding state response with old topology id %d for cache %s, the smallest allowed topology id is %d",
                     topologyId, minTopologyId, cacheName);
            return;
         }

         InboundTransferTask transferTask = transferTaskMap.get(sender);
         for (StateChunk chunk : stateChunks) {
            chunk.getCacheEntries().forEach(ice -> addKeyToReplicaMap(sender, ice));
            transferTask.onStateReceived(chunk.getSegmentId(), chunk.isLastChunk());
         }
      }
   }

   @Override
   public void stop() {
      synchronized (lock) {
         if (trace) log.tracef("Stop called on StateReceiverImpl for cache %s", cacheName);
         cancelAllSegmentRequests(null);
      }
   }

   @Override
   public void onTopologyUpdate(CacheTopology cacheTopology, boolean isRebalance) {
      if (trace) {
         boolean isMember = cacheTopology.getMembers().contains(rpcManager.getAddress());
         log.tracef("Received new topology for cache %s, isRebalance = %b, isMember = %b, topology = %s",
               cacheName, isRebalance, isMember, cacheTopology);
      }

      synchronized (lock) {
         boolean newSegmentOwners = isRebalance && newSegmentOwners(cacheTopology);
         this.cacheTopology = cacheTopology;

         if (newSegmentOwners)
            minTopologyId = cacheTopology.getTopologyId();

         if (newSegmentOwners && !completableFuture.isDone())
            cancelAllSegmentRequests(new CacheException("Cancelling replica request as the owners of the requested " +
                  "segment have changed."));
      }
   }

   InboundTransferTask createTransferTask(int segmentId, Address source) {
      return new InboundTransferTask(Collections.singleton(segmentId), source, cacheTopology.getTopologyId(),
            rpcManager, commandsFactory, transferTimeout, cacheName, false);
   }

   private List<Map<Address, InternalCacheValue<V>>> getAddressValueMap() {
      synchronized (lock) {
         List<Map<Address, InternalCacheValue<V>>> retVal = keyReplicaMap.entrySet().stream()
               .map(Map.Entry::getValue)
               .collect(Collectors.toList());
         keyReplicaMap.clear();
         transferTaskMap.clear();
         return retVal;
      }
   }

   private void cancelAllSegmentRequests(Throwable throwable) {
      synchronized (lock) {
         if (trace) log.tracef(throwable, "Cancelling All Segment Requests on cache %s", cacheName);
         transferTaskMap.forEach((address, inboundTransferTask) -> inboundTransferTask.cancel());
         transferTaskMap.clear();
         if (throwable != null) {
            completableFuture.completeExceptionally(throwable);
         } else {
            completableFuture.cancel(true);
         }
      }
   }

   private void addKeyToReplicaMap(Address address, InternalCacheEntry<K, V> ice) {
      keyReplicaMap.computeIfAbsent(ice.getKey(), k -> new HashMap()).put(address, ice.toInternalCacheValue());
   }

   private boolean newSegmentOwners(CacheTopology newTopology) {
      if (cacheTopology == null)
         return true;

      Collection<Address> newMembers = newTopology.getReadConsistentHash().locateOwnersForSegment(segmentId);
      Collection<Address> currentMembers = cacheTopology.getReadConsistentHash().locateOwnersForSegment(segmentId);
      return !currentMembers.equals(newMembers);
   }
}
