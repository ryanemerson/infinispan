package org.infinispan.conflict.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Spliterators;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.infinispan.AdvancedCache;
import org.infinispan.commands.CommandsFactory;
import org.infinispan.commands.read.GetCacheEntryCommand;
import org.infinispan.commands.remote.ClusteredGetCommand;
import org.infinispan.commons.CacheException;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ClusteringConfiguration;
import org.infinispan.conflict.ConflictManager;
import org.infinispan.conflict.EntryMergePolicy;
import org.infinispan.container.entries.CacheEntry;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.container.entries.InternalCacheValue;
import org.infinispan.context.InvocationContext;
import org.infinispan.context.InvocationContextFactory;
import org.infinispan.context.impl.FlagBitSets;
import org.infinispan.distribution.DistributionInfo;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.distribution.LocalizedCacheTopology;
import org.infinispan.distribution.ch.ConsistentHash;
import org.infinispan.distribution.ch.KeyPartitioner;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.factories.annotations.Start;
import org.infinispan.interceptors.AsyncInterceptorChain;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.DataRehashed;
import org.infinispan.notifications.cachelistener.annotation.TopologyChanged;
import org.infinispan.notifications.cachelistener.event.DataRehashedEvent;
import org.infinispan.notifications.cachelistener.event.TopologyChangedEvent;
import org.infinispan.remoting.responses.Response;
import org.infinispan.remoting.responses.SuccessfulResponse;
import org.infinispan.remoting.rpc.ResponseMode;
import org.infinispan.remoting.rpc.RpcManager;
import org.infinispan.remoting.rpc.RpcOptions;
import org.infinispan.remoting.transport.Address;
import org.infinispan.statetransfer.StateConsumer;
import org.infinispan.topology.CacheTopology;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * @author Ryan Emerson
 */
@Listener(observation = Listener.Observation.BOTH)
public class DefaultConflictManager<K, V> implements ConflictManager<K, V> {

   private static Log log = LogFactory.getLog(DefaultConflictManager.class);

   private AsyncInterceptorChain interceptorChain;
   private AdvancedCache<K, V> cache;
   private CommandsFactory commandsFactory;
   private DistributionManager distributionManager;
   private EntryMergePolicy<K, V> entryMergePolicy;
   private InvocationContextFactory invocationContextFactory;
   private KeyPartitioner keyPartitioner;
   private RpcManager rpcManager;
   private StateConsumer stateConsumer;
   private StateReceiver<K, V> stateReceiver;
   private String cacheName;
   private Address localAddress;
   private RpcOptions rpcOptions;
   private final AtomicBoolean streamInProgress = new AtomicBoolean();
   private final Map<K, VersionRequest> versionRequestMap = new HashMap<>();
   private final Queue<VersionRequest> retryQueue = new ConcurrentLinkedQueue<>();
   private volatile LocalizedCacheTopology installedTopology;

   @Inject
   public void inject(AsyncInterceptorChain interceptorChain,
                      AdvancedCache<K, V> cache,
                      CommandsFactory commandsFactory,
                      DistributionManager distributionManager,
                      EntryMergePolicy<K, V> entryMergePolicy,
                      InvocationContextFactory invocationContextFactory,
                      KeyPartitioner keyPartitioner,
                      RpcManager rpcManager,
                      StateConsumer stateConsumer,
                      StateReceiver<K, V> stateReceiver) {
      this.interceptorChain = interceptorChain;
      this.cache = cache;
      this.commandsFactory = commandsFactory;
      this.distributionManager = distributionManager;
      this.entryMergePolicy = entryMergePolicy;
      this.invocationContextFactory = invocationContextFactory;
      this.keyPartitioner = keyPartitioner;
      this.rpcManager = rpcManager;
      this.stateConsumer = stateConsumer;
      this.stateReceiver = stateReceiver;
   }

   @Start
   public void start() {
      this.cache.addListener(this);
      this.cacheName = cache.getName();
      this.localAddress = rpcManager.getAddress();

      initRpcOptions();
      cache.getCacheConfiguration().clustering()
            .attributes().attribute(ClusteringConfiguration.REMOTE_TIMEOUT)
            .addListener(((a, o) -> initRpcOptions()));
   }

   private void initRpcOptions() {
      this.rpcOptions = rpcManager.getRpcOptionsBuilder(ResponseMode.SYNCHRONOUS_IGNORE_LEAVERS).build();
   }

   @TopologyChanged
   @SuppressWarnings("unused")
   public void onTopologyChanged(TopologyChangedEvent<K, V> event) {
      this.installedTopology = distributionManager.getCacheTopology();
   }

