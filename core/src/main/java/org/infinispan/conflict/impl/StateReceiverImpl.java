package org.infinispan.conflict.impl;

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
import org.infinispan.container.DataContainer;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.distribution.ch.ConsistentHash;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.factories.annotations.Start;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.DataRehashed;
import org.infinispan.notifications.cachelistener.event.DataRehashedEvent;
import org.infinispan.remoting.rpc.RpcManager;
import org.infinispan.remoting.transport.Address;
import org.infinispan.statetransfer.InboundTransferTask;
import org.infinispan.statetransfer.StateChunk;

import net.jcip.annotations.GuardedBy;

/**
 * @author Ryan Emerson
 * @since 9.0
 */
@Listener
public class StateReceiverImpl<K, V> implements StateReceiver<K, V> {

   private static Log log = LogFactory.getLog(StateReceiverImpl.class);
   private static boolean trace = log.isTraceEnabled();

   private String cacheName;
   private Cache<K, V> cache;
   private CommandsFactory commandsFactory;
   private DataContainer<K, V> dataContainer;
   private RpcManager rpcManager;
   private long transferTimeout;

   private final Object lock = new Object();

   @GuardedBy("lock")
   private final Map<K, Map<Address, InternalCacheEntry<K, V>>> keyReplicaMap = new HashMap<>();

   @GuardedBy("lock")
   private final Map<Address, InboundTransferTask> transferTaskMap = new HashMap<>();

   private CompletableFuture<List<Map<Address, InternalCacheEntry<K, V>>>> segmentRequestFuture = CompletableFuture.completedFuture(null);

   private volatile int minTopologyId;
   private volatile int segmentId;

   @Inject
   public void init(Cache<K, V> cache,
                    CommandsFactory commandsFactory,
                    DataContainer<K, V> dataContainer,
                    RpcManager rpcManager) {
      this.cache = cache;
      this.commandsFactory = commandsFactory;
      this.dataContainer = dataContainer;
      this.rpcManager = rpcManager;
   }

   @Start
   public void start() {
      this.cache.addListener(this);
      this.cacheName = cache.getName();
      this.transferTimeout = cache.getCacheConfiguration().clustering().stateTransfer().timeout();
   }

   @Override
   public void stop() {
      synchronized (lock) {
         if (trace) log.tracef("Stop called on StateReceiverImpl for cache %s", cacheName);
         cancelAllSegmentRequests(null);
      }
   }

   @DataRehashed
   @SuppressWarnings("unused")
   void onDataRehash(DataRehashedEvent dataRehashedEvent) {
      if (dataRehashedEvent.isPre()) {
         // Perform actions on the pre-event so that we can cancel remaining transfers ASAP
         synchronized (lock) {
            boolean newSegmentOwners = newSegmentOwners(dataRehashedEvent);

            if (minTopologyId < 0 || newSegmentOwners)
               minTopologyId = dataRehashedEvent.getNewTopologyId();

            if (newSegmentOwners && !segmentRequestFuture.isDone())
               cancelAllSegmentRequests(new CacheException("Cancelling replica request as the owners of the requested " +
                     "segment have changed."));
         }
      }
   }

   @Override
   public CompletableFuture<List<Map<Address, InternalCacheEntry<K, V>>>> getAllReplicasForSegment(int segmentId, ConsistentHash hash) {
      synchronized (lock) {
         // TODO improve handling of concurrent invocations
         if (!segmentRequestFuture.isDone()) {
            if (trace) log.tracef("Concurrent invocations of getAllReplicasForSegment detected %s", segmentRequestFuture);
            throw new IllegalStateException("It is not possible to request multiple segments of a cache simultaneously.");
         }

         this.segmentId = segmentId;
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
         segmentRequestFuture = CompletableFuture
               .allOf(completableFutures.toArray(new CompletableFuture[completableFutures.size()]))
               .thenApply(aVoid -> getAddressValueMap());

         // If an exception is thrown by any of the inboundTransferTasks, then remove all segment results and cancel all tasks
         segmentRequestFuture.exceptionally(throwable -> {
            if (trace) log.tracef(throwable, "Exception when processing InboundTransferTask for cache %s", cacheName);
            cancelAllSegmentRequests(throwable);
            return null;
         });
         return segmentRequestFuture;
      }
   }

   @Override
   public void receiveState(Address sender, int topologyId, Collection<StateChunk> stateChunks) {
      synchronized (lock) {
         if (segmentRequestFuture.isDone()) {
            if (trace)
               log.tracef("Ignoring received state for cache %s because the associated request has completed %s",
                     cacheName, segmentRequestFuture);
            return;
         }

         if (topologyId < minTopologyId) {
            if (trace)
               log.tracef("Discarding state response with old topology id %d for cache %s, the smallest allowed topology id is %d",
                     topologyId, minTopologyId, cacheName);
            return;
         }

         InboundTransferTask transferTask = transferTaskMap.remove(sender);
         if (transferTask == null) {
            if (trace)
               log.tracef("State received for an unknown request. No record of a state request exists for node %s", sender);
            return;
         }

         for (StateChunk chunk : stateChunks) {
            chunk.getCacheEntries().forEach(ice -> addKeyToReplicaMap(sender, ice));
            transferTask.onStateReceived(chunk.getSegmentId(), chunk.isLastChunk());
         }
      }
   }

   Map<K, Map<Address, InternalCacheEntry<K, V>>> getKeyReplicaMap() {
      return keyReplicaMap;
   }

   Map<Address, InboundTransferTask> getTransferTaskMap() {
      return transferTaskMap;
   }

   InboundTransferTask createTransferTask(int segmentId, Address source) {
      return new InboundTransferTask(Collections.singleton(segmentId), source, rpcManager.getTopologyId(),
            rpcManager, commandsFactory, transferTimeout, cacheName, false);
   }

   private List<Map<Address, InternalCacheEntry<K, V>>> getAddressValueMap() {
      synchronized (lock) {
         List<Map<Address, InternalCacheEntry<K, V>>> retVal = keyReplicaMap.entrySet().stream()
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
            segmentRequestFuture.completeExceptionally(throwable);
         } else {
            segmentRequestFuture.cancel(true);
         }
      }
   }

   private void addKeyToReplicaMap(Address address, InternalCacheEntry<K, V> ice) {
      keyReplicaMap.computeIfAbsent(ice.getKey(), k -> new HashMap<>()).put(address, ice);
   }

   private boolean newSegmentOwners(DataRehashedEvent dataRehashedEvent) {
      ConsistentHash startHash = dataRehashedEvent.getConsistentHashAtStart();
      ConsistentHash endHash = dataRehashedEvent.getConsistentHashAtEnd();
      if (startHash == null)
         return true;

      Collection<Address> newMembers = startHash.locateOwnersForSegment(segmentId);
      Collection<Address> currentMembers = endHash.locateOwnersForSegment(segmentId);
      return !currentMembers.equals(newMembers);
   }
}
