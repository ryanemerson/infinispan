package org.infinispan.conflict.resolution;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.infinispan.statetransfer.StateTransferManager;
import org.infinispan.topology.CacheTopology;
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
   private StateReceiver<K, V> stateReceiver;
   private RpcManager rpcManager;
   private StateTransferManager stateTransferManager;
   private final AtomicBoolean streamInProgress = new AtomicBoolean();

   @Inject
   public void inject(Cache cache,
                      CommandsFactory commandsFactory,
                      DataContainer<K, V> dataContainer,
                      DistributionManager distributionManager,
                      StateReceiver<K, V> stateReceiver,
                      RpcManager rpcManager,
                      StateTransferManager stateTransferManager) {
      this.cache = cache;
      this.commandsFactory = commandsFactory;
      this.dataContainer = dataContainer;
      this.distributionManager = distributionManager;
      this.stateReceiver = stateReceiver;
      this.rpcManager = rpcManager;
      this.stateTransferManager = stateTransferManager;
   }

   @Override
   public Map<Address, InternalCacheValue<V>> getAllVersions(K key) {
      if (stateTransferManager.isStateTransferInProgress())
         throw log.getAllVersionsOfKeyDuringStateTransfer(key, cache.getName());

      final CacheTopology startTopology = stateTransferManager.getCacheTopology();
      List<Address> keyReplicaNodes = distributionManager.getWriteConsistentHash().locateOwners(key);
      Address localAddress = rpcManager.getAddress();
      Map<Address, InternalCacheValue<V>> versionsMap = new HashMap<>();
      if (keyReplicaNodes.contains(localAddress)) {
         InternalCacheValue<V> icv = dataContainer.containsKey(key) ? dataContainer.get(key).toInternalCacheValue() : null;
         versionsMap.put(localAddress, icv);
      }

      ClusteredGetCommand cmd = commandsFactory.buildClusteredGetCommand(key, FlagBitSets.SKIP_OWNERSHIP_CHECK);
      long timeout = cache.getCacheConfiguration().clustering().remoteTimeout();
      RpcOptions rpcOptions = new RpcOptions(timeout, TimeUnit.SECONDS, null, ResponseMode.SYNCHRONOUS_IGNORE_LEAVERS, DeliverOrder.NONE);
      CompletableFuture<Map<Address, Response>> future = rpcManager.invokeRemotelyAsync(keyReplicaNodes, cmd, rpcOptions);
      try {
         Map<Address, Response> responseMap = future.get();
         for (Map.Entry<Address, Response> entry : responseMap.entrySet()) {
            if (entry.getValue() instanceof SuccessfulResponse) {
               SuccessfulResponse response = (SuccessfulResponse) entry.getValue();
               versionsMap.put(entry.getKey(), (InternalCacheValue<V>) response.getResponseValue());
            } else {
               throw new CacheException(String.format("Unable to get key %s from %s: %s", key, entry.getKey(), entry.getValue()));
            }
         }
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
         throw new CacheException(e);
      } catch (ExecutionException e) {
         throw new CacheException(e.getCause());
      }

      CacheTopology currentTopology = stateTransferManager.getCacheTopology();
      if (!Objects.equals(currentTopology, startTopology) && currentTopology.getRebalanceId() > startTopology.getRebalanceId()) {
         throw log.getAllVersionsOfKeyTopologyChange(key, cache.getName());
      }
      return versionsMap;
   }

   @Override
   public Stream<Map<Address, InternalCacheEntry<K, V>>> getConflicts() {
      if (stateTransferManager.isStateTransferInProgress())
         throw log.getConflictsStateTransferInProgress(cache.getName());

      return StreamSupport.stream(new ReplicaSpliterator(), false).filter(filterConsistentEntries());
   }

   @Override
   public boolean isResolutionPossible() throws Exception {
      return !stateTransferManager.isStateTransferInProgress() &&
            stateTransferManager.getRebalancingStatus().equals(RebalancingStatus.COMPLETE.toString());
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
