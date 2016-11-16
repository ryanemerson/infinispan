package org.infinispan.conflict.resolution;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.infinispan.Cache;
import org.infinispan.commands.CommandsFactory;
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
import org.infinispan.topology.LocalTopologyManager;

import net.jcip.annotations.GuardedBy;

/**
 * @author Ryan Emerson
 * @since 9.0
 */
public class StateReceiverImpl<K, V> implements StateReceiver<V> {

   private static Log log = LogFactory.getLog(StateReceiverImpl.class);

   private Cache cache;
   private CommandsFactory commandsFactory;
   private Configuration configuration;
   private DataContainer<K, V> dataContainer;
   private LocalTopologyManager localTopologyManager;
   private RpcManager rpcManager;
   private long transferTimeout;

   @GuardedBy("keyReplicaMap")
   private final Map<K, Map<Address, InternalCacheValue<V>>> keyReplicaMap = new HashMap<>();

   @GuardedBy("keyReplicaMap")
   private final Map<Address, InboundTransferTask> transferTaskMap = new HashMap<>();

   private volatile CompletableFuture<List<Map<Address, InternalCacheValue<V>>>> transferFuture;

   @Inject
   public void init(Cache cache,
                    CommandsFactory commandsFactory,
                    Configuration configuration,
                    DataContainer<K, V> dataContainer,
                    LocalTopologyManager localTopologyManager,
                    RpcManager rpcManager) {
      this.cache = cache;
      this.commandsFactory = commandsFactory;
      this.configuration = configuration;
      this.dataContainer = dataContainer;
      this.localTopologyManager = localTopologyManager;
      this.rpcManager = rpcManager;

      this.transferTimeout = configuration.clustering().stateTransfer().timeout();
   }

   @Override
   public boolean isStateTransferInProgress() {
      return transferFuture != null;
   }

   @Override
   public CompletableFuture<List<Map<Address, InternalCacheValue<V>>>> getAllReplicasForSegment(int segmentId) {
      // TODO improve handling of concurrent invocations
      synchronized (keyReplicaMap) {
         if (transferFuture != null)
            throw new IllegalStateException("It is not possible to request multiple segments of a cache simultaneously.");

         transferFuture = new CompletableFuture<>();
         CacheTopology cacheTopology = localTopologyManager.getCacheTopology(cache.getName());
         ConsistentHash hash = cacheTopology.getWriteConsistentHash();
         List<Address> replicas = hash.locateOwnersForSegment(segmentId);

         for (Address replica : replicas) {
            if (replica.equals(rpcManager.getAddress())) {
               dataContainer.forEach(entry -> {
                  K key = entry.getKey();
                  if (hash.getSegment(key) == segmentId) {
                     addKeyToReplicaMap(replica, entry);
                  }
               });
            } else {
               InboundTransferTask inboundTransferTask = new InboundTransferTask(
                     Collections.singleton(segmentId), replica, cacheTopology.getTopologyId(), rpcManager,
                     commandsFactory, transferTimeout, cache.getName(), false);

               transferTaskMap.put(replica, inboundTransferTask);
               inboundTransferTask.requestSegments();
            }
         }
      }
      return transferFuture;
   }

   @Override
   public void receiveState(Address sender, Collection<StateChunk> stateChunks) {
      synchronized (keyReplicaMap) {
         InboundTransferTask transferTask = transferTaskMap.get(sender);
         boolean lastChunkReceived = false;
         for (StateChunk chunk : stateChunks) {
            transferTask.onStateReceived(chunk.getSegmentId(), chunk.isLastChunk());
            chunk.getCacheEntries().forEach(ice -> addKeyToReplicaMap(sender, ice));
            lastChunkReceived = chunk.isLastChunk();
         }

         if (lastChunkReceived) {
            List<Map<Address, InternalCacheValue<V>>> retVal = keyReplicaMap.entrySet().stream()
                  .map(Map.Entry::getValue)
                  .collect(Collectors.toList());
            keyReplicaMap.clear();
            transferFuture.complete(retVal);
            transferFuture = null;
         }
      }
   }

   @Override
   public void stop() {
      cancelAllSegmentRequests(null);
   }

   private void cancelAllSegmentRequests(Throwable t) {
      transferTaskMap.forEach((address, inboundTransferTask) -> inboundTransferTask.cancel());
      if (t != null) {
         transferFuture.completeExceptionally(t);
      } else {
         transferFuture.cancel(true);
      }
      transferFuture = null;
   }

   private void addKeyToReplicaMap(Address address, InternalCacheEntry<K, V> ice) {
      keyReplicaMap.computeIfAbsent(ice.getKey(), k -> new HashMap()).put(address, ice.toInternalCacheValue());
   }
}
