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
import org.infinispan.commands.ReplicableCommand;
import org.infinispan.commands.read.GetCacheEntryCommand;
import org.infinispan.commands.remote.ClusteredGetCommand;
import org.infinispan.commands.write.PutKeyValueCommand;
import org.infinispan.commands.write.RemoveCommand;
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
import org.infinispan.distribution.DistributionInfo;
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
import org.infinispan.remoting.responses.UnsuccessfulResponse;
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
   private static final long remoteFlags = EnumUtil.bitSetOf(Flag.IGNORE_RETURN_VALUES, Flag.SKIP_OWNERSHIP_CHECK);

   private AsyncInterceptorChain interceptorChain;
   private AdvancedCache<K, V> cache;
   private CommandsFactory commandsFactory;
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
                      EntryMergePolicy<K, V> entryMergePolicy,
                      InvocationContextFactory invocationContextFactory,
                      KeyPartitioner keyPartitioner,
                      RpcManager rpcManager,
                      StateConsumer stateConsumer,
                      StateReceiver<K, V> stateReceiver) {
      this.interceptorChain = interceptorChain;
      this.cache = cache;
      this.commandsFactory = commandsFactory;
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
      // We have to manually create a LocalizedTopology here, as CacheNotifier::notifyTopologyChanged is called before
      // DistributionManager::setTopology and we always need to utilise the latest hash
      this.installedTopology = createLocalizedTopology(event.getConsistentHashAtEnd());
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
      doResolveConflicts(installedTopology, null);
   }

   public void resolveConflicts(PartitionMergeInfo mergeInfo) {
      LocalizedCacheTopology localizedCacheTopology = createLocalizedTopology(mergeInfo.getMergeHash());
      doResolveConflicts(localizedCacheTopology, mergeInfo);
   }

   private void doResolveConflicts(LocalizedCacheTopology topology, PartitionMergeInfo mergeInfo) {
      Set<Address> preferredPartition = getPreferredPartition(topology);

      if (trace) log.tracef("Attempting to resolve conflicts.  All Members %s, Installed topology %s, Preferred Partition %s",
            mergeInfo.getMergeHash().getMembers(), installedTopology, preferredPartition);

      int conflictsResolved = 0;
      Iterator<Map<Address, InternalCacheEntry<K, V>>> it = getConflicts(topology).iterator();
      while (it.hasNext()) {
         Map<Address, InternalCacheEntry<K, V>> conflictMap = it.next();
         if (trace) log.tracef("Conflict detected %s", conflictMap);

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

         if (trace) log.tracef("Applying EntryMergePolicy %s to PreferredEntry %s, otherEntries %s",
               entryMergePolicy.getClass(), preferredEntry, entries);

         CacheEntry<K, V> entry = preferredEntry instanceof NullValueEntry ? null : preferredEntry;
         List<CacheEntry<K, V>> otherEntries = entries.stream().filter(e -> !(e instanceof NullValueEntry)).collect(Collectors.toList());
         CacheEntry<K, V> mergedEntry = entryMergePolicy.merge(entry, otherEntries);
         if (mergedEntry == null) {
            removeCacheEntry(key, topology, mergeInfo);
         } else {
            updateCacheEntries(mergedEntry, topology, mergeInfo);
         }
         conflictsResolved++;
      }

      if (trace) log.tracef("Finished attempting to resolve %s conflicts", conflictsResolved);
   }

   private LocalizedCacheTopology createLocalizedTopology(ConsistentHash hash) {
      CacheMode mode = cache.getCacheConfiguration().clustering().cacheMode();
      CacheTopology topology = new CacheTopology(-1,  -1, hash, null, hash.getMembers(), null);
      return new LocalizedCacheTopology(mode, topology, keyPartitioner, rpcManager.getAddress());
   }

   private Set<Address> getPreferredPartition(LocalizedCacheTopology topology) {
      // If mergeHash is the same object as writeConsistentHash, no merge has occurred, so current members are used
      Set<Address> currentPartition = new HashSet<>(installedTopology.getMembers());
      if (!isPartitionMerge(topology))
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

   private void updateCacheEntries(CacheEntry<K,V> entry, LocalizedCacheTopology topology, PartitionMergeInfo mergeInfo) {
      if (trace) log.tracef("Updating key %s with entry %s", entry.getKey(), entry);
      // If no partitions, then just perform update as normal put command using the preferred entries metadata
      if (!isPartitionMerge(topology)) {
         if (trace) log.tracef("No partition merge has occurred, putting entry %s with metadata as normal cache operation", entry);
         cache.put(entry.getKey(), entry.getValue(), entry.getMetadata());
         return;
      }

      // Here we update the local entries for all NEW owners as defined in the merged hash
      // Entries that are no longer valid, i.e. an entry stored on a node which is no longer an owner, are removed during ST
      // TODO? Possible optimisation is restrict the update below to the new primary owner and let ST handle the rest

      DistributionInfo distributionInfo = topology.getDistribution(entry.getKey());
      Set<Address> keyOwners = new HashSet<>(distributionInfo.writeOwners());
      if (distributionInfo.isWriteOwner()) {
         if (trace) log.tracef("This node %s is a write owner for key %s so putting locally", rpcManager.getAddress(), entry.getKey());

         keyOwners.remove(rpcManager.getAddress());
         PutKeyValueCommand cmd = commandsFactory.buildPutKeyValueCommand(entry.getKey(), entry.getValue(), entry.getMetadata(), localFlags);
         InvocationContext ctx = invocationContextFactory.createNonTxInvocationContext();
         interceptorChain.invoke(ctx, cmd);
      }

      Map<Address, ReplicableCommand> cmdMap = new HashMap<>();
      for (Address address : keyOwners) {
         int topologyId = mergeInfo.getTopologyId(address);
         PutKeyValueCommand cmd = commandsFactory.buildPutKeyValueCommand(entry.getKey(), entry.getValue(), entry.getMetadata(), remoteFlags);
         cmd.setTopologyId(topologyId); // Set to max stable topology id received for destination node
         cmdMap.put(address, cmd);

         if (trace) log.tracef("Putting entry for key %s at address %s with topology id %s", entry, address, topologyId);
      }

      Map<Address, Response> responseMap = rpcManager.invokeRemotely(cmdMap, rpcOptions);
      responseMap.entrySet().stream()
            .filter(e -> !e.getValue().isSuccessful())
            .forEach(e -> log.warnf("Unable to update %s with entry %s, due to %s", e.getKey(), entry,
                  ((UnsuccessfulResponse) e.getValue()).getResponseValue()));
   }

   private void removeCacheEntry(K key, LocalizedCacheTopology topology, PartitionMergeInfo mergeInfo) {
      if (trace) log.tracef("Removing key %s", key);

      if (!isPartitionMerge(topology)) {
         if (trace) log.tracef("No partition merge has occurred, removing key %s as normal cache operation", key);
         cache.remove(key);
         return;
      }

      DistributionInfo distributionInfo = topology.getDistribution(key);
      Set<Address> keyOwners = new HashSet<>(distributionInfo.writeOwners());
      if (distributionInfo.isWriteOwner()) {
         if (trace) log.tracef("This node %s is a write owner for key %s so removing locally", rpcManager.getAddress(), key);

         keyOwners.remove(rpcManager.getAddress());
         RemoveCommand cmd = commandsFactory.buildRemoveCommand(key, null, localFlags);
         InvocationContext ctx = invocationContextFactory.createNonTxInvocationContext();
         interceptorChain.invoke(ctx, cmd);
      }

      Map<Address, ReplicableCommand> cmdMap = new HashMap<>();
      for (Address address : keyOwners) {
         int topologyId = mergeInfo.getTopologyId(address);
         RemoveCommand cmd = commandsFactory.buildRemoveCommand(key, null, remoteFlags);
         cmd.setTopologyId(topologyId); // Set to max stable topology id received for destination node
         cmdMap.put(address, cmd);

         if (trace) log.tracef("Removing key %s at address %s with topology id %s", key, address, topologyId);
      }

      Map<Address, Response> responseMap = rpcManager.invokeRemotely(cmdMap, rpcOptions);
      responseMap.entrySet().stream()
            .filter(e -> !e.getValue().isSuccessful())
            .forEach(e -> log.warnf("Unable to remove entry %s from %s, due to %s", key, e.getKey(),
                  ((UnsuccessfulResponse) e.getValue()).getResponseValue()));
   }

   private boolean isPartitionMerge(LocalizedCacheTopology topology) {
      return topology != installedTopology;
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

         if (trace) log.tracef("Requesting all versions of key %s from owners %s", key, keyOwners);

         final Map<Address, InternalCacheValue<V>> versionsMap = new HashMap<>();
         if (keyOwners.contains(localAddress)) {
            GetCacheEntryCommand cmd = commandsFactory.buildGetCacheEntryCommand(key, localFlags);
            InvocationContext ctx = invocationContextFactory.createNonTxInvocationContext();
            InternalCacheEntry<K, V> internalCacheEntry = (InternalCacheEntry<K, V>) interceptorChain.invoke(ctx, cmd);

            if (internalCacheEntry != null)
               versionsMap.put(localAddress, internalCacheEntry.toInternalCacheValue());
//            InternalCacheValue<V> icv = internalCacheEntry == null ? null : internalCacheEntry.toInternalCacheValue();
//            versionsMap.put(localAddress, icv);
         }

         ClusteredGetCommand cmd = commandsFactory.buildClusteredGetCommand(key, FlagBitSets.SKIP_OWNERSHIP_CHECK);
         rpcFuture = rpcManager.invokeRemotelyAsync(keyOwners, cmd, rpcOptions);
         rpcFuture.whenComplete((responseMap, exception) -> {
            if (exception != null) {
               if (trace) log.tracef("Exception encountered when requesting all versions of key %s", key);

               String msg = String.format("%s encountered when trying to receive all versions of key '%s' on cache '%s'",
                     exception.getCause(), key, cacheName);
               completableFuture.completeExceptionally(new CacheException(msg, exception.getCause()));
            }

            for (Map.Entry<Address, Response> entry : responseMap.entrySet()) {
               if (trace) log.tracef("Version request received response %s from %s", entry.getValue(), entry.getKey());
               if (entry.getValue() instanceof SuccessfulResponse) {
                  SuccessfulResponse response = (SuccessfulResponse) entry.getValue();
                  Object rspVal = response.getResponseValue();
                  versionsMap.put(entry.getKey(), (InternalCacheValue<V>) rspVal);
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

   // TODO make this work in parallel, to improve performance during merge with many entries
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
               if (trace) log.tracef("Attempting to receive all replicas for segment %s with topology %s", currentSegment, topology);
               segmentEntries = stateReceiver.getAllReplicasForSegment(currentSegment, topology).get();
               if (trace) log.tracef("Segment %s entries received: %s", currentSegment, segmentEntries);
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
