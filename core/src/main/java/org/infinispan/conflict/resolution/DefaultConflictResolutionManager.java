package org.infinispan.conflict.resolution;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Spliterators;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.infinispan.Cache;
import org.infinispan.commands.CommandsFactory;
import org.infinispan.commands.remote.ClusteredGetCommand;
import org.infinispan.commons.CacheException;
import org.infinispan.container.DataContainer;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.container.entries.InternalCacheValue;
import org.infinispan.context.impl.FlagBitSets;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.remoting.inboundhandler.DeliverOrder;
import org.infinispan.remoting.responses.Response;
import org.infinispan.remoting.responses.SuccessfulResponse;
import org.infinispan.remoting.rpc.ResponseMode;
import org.infinispan.remoting.rpc.RpcManager;
import org.infinispan.remoting.rpc.RpcOptions;
import org.infinispan.remoting.transport.Address;
import org.infinispan.statetransfer.StateConsumer;
import org.infinispan.topology.CacheTopology;
import org.infinispan.topology.LocalTopologyManager;
import org.infinispan.topology.RebalancingStatus;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * @author Ryan Emerson
 */
public class DefaultConflictResolutionManager<K, V> implements ConflictResolutionManager<K, V> {

   private static Log log = LogFactory.getLog(DefaultConflictResolutionManager.class);

   private Cache cache;
   private CommandsFactory commandsFactory;
   private DataContainer<K, V> dataContainer;
   private DistributionManager distributionManager;
   private LocalTopologyManager localTopologyManager;
   private RpcManager rpcManager;
   private StateConsumer stateConsumer;
   private StateReceiver<K, V> stateReceiver;
   private String cacheName;
   private final AtomicBoolean streamInProgress = new AtomicBoolean();
   private final Map<K, VersionRequestMeta> versionRequestMap = new HashMap<>();
   private volatile CacheTopology cacheTopology;

   @Inject
   public void inject(Cache cache,
                      CommandsFactory commandsFactory,
                      DataContainer<K, V> dataContainer,
                      DistributionManager distributionManager,
                      LocalTopologyManager localTopologyManager,
                      RpcManager rpcManager,
                      StateConsumer stateConsumer,
                      StateReceiver<K, V> stateReceiver) {
      this.cache = cache;
      this.commandsFactory = commandsFactory;
      this.dataContainer = dataContainer;
      this.distributionManager = distributionManager;
      this.localTopologyManager = localTopologyManager;
      this.rpcManager = rpcManager;
      this.stateConsumer = stateConsumer;
      this.stateReceiver = stateReceiver;
      this.cacheName = cache.getName();
   }

   @Override
   public void onTopologyUpdate(CacheTopology cacheTopology, boolean isRebalance) {
      this.cacheTopology = cacheTopology;
      if (isRebalance)
         cancelOutdatedKeyVersionRequests(cacheTopology);
      stateReceiver.onTopologyUpdate(cacheTopology, isRebalance);
   }

   private void cancelOutdatedKeyVersionRequests(CacheTopology newTopology) {
      synchronized (versionRequestMap) {
         for (Map.Entry<K, VersionRequestMeta> entry : versionRequestMap.entrySet()) {
            VersionRequestMeta meta = entry.getValue();
            if (newTopology.getRebalanceId() > meta.rebalanceId)
               meta.completableFuture.completeExceptionally(log.getAllVersionsOfKeyTopologyChange(entry.getKey(), cacheName));
         }
      }
   }