   @DataRehashed
   @SuppressWarnings("unused")
   public void onDataRehashed(DataRehashedEvent<K, V> event) {
      if (event.isPre()) {
         // We know that a rehash is about to occur, so postpone all requests which are affected
         synchronized (versionRequestMap) {
            versionRequestMap.values().forEach(VersionRequest::cancelRequestIfOutdated);
         }
      } else {
         // Rehash has finished, so restart postponed requests
         VersionRequest request;
         while ((request = retryQueue.poll()) != null) {
            request.start();
         }
      }
   }

   @Override
   public Map<Address, InternalCacheValue<V>> getAllVersions(final K key) {
      final VersionRequest request;
      synchronized (versionRequestMap) {
         request = versionRequestMap.computeIfAbsent(key, k -> new VersionRequest(k, stateConsumer.isStateTransferInProgress()));
      }

      try {
         return request.completableFuture.get();
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
      return getConflicts(installedTopology);
   }

   private Stream<Map<Address, InternalCacheEntry<K, V>>> getConflicts(LocalizedCacheTopology topology) {
      if (stateConsumer.isStateTransferInProgress())
         throw log.getConflictsStateTransferInProgress(cacheName);

      return StreamSupport.stream(new ReplicaSpliterator(topology), false).filter(filterConsistentEntries());
   }

   @Override
   public void resolveConflicts() {
      doResolveConflicts(installedTopology);
   }

   public void resolveConflicts(ConsistentHash mergeHash) {
      CacheMode mode = cache.getCacheConfiguration().clustering().cacheMode();
      CacheTopology topology = new CacheTopology(-1,  -1, mergeHash, null, mergeHash.getMembers(), null);
      LocalizedCacheTopology localizedCacheTopology = new LocalizedCacheTopology(mode, topology, keyPartitioner, rpcManager.getAddress());
      doResolveConflicts(localizedCacheTopology);
   }

   private void doResolveConflicts(LocalizedCacheTopology topology) {
      Set<Address> preferredPartition = getPreferredPartition(topology);

      System.out.println("Topology: " + installedTopology);
      System.out.println("PreferredPartition: " + preferredPartition);

      getConflicts(topology).forEach(conflictMap -> {
         Collection<InternalCacheEntry<K, V>> entries = conflictMap.values();
         K key = entries.iterator().next().getKey();
         Address primaryReplica = topology.getDistribution(key).primary();

         List<Address> preferredEntries = conflictMap.entrySet().stream()
               .map(Map.Entry::getKey)
               .filter(preferredPartition::contains)
               .collect(Collectors.toList());

         // If only one entry exists in the preferred partition, then use that entry
         CacheEntry<K, V> preferredEntry;
         if (preferredEntries.size() == 1) {
            preferredEntry = conflictMap.remove(preferredEntries.get(0));
         } else {
            // If multiple conflicts exist in the preferred partition, then use primary replica from the preferred partition
            // If not a merge, then also use primary as preferred entry
            // Preferred is null if no entry exists in preferred partition
            preferredEntry = conflictMap.remove(primaryReplica);
         }

         System.out.println("Preferred ENtry: " + preferredEntry);

         CacheEntry<K, V> mergedEntry = entryMergePolicy.merge(preferredEntry, new ArrayList<>(entries));
         updateCacheEntries(mergedEntry, null);
      });
   }

   private Set<Address> getPreferredPartition(LocalizedCacheTopology topology) {
      // If mergeHash is the same object as writeConsistentHash, no merge has occurred, so current members are used
      Set<Address> currentPartition = new HashSet<>(installedTopology.getMembers());
      if (topology == installedTopology)
         return currentPartition;

      Set<Address> joiningPartition = new HashSet<>(topology.getMembers());
      joiningPartition.removeAll(currentPartition);

      // Pick the larger partition, but if equal, always pick the partition that contains the transport's coordinator
      if (joiningPartition.size() < currentPartition.size()) {
         return currentPartition;
      } else if (joiningPartition.size() > currentPartition.size()){
         return joiningPartition;
      } else {
         Address coordinator = rpcManager.getTransport().getCoordinator();
         return currentPartition.contains(coordinator) ? currentPartition : joiningPartition;
      }
   }

   private void updateCacheEntries(CacheEntry<K,V> entry, Address primary) {
      AsyncInterceptorChain ic = interceptorChain;
      System.out.println(ic);
   }

   @Override
   public boolean isStateTransferInProgress() throws Exception {
      return stateConsumer.isStateTransferInProgress();
   }

   private class VersionRequest {
      final K key;
      final CompletableFuture<Map<Address, InternalCacheValue<V>>> completableFuture = new CompletableFuture<>();
      volatile CompletableFuture<Map<Address, Response>> rpcFuture;
      volatile List<Address> keyOwners;

      VersionRequest(K key, boolean postpone) {
         this.key = key;

         if (postpone) {
            retryQueue.add(this);
         } else {
            start();
         }
      }

      void cancelRequestIfOutdated() {
         DistributionInfo distributionInfo = installedTopology.getDistribution(key);
         if (rpcFuture != null && !completableFuture.isDone() && !keyOwners.equals(distributionInfo.writeOwners())) {
            rpcFuture = null;
            keyOwners.clear();
            if (rpcFuture.cancel(false))
               retryQueue.add(this);
         }
      }

      void start() {
         DistributionInfo distributionInfo = installedTopology.getDistribution(key);
         keyOwners = distributionInfo.writeOwners();
         final Map<Address, InternalCacheValue<V>> versionsMap = new HashMap<>();
         if (keyOwners.contains(localAddress)) {
            GetCacheEntryCommand cmd = commandsFactory.buildGetCacheEntryCommand(key, FlagBitSets.CACHE_MODE_LOCAL);
            InvocationContext ctx = invocationContextFactory.createNonTxInvocationContext();
            InternalCacheEntry<K, V> internalCacheEntry = (InternalCacheEntry<K, V>) interceptorChain.invoke(ctx, cmd);
            InternalCacheValue<V> icv = internalCacheEntry != null ? internalCacheEntry.toInternalCacheValue() : null;
            versionsMap.put(localAddress, icv);
         }

         ClusteredGetCommand cmd = commandsFactory.buildClusteredGetCommand(key, FlagBitSets.SKIP_OWNERSHIP_CHECK);
         rpcFuture = rpcManager.invokeRemotelyAsync(keyOwners, cmd, rpcOptions);
         rpcFuture.whenComplete((responseMap, exception) -> {
            if (exception != null) {
               String msg = String.format("%s encountered when trying to receive all versions of key '%s' on cache '%s'",
                     exception.getCause(), key, cacheName);
               completableFuture.completeExceptionally(new CacheException(msg, exception.getCause()));
            }

            for (Map.Entry<Address, Response> entry : responseMap.entrySet()) {
               if (entry.getValue() instanceof SuccessfulResponse) {
                  SuccessfulResponse response = (SuccessfulResponse) entry.getValue();
                  versionsMap.put(entry.getKey(), (InternalCacheValue<V>) response.getResponseValue());
               } else {
                  completableFuture.completeExceptionally(new CacheException(String.format("Unable to retrieve key %s from %s: %s", key, entry.getKey(), entry.getValue())));
               }
            }
            completableFuture.complete(versionsMap);
         });
      }
   }

   private Predicate<? super Map<Address, InternalCacheEntry<K, V>>> filterConsistentEntries() {
      return map -> map.values().stream().distinct().limit(2).count() > 1 || map.values().isEmpty();
   }

   // We could make this work in parallel, however if we are receiving multiple segments simultaneously there is the
   // potential for an OutOfMemoryError depending on the total number of cache entries
   private class ReplicaSpliterator extends Spliterators.AbstractSpliterator<Map<Address, InternalCacheEntry<K, V>>> {
      private final LocalizedCacheTopology topology;
      private final int totalSegments;
      private int currentSegment = 0;
      private List<Map<Address, InternalCacheEntry<K, V>>> segmentEntries;
      private Iterator<Map<Address, InternalCacheEntry<K, V>>> iterator = Collections.emptyIterator();

      ReplicaSpliterator(LocalizedCacheTopology topology) {
         super(Long.MAX_VALUE, 0);

         if (!streamInProgress.compareAndSet(false, true))
            throw log.getConflictsAlreadyInProgress();

         this.topology = topology;
         this.totalSegments = topology.getWriteConsistentHash().getNumSegments();
      }

      @Override
      public boolean tryAdvance(Consumer<? super Map<Address, InternalCacheEntry<K, V>>> action) {
         while (!iterator.hasNext()) {
            if (currentSegment >= totalSegments) {
               streamInProgress.compareAndSet(true, false);
               return false;
            }

            try {
               segmentEntries = stateReceiver.getAllReplicasForSegment(currentSegment, topology).get();
               iterator = segmentEntries.iterator();
               currentSegment++;
            } catch (InterruptedException e) {
               streamInProgress.compareAndSet(true, false);
               Thread.currentThread().interrupt();
               throw new CacheException(e);
            } catch (ExecutionException | CancellationException e) {
               streamInProgress.compareAndSet(true, false);
               throw new CacheException(e.getMessage(), e.getCause());
            }
         }
         action.accept(iterator.next());
         return true;
      }
   }
}
