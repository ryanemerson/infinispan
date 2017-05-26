package org.infinispan.conflict.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Spliterator;
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
import org.infinispan.commons.util.EnumUtil;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ClusteringConfiguration;
import org.infinispan.conflict.ConflictManager;
import org.infinispan.conflict.EntryMergePolicy;
import org.infinispan.container.entries.CacheEntry;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.container.entries.InternalCacheValue;
import org.infinispan.context.Flag;
import org.infinispan.context.InvocationContext;
import org.infinispan.context.InvocationContextFactory;
import org.infinispan.context.impl.FlagBitSets;
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
   private static boolean trace = log.isTraceEnabled();

   private static final long localFlags = EnumUtil.bitSetOf(Flag.CACHE_MODE_LOCAL, Flag.SKIP_OWNERSHIP_CHECK, Flag.SKIP_LOCKING);

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
      this.cache = cache.withFlags(Flag.IGNORE_RETURN_VALUES, Flag.PUT_FOR_STATE_TRANSFER);
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
      // We have to manually create a LocalizedTopology here, as CacheNotifier::notifyTopologyChanged is called before
      // DistributionManager::setTopology and we always need to utilise the latest hash
      if (trace) log.tracef("Installed new topology %s: %s", event.getNewTopologyId(), event.getWriteConsistentHashAtEnd());
      this.installedTopology = distributionManager.getCacheTopology();
   }

   @DataRehashed
   @SuppressWarnings("unused")
   public void onDataRehashed(DataRehashedEvent<K, V> event) {
      if (event.isPre() && isStateTransferInProgress()) {
         // We know that a rehash is about to occur, so postpone all requests which are affected
         synchronized (versionRequestMap) {
            versionRequestMap.values().forEach(VersionRequest::cancelRequestIfOutdated);
         }
      } else if (!event.isPre() && event.getPhase() == CacheTopology.Phase.READ_ALL_WRITE_ALL) {
         this.installedTopology = createLocalizedTopology(event.getConsistentHashAtEnd());

         // If phase is READ_ALL_WRITE_ALL, we know that ST has finished and that it is safe to retry postponed version requests
         VersionRequest request;
         while ((request = retryQueue.poll()) != null) {
            if (trace) log.tracef("Retrying %s", request);
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

      if (!streamInProgress.compareAndSet(false, true))
         throw log.getConflictsAlreadyInProgress();

      try {
         return StreamSupport
               .stream(new ReplicaSpliterator(topology), false)
               .filter(filterConsistentEntries());
      } finally {
         streamInProgress.compareAndSet(true, false);
      }
   }

   @Override
   public boolean isConflictResolutionInProgress() {
      return streamInProgress.get();
   }

   @Override
   public void resolveConflicts() {
      resolveConflicts(installedTopology);
   }

   public void resolveConflicts(CacheTopology topology) {
      LocalizedCacheTopology localizedTopology;
      if (topology instanceof LocalizedCacheTopology) {
         localizedTopology = (LocalizedCacheTopology) topology;
      } else {
         localizedTopology = createLocalizedTopology(topology);
      }
      doResolveConflicts(localizedTopology);
   }

   private void doResolveConflicts(final LocalizedCacheTopology topology) {
      final Set<Address> preferredPartition = new HashSet<>(topology.getCurrentCH().getMembers());

      if (trace) log.tracef("Attempting to resolve conflicts.  All Members %s, Installed topology %s, Preferred Partition %s",
            topology.getMembers(), topology, preferredPartition);

      List<CompletableFuture<V>> completableFutures =
            getConflicts(topology)
                  .map(conflictMap -> processConflict(conflictMap, topology, preferredPartition))
                  .collect(Collectors.toList());
      try {
         CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[completableFutures.size()])).get();
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
         throw new CacheException(e);
      } catch (ExecutionException | CancellationException e) {
         stateReceiver.stop();
         throw new CacheException(e.getMessage(), e.getCause());
      }
   }

   private CompletableFuture<V> processConflict(Map<Address, InternalCacheEntry<K, V>> conflictMap,
                                                LocalizedCacheTopology topology, Set<Address> preferredPartition) {
      if (trace) log.tracef("Conflict detected %s", conflictMap);

      Collection<InternalCacheEntry<K, V>> entries = conflictMap.values();
      final K key = entries.iterator().next().getKey();
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

      if (trace) log.tracef("Applying EntryMergePolicy %s to PreferredEntry %s, otherEntries %s",
            entryMergePolicy.getClass(), preferredEntry, entries);

      CacheEntry<K, V> entry = preferredEntry instanceof NullValueEntry ? null : preferredEntry;
      List<CacheEntry<K, V>> otherEntries = entries.stream().filter(e -> !(e instanceof NullValueEntry)).collect(Collectors.toList());
      CacheEntry<K, V> mergedEntry = entryMergePolicy.merge(entry, otherEntries);

      CompletableFuture<V> future;
      if (mergedEntry == null) {
         if (trace) log.tracef("Executing remove on conflict: key %s", key);
         future = cache.removeAsync(key);
      } else {
         if (trace) log.tracef("Executing update on conflict: key %s with entry %s", key, entry);
         future = cache.putAsync(key, mergedEntry.getValue(), mergedEntry.getMetadata());
      }
      future.exceptionally(t -> {
         log.exceptionDuringConflictResolution(key, t);
         return null;
      });
      return future;
   }

   private LocalizedCacheTopology createLocalizedTopology(ConsistentHash hash) {
      CacheTopology topology = new CacheTopology(-1, -1, hash, null,
            CacheTopology.Phase.NO_REBALANCE, hash.getMembers(), null);
      return createLocalizedTopology(topology);
   }

   private LocalizedCacheTopology createLocalizedTopology(CacheTopology topology) {
      CacheMode mode = cache.getCacheConfiguration().clustering().cacheMode();
      return new LocalizedCacheTopology(mode, topology, keyPartitioner, rpcManager.getAddress());
   }

   @Override
   public boolean isStateTransferInProgress() {
      return stateConsumer.isStateTransferInProgress();
   }

   private class VersionRequest {
      final K key;
      final boolean postpone;
      final CompletableFuture<Map<Address, InternalCacheValue<V>>> completableFuture = new CompletableFuture<>();
      volatile CompletableFuture<Map<Address, Response>> rpcFuture;
      volatile Collection<Address> keyOwners;

      VersionRequest(K key, boolean postpone) {
         this.key = key;
         this.postpone = postpone;

         if (trace) log.tracef("Creating %s", this);

         if (postpone) {
            retryQueue.add(this);
         } else {
            start();
         }
      }

      void cancelRequestIfOutdated() {
         Collection<Address> latestOwners = installedTopology.getWriteOwners(key);
         if (rpcFuture != null && !completableFuture.isDone() && !keyOwners.equals(latestOwners)) {
            rpcFuture = null;
            keyOwners.clear();
            if (rpcFuture.cancel(false)) {
               retryQueue.add(this);

               if (trace) log.tracef("Cancelling %s for nodes %s. New write owners %s", this, keyOwners, latestOwners);
            }
         }
      }

      void start() {
         keyOwners = installedTopology.getWriteOwners(key);

         if (trace) log.tracef("Attempting %s from owners %s", this, keyOwners);

         final Map<Address, InternalCacheValue<V>> versionsMap = new HashMap<>();
         if (keyOwners.contains(localAddress)) {
            GetCacheEntryCommand cmd = commandsFactory.buildGetCacheEntryCommand(key, localFlags);
            InvocationContext ctx = invocationContextFactory.createNonTxInvocationContext();
            InternalCacheEntry<K, V> internalCacheEntry = (InternalCacheEntry<K, V>) interceptorChain.invoke(ctx, cmd);
            InternalCacheValue<V> icv = internalCacheEntry == null ? null : internalCacheEntry.toInternalCacheValue();
            versionsMap.put(localAddress, icv);
         }

         ClusteredGetCommand cmd = commandsFactory.buildClusteredGetCommand(key, FlagBitSets.SKIP_OWNERSHIP_CHECK);
         rpcFuture = rpcManager.invokeRemotelyAsync(keyOwners, cmd, rpcOptions);
         rpcFuture.whenComplete((responseMap, exception) -> {
            if (trace) log.tracef("%s received responseMap %s, exception %s", this, responseMap, exception);

            if (exception != null) {
               String msg = String.format("%s encountered when attempting '%s' on cache '%s'", exception.getCause(), this, cacheName);
               completableFuture.completeExceptionally(new CacheException(msg, exception.getCause()));
               return;
            }

            for (Map.Entry<Address, Response> entry : responseMap.entrySet()) {
               if (trace) log.tracef("%s received response %s from %s", this, entry.getValue(), entry.getKey());
               if (entry.getValue() instanceof SuccessfulResponse) {
                  SuccessfulResponse response = (SuccessfulResponse) entry.getValue();
                  Object rspVal = response.getResponseValue();
                  versionsMap.put(entry.getKey(), (InternalCacheValue<V>) rspVal);
               } else {
                  completableFuture.completeExceptionally(new CacheException(String.format("Unable to retrieve key %s from %s: %s", key, entry.getKey(), entry.getValue())));
                  return;
               }
            }
            completableFuture.complete(versionsMap);
         });
      }

      @Override
      public String toString() {
         return "VersionRequest{" +
               "key=" + key +
               ", postpone=" + postpone +
               '}';
      }
   }

   private Predicate<? super Map<Address, InternalCacheEntry<K, V>>> filterConsistentEntries() {
      return map -> map.values().stream().distinct().limit(2).count() > 1 || map.values().isEmpty();
   }

   private class ReplicaSpliterator implements Spliterator<Map<Address, InternalCacheEntry<K, V>>> {
      private final LocalizedCacheTopology topology;
      private int totalSegments, currentSegment;
      private Iterator<Map<Address, InternalCacheEntry<K, V>>> iterator = Collections.emptyIterator();

      ReplicaSpliterator(LocalizedCacheTopology topology) {
         this(topology, 0, topology.getWriteConsistentHash().getNumSegments());
      }

      ReplicaSpliterator(LocalizedCacheTopology topology, int currentSegment, int totalSegments) {
         this.topology = topology;
         this.currentSegment = currentSegment;
         this.totalSegments = totalSegments;
      }

      @Override
      public boolean tryAdvance(Consumer<? super Map<Address, InternalCacheEntry<K, V>>> action) {
         while (!iterator.hasNext()) {
            if (currentSegment < totalSegments) {
               try {
                  if (trace)
                     log.tracef("Attempting to receive all replicas for segment %s with topology %s", currentSegment, topology);
                  List<Map<Address, InternalCacheEntry<K, V>>> segmentEntries = stateReceiver.getAllReplicasForSegment(currentSegment, topology).get();
                  if (trace && !segmentEntries.isEmpty())
                     log.tracef("Segment %s entries received: %s", currentSegment, segmentEntries);
                  iterator = segmentEntries.iterator();
                  currentSegment++;
               } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                  throw new CacheException(e);
               } catch (ExecutionException | CancellationException e) {
                  stateReceiver.stop();
                  throw new CacheException(e.getMessage(), e.getCause());
               }
            } else {
               return false;
            }
         }
         action.accept(iterator.next());
         return true;
      }

      @Override
      public Spliterator<Map<Address, InternalCacheEntry<K, V>>> trySplit() {
         int lo = currentSegment;
         int mid = (lo + totalSegments) >>> 1;
         if (lo < mid) {
            currentSegment = mid;
            return new ReplicaSpliterator(topology, lo, mid);
         }
         return null;
      }

      @Override
      public long estimateSize() {
         return Long.MAX_VALUE;
      }

      @Override
      public int characteristics() {
         return 0;
      }
   }
}