   @Override
   public Map<Address, InternalCacheValue<V>> getAllVersions(final K key) {
      if (stateConsumer.isStateTransferInProgress())
         throw log.getAllVersionsOfKeyDuringStateTransfer(key, cacheName);

      final VersionRequestMeta meta;
      final Map<Address, InternalCacheValue<V>> versionsMap = new HashMap<>();
      synchronized (versionRequestMap) {
         meta = versionRequestMap.computeIfAbsent(key, k -> new VersionRequestMeta());
         if (meta.isRequestInProgress())
            return waitForAllVersions(key, meta); // Return the existing CompletableFuture if a request is already in progress
      }

      List<Address> keyReplicaNodes = distributionManager.getWriteConsistentHash().locateOwners(key);
      Address localAddress = rpcManager.getAddress();
      if (keyReplicaNodes.contains(localAddress)) {
         InternalCacheValue<V> icv = dataContainer.containsKey(key) ? dataContainer.get(key).toInternalCacheValue() : null;
         versionsMap.put(localAddress, icv);
      }

      ClusteredGetCommand cmd = commandsFactory.buildClusteredGetCommand(key, FlagBitSets.SKIP_OWNERSHIP_CHECK);
      long timeout = cache.getCacheConfiguration().clustering().remoteTimeout();
      RpcOptions rpcOptions = new RpcOptions(timeout, TimeUnit.MILLISECONDS, null, ResponseMode.SYNCHRONOUS_IGNORE_LEAVERS, DeliverOrder.NONE);
      CompletableFuture<Map<Address, Response>> rpcFuture = rpcManager.invokeRemotelyAsync(keyReplicaNodes, cmd, rpcOptions);

      meta.completableFuture = rpcFuture.handle((responseMap, exception) -> {
         if (exception != null) {
            String msg = String.format("%s encountered when trying to receive all versions of key '%s' on cache '%s'",
                  exception.getCause(), key, cacheName);
            throw new CacheException(msg, exception.getCause());
         }

         for (Map.Entry<Address, Response> entry : responseMap.entrySet()) {
            if (entry.getValue() instanceof SuccessfulResponse) {
               SuccessfulResponse response = (SuccessfulResponse) entry.getValue();
               versionsMap.put(entry.getKey(), (InternalCacheValue<V>) response.getResponseValue());
            } else {
               throw new CacheException(String.format("Unable to get key %s from %s: %s", key, entry.getKey(), entry.getValue()));
            }
         }
         return versionsMap;
      });
      return waitForAllVersions(key, meta);
   }

   private Map<Address, InternalCacheValue<V>> waitForAllVersions(K key, VersionRequestMeta meta) {
      try {
         return meta.completableFuture.get();
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
         throw new CacheException(e);
      } catch (ExecutionException e) {
         if (e.getCause() instanceof CacheException)
            throw (CacheException) e.getCause();

         throw new CacheException(e.getCause());
      } finally {
         synchronized (versionRequestMap) {
            versionRequestMap.remove(key);
         }
      }
   }

   @Override
   public Stream<Map<Address, InternalCacheEntry<K, V>>> getConflicts() {
      if (stateConsumer.isStateTransferInProgress())
         throw log.getConflictsStateTransferInProgress(cacheName);

      return StreamSupport.stream(new ReplicaSpliterator(), false).filter(filterConsistentEntries());
   }

   @Override
   public boolean isResolutionPossible() throws Exception {
      return !stateConsumer.isStateTransferInProgress() &&
            localTopologyManager.getRebalancingStatus(cacheName).equals(RebalancingStatus.COMPLETE);
   }

   private class VersionRequestMeta {
      final int rebalanceId = cacheTopology.getRebalanceId();
      volatile CompletableFuture<Map<Address, InternalCacheValue<V>>> completableFuture;

      boolean isRequestInProgress() {
         return completableFuture != null && !completableFuture.isDone();
      }
   }

   private Predicate<? super Map<Address, InternalCacheEntry<K, V>>> filterConsistentEntries() {
      return map -> map.values().stream().distinct().limit(2).count() > 1 || map.values().isEmpty();
   }

   // We could make this work in parallel, however if we are receiving multiple segments simultaneously there is the
   // potential for an OutOfMemoryError depending on the total number of cache entries
   private class ReplicaSpliterator extends Spliterators.AbstractSpliterator<Map<Address, InternalCacheEntry<K, V>>> {
      private final int totalSegments;
      private int currentSegment = 0;
      private List<Map<Address, InternalCacheEntry<K, V>>> segmentEntries;
      private Iterator<Map<Address, InternalCacheEntry<K, V>>> iterator = Collections.emptyIterator();

      ReplicaSpliterator() {
         super(Long.MAX_VALUE, 0);

         if (!streamInProgress.compareAndSet(false, true))
            throw log.getConflictsAlreadyInProgress();

         this.totalSegments = distributionManager.getWriteConsistentHash().getNumSegments();
      }

      @Override
      public boolean tryAdvance(Consumer<? super Map<Address, InternalCacheEntry<K, V>>> action) {
         while (!iterator.hasNext()) {
            if (currentSegment >= totalSegments) {
               streamInProgress.compareAndSet(true, false);
               return false;
            }

            try {
               segmentEntries = stateReceiver.getAllReplicasForSegment(currentSegment).get();
               iterator = segmentEntries.iterator();
               currentSegment++;
            } catch (InterruptedException e) {
               streamInProgress.compareAndSet(true, false);
               Thread.currentThread().interrupt();
               throw new CacheException(e);
            } catch (ExecutionException | CancellationException e) {
               streamInProgress.compareAndSet(true, false);
               throw new CacheException(e.getMessage(), e.getCause()); // Should we throw exception? Or WARN and just continue to the next segment?
            }
         }
         action.accept(iterator.next());
         return true;
      }
   }
}
