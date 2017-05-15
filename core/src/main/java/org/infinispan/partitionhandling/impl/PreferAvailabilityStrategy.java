package org.infinispan.partitionhandling.impl;

import static org.infinispan.util.logging.events.Messages.MESSAGES;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.infinispan.AdvancedCache;
import org.infinispan.commons.util.InfinispanCollections;
import org.infinispan.conflict.ConflictManagerFactory;
import org.infinispan.conflict.impl.DefaultConflictManager;
import org.infinispan.conflict.impl.PartitionMergeInfo;
import org.infinispan.distribution.ch.ConsistentHash;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.partitionhandling.AvailabilityMode;
import org.infinispan.remoting.transport.Address;
import org.infinispan.topology.CacheStatusResponse;
import org.infinispan.topology.CacheTopology;
import org.infinispan.topology.PersistentUUIDManager;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;
import org.infinispan.util.logging.events.EventLogCategory;
import org.infinispan.util.logging.events.EventLogManager;

public class PreferAvailabilityStrategy implements AvailabilityStrategy {
   private static final Log log = LogFactory.getLog(PreferAvailabilityStrategy.class);
   private final EmbeddedCacheManager cacheManager;
   private final EventLogManager eventLogManager;
   private final PersistentUUIDManager persistentUUIDManager;
   private DefaultConflictManager conflictManager;

   public PreferAvailabilityStrategy(EmbeddedCacheManager cacheManager, EventLogManager eventLogManager, PersistentUUIDManager persistentUUIDManager) {
      this.cacheManager = cacheManager;
      this.eventLogManager = eventLogManager;
      this.persistentUUIDManager = persistentUUIDManager;
   }

   @Override
   public void onJoin(AvailabilityStrategyContext context, Address joiner) {
      context.queueRebalance(context.getExpectedMembers());
   }

   @Override
   public void onGracefulLeave(AvailabilityStrategyContext context, Address leaver) {
      CacheTopology currentTopology = context.getCurrentTopology();
      List<Address> newMembers = new ArrayList<>(currentTopology.getMembers());
      newMembers.remove(leaver);
      if (newMembers.isEmpty()) {
         log.debugf("The last node of cache %s left", context.getCacheName());
         context.updateCurrentTopology(newMembers);
         return;
      }
      if (context.getStableTopology() != null && isDataLost(context.getStableTopology().getCurrentCH(), newMembers)) {
         eventLogManager.getEventLogger().context(context.getCacheName()).warn(EventLogCategory.CLUSTER, MESSAGES.lostDataBecauseOfGracefulLeaver(leaver));
      }

      // We have to do this in case rebalancing is disabled, or there is another rebalance in progress
      context.updateCurrentTopology(newMembers);
      context.queueRebalance(newMembers);
   }

   @Override
   public void onClusterViewChange(AvailabilityStrategyContext context, List<Address> clusterMembers) {
      CacheTopology currentTopology = context.getCurrentTopology();
      List<Address> newMembers = new ArrayList<>(currentTopology.getMembers());
      if (!newMembers.retainAll(clusterMembers)) {
         log.tracef("Cache %s did not lose any members, skipping rebalance", context.getCacheName());
         return;
      }

      checkForLostData(context, newMembers);

      // We have to do the update in case rebalancing is disabled, or there is another rebalance in progress
      context.updateCurrentTopology(newMembers);
      context.queueRebalance(newMembers);
   }

   protected void checkForLostData(AvailabilityStrategyContext context, List<Address> newMembers) {
      CacheTopology stableTopology = context.getStableTopology();
      List<Address> stableMembers = stableTopology.getMembers();
      List<Address> lostMembers = new ArrayList<>(stableMembers);
      lostMembers.removeAll(newMembers);
      if (isDataLost(stableTopology.getCurrentCH(), newMembers)) {
         eventLogManager.getEventLogger().context(context.getCacheName()).fatal(EventLogCategory.CLUSTER, MESSAGES.lostDataBecauseOfAbruptLeavers(lostMembers));
      } else if (lostMembers.size() >= Math.ceil(stableMembers.size() / 2d)) {
         eventLogManager.getEventLogger().context(context.getCacheName()).warn(EventLogCategory.CLUSTER, MESSAGES.minorityPartition(newMembers, lostMembers, stableMembers));
      }
   }

