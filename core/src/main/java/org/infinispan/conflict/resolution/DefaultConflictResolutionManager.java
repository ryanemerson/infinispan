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
import org.infinispan.commons.util.EnumUtil;
import org.infinispan.container.DataContainer;
import org.infinispan.container.entries.InternalCacheValue;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.distribution.ch.ConsistentHash;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.remoting.inboundhandler.DeliverOrder;
import org.infinispan.remoting.responses.Response;
import org.infinispan.remoting.responses.SuccessfulResponse;
import org.infinispan.remoting.rpc.ResponseMode;
import org.infinispan.remoting.rpc.RpcManager;
import org.infinispan.remoting.rpc.RpcOptions;
import org.infinispan.remoting.transport.Address;

/**
 * @author Ryan Emerson
 */
public class DefaultConflictResolutionManager<K, V> implements ConflictResolutionManager<K, V> {

   private Cache cache;
   private CommandsFactory commandsFactory;
   private DataContainer<K, V> dataContainer;
   private DistributionManager distributionManager;
   private StateReceiver<V> stateReceiver;
   private RpcManager rpcManager;
   private final AtomicBoolean streamInProgress = new AtomicBoolean();

   @Inject
   public void inject(Cache cache,
                      CommandsFactory commandsFactory,
                      DataContainer<K, V> dataContainer,
                      DistributionManager distributionManager,
                      StateReceiver<V> stateReceiver,
                      RpcManager rpcManager) {
      this.cache = cache;
      this.commandsFactory = commandsFactory;
      this.dataContainer = dataContainer;
      this.distributionManager = distributionManager;
      this.stateReceiver = stateReceiver;
      this.rpcManager = rpcManager;
   }

   @Override
   public Map<Address, InternalCacheValue<V>> getAllVersions(K key) {
      ConsistentHash hash = distributionManager.getConsistentHash();
      Address localAddress = rpcManager.getAddress();
      Map<Address, InternalCacheValue<V>> versionsMap = new HashMap<>();
      if (hash.getMembers().contains(localAddress)) {
         InternalCacheValue<V> icv = dataContainer.containsKey(key) ? dataContainer.get(key).toInternalCacheValue() : null;
         versionsMap.put(localAddress, icv);
      }

      // TODO add timeout configuration
      ClusteredGetCommand cmd = commandsFactory.buildClusteredGetCommand(key, EnumUtil.EMPTY_BIT_SET);
      RpcOptions rpcOptions = new RpcOptions(15, TimeUnit.SECONDS, null, ResponseMode.SYNCHRONOUS_IGNORE_LEAVERS, DeliverOrder.NONE);
      CompletableFuture<Map<Address, Response>> future = rpcManager.invokeRemotelyAsync(hash.getMembers(), cmd, rpcOptions);
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
      return versionsMap;
   }

   @Override
   public Stream<Map<Address, InternalCacheValue<V>>> getConflicts() {
      return StreamSupport.stream(new ReplicaSpliterator(), false).filter(filterConsistentEntries());
   }

   @Override
   public void resolveConflict(K key) {
      throw new UnsupportedOperationException("Not yet implemented");
   }

   @Override
   public void resolveConflicts() {
      throw new UnsupportedOperationException("Not yet implemented");
   }

   private Predicate<? super Map<Address, InternalCacheValue<V>>> filterConsistentEntries() {
      return map -> map.values().stream().distinct().limit(2).count() > 1 || map.values().isEmpty();
   }

   // We could make this work in parallel, however if we are receiving multiple segments simultaneously there is the
   // potential for an OutOfMemoryError depending on the total number of cache entries
   private class ReplicaSpliterator extends Spliterators.AbstractSpliterator<Map<Address, InternalCacheValue<V>>> {
      private final int totalSegments;
      private int currentSegment = 0;
      private List<Map<Address, InternalCacheValue<V>>> segmentEntries;
      private Iterator<Map<Address, InternalCacheValue<V>>> iterator = Collections.emptyIterator();

      ReplicaSpliterator() {
         super(Long.MAX_VALUE, 0);

         if (!streamInProgress.compareAndSet(false, true))
            throw new IllegalStateException("ConflictResolutionaManager.getConflicts() already in progress");

         this.totalSegments = distributionManager.getConsistentHash().getNumSegments();
      }

      @Override
      public boolean tryAdvance(Consumer<? super Map<Address, InternalCacheValue<V>>> action) {
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
               Thread.currentThread().interrupt();
               throw new CacheException(e);
            } catch (ExecutionException | CancellationException e) {
               throw new CacheException(e.getCause()); // Should we throw exception? Or WARN and just continue to the next segment?
            }
         }
         action.accept(iterator.next());
         return true;
      }
   }
}