   @Override
   public void onPartitionMerge(AvailabilityStrategyContext context, Map<Address, CacheStatusResponse> statusResponseMap) {
      // Pick the biggest stable topology (i.e. the one with most members)
      CacheTopology maxStableTopology = null;
      Map<Address, CacheTopology> maxTopologyMap = new HashMap<>();
      for (Map.Entry<Address, CacheStatusResponse> entry : statusResponseMap.entrySet()) {
         CacheTopology stableTopology = entry.getValue().getStableTopology();
         if (stableTopology == null) {
            // The node hasn't properly joined yet.
            continue;
         }

         maxTopologyMap.put(entry.getKey(), stableTopology);
         if (maxStableTopology == null || maxStableTopology.getMembers().size() < stableTopology.getMembers().size()) {
            maxStableTopology = stableTopology;
         }
      }

      // Now pick the biggest current topology derived from the biggest stable topology
      CacheTopology maxTopology = null;
      Collection<CacheStatusResponse> statusResponses = statusResponseMap.values();
      for (CacheStatusResponse response : statusResponses) {
         CacheTopology stableTopology = response.getStableTopology();
         if (!Objects.equals(stableTopology, maxStableTopology))
            continue;

         CacheTopology topology = response.getCacheTopology();
         if (topology == null) {
            // The node hasn't properly joined yet.
            continue;
         }
         if (maxTopology == null || maxTopology.getMembers().size() < topology.getMembers().size()) {
            maxTopology = topology;
         }
      }

      if (maxTopology == null) {
         log.debugf("No current topology, recovered only joiners for cache %s", context.getCacheName());
      }

      // Since we picked the biggest topology, its topology id may not be the biggest
      int maxTopologyId = 0;
      int maxRebalanceId = 0;
      for (CacheStatusResponse response : statusResponses) {
         CacheTopology topology = response.getCacheTopology();
         if (topology != null) {
            if (maxTopologyId < topology.getTopologyId()) {
               maxTopologyId = topology.getTopologyId();
            }
            if (maxRebalanceId < topology.getRebalanceId()) {
               maxRebalanceId = topology.getRebalanceId();
            }
         }
      }

      // If we have more expected members than stable members, then we know that a split brain heal is occurring, so initiate
      // conflict resolution before a new merged topology is disseminated across the cluster
      List<Address> newMembers = context.getExpectedMembers();
      boolean isNewCoordinator = maxTopology != null && maxTopology.getMembers().size() == 1;
      if (!isNewCoordinator && maxStableTopology != null && newMembers.size() > maxStableTopology.getMembers().size()) {
         if (conflictManager == null) {
            AdvancedCache<?, ?> cache = cacheManager.getCache(context.getCacheName()).getAdvancedCache();
            conflictManager = (DefaultConflictManager) ConflictManagerFactory.get(cache);
         }

         boolean resolveConflicts = cacheManager.getCacheConfiguration(context.getCacheName()).clustering().partitionHandling().mergePolicy() != null;
         if (resolveConflicts) {
            PartitionMergeInfo mergeInfo = PartitionMergeInfo.create(context, maxStableTopology.getCurrentCH(), maxTopologyMap);
            conflictManager.resolveConflicts(mergeInfo);
         }
      }

      // Increment the topology id so that it's bigger than any topology that might have been sent by the old
      // coordinator. +1 is enough because there nodes wait for the new JGroups view before answering the status
      // request, and after they have the new view they can't process topology updates with the old view id.
      // Also cancel any pending rebalance by removing the pending CH, because we don't recover the rebalance
      // confirmation status (yet).
      CacheTopology mergedTopology = null;
      if (maxTopology != null) {
         // There's no pendingCH, therefore the topology is in stable phase
         mergedTopology = new CacheTopology(maxTopologyId + 1, maxRebalanceId + 1,
               maxTopology.getCurrentCH(), null, CacheTopology.Phase.NO_REBALANCE, maxTopology.getActualMembers(),
               persistentUUIDManager.mapAddresses(maxTopology.getActualMembers()));
      }

      context.updateTopologiesAfterMerge(mergedTopology, maxStableTopology, null);

      // First update the CHs to remove any nodes that left from the current topology
      List<Address> survivingMembers = new ArrayList<>(newMembers);
      if (mergedTopology != null && survivingMembers.retainAll(mergedTopology.getMembers())) {
         checkForLostData(context, survivingMembers);
      }
      context.updateCurrentTopology(survivingMembers);

      // Then start a rebalance with the merged members
      context.queueRebalance(newMembers);
   }

   @Override
   public void onRebalanceEnd(AvailabilityStrategyContext context) {
      // Do nothing, if we need another rebalance it's already in the queue
   }

   @Override
   public void onManualAvailabilityChange(AvailabilityStrategyContext context, AvailabilityMode newAvailabilityMode) {
      // The cache should always be AVAILABLE
   }

   private boolean isDataLost(ConsistentHash currentCH, List<Address> newMembers) {
      for (int i = 0; i < currentCH.getNumSegments(); i++) {
         if (!InfinispanCollections.containsAny(newMembers, currentCH.locateOwnersForSegment(i)))
            return true;
      }
      return false;
   }

}
