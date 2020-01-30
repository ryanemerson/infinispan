package org.infinispan.topology;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.distribution.ch.ConsistentHash;
import org.infinispan.marshall.protostream.impl.WrappedMessages;
import org.infinispan.protostream.WrappedMessage;
import org.infinispan.protostream.annotations.ProtoEnumValue;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.remoting.transport.Address;
import org.infinispan.remoting.transport.jgroups.JGroupsAddress;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * The status of a cache from a distribution/state transfer point of view.
 * <p/>
 * The pending CH can be {@code null} if we don't have a state transfer in progress.
 * <p/>
 * The {@code topologyId} is incremented every time the topology changes (e.g. a member leaves, state transfer
 * starts or ends).
 * The {@code rebalanceId} is not modified when the consistent hashes are updated without requiring state
 * transfer (e.g. when a member leaves).
 *
 * @author Dan Berindei
 * @since 5.2
 */
@ProtoTypeId(ProtoStreamTypeIds.CACHE_TOPOLOGY)
public class CacheTopology {

   private static Log log = LogFactory.getLog(CacheTopology.class);
   private static final boolean trace = log.isTraceEnabled();

   @ProtoField(number = 1, defaultValue = "-1")
   final int topologyId;

   @ProtoField(number = 2, defaultValue = "-1")
   final int rebalanceId;

   @ProtoField(number = 3)
   final WrappedMessage currentCH;

   @ProtoField(number = 4)
   final WrappedMessage pendingCH;

   @ProtoField(number = 5)
   final WrappedMessage unionCH;

   @ProtoField(number = 6)
   final Phase phase;

   // The persistent UUID of each actual member
   @ProtoField(number = 7, collectionImplementation = ArrayList.class)
   List<PersistentUUID> persistentUUIDs;

   List<Address> actualMembers;

   @ProtoFactory
   CacheTopology(int topologyId, int rebalanceId, WrappedMessage currentCH, WrappedMessage pendingCH,
                 WrappedMessage unionCH, Phase phase, List<PersistentUUID> persistentUUIDs,
                 List<JGroupsAddress> jGroupsMembers) {
      this.topologyId = topologyId;
      this.rebalanceId = rebalanceId;
      this.currentCH = currentCH;
      this.pendingCH = pendingCH;
      this.unionCH = unionCH;
      this.phase = phase;
      this.persistentUUIDs = persistentUUIDs;
      this.actualMembers = (List<Address>)(List<?>) jGroupsMembers;
   }

   public CacheTopology(int topologyId, int rebalanceId, ConsistentHash currentCH, ConsistentHash pendingCH,
                        Phase phase, List<Address> actualMembers, List<PersistentUUID> persistentUUIDs) {
      this(topologyId, rebalanceId, currentCH, pendingCH, null, phase, actualMembers, persistentUUIDs);
   }

   public CacheTopology(int topologyId, int rebalanceId, ConsistentHash currentCH, ConsistentHash pendingCH,
                        ConsistentHash unionCH, Phase phase, List<Address> actualMembers, List<PersistentUUID> persistentUUIDs) {
      if (pendingCH != null && !pendingCH.getMembers().containsAll(currentCH.getMembers())) {
         throw new IllegalArgumentException("A cache topology's pending consistent hash must " +
               "contain all the current consistent hash's members: currentCH=" + currentCH + ", pendingCH=" + pendingCH);
      }
      if (persistentUUIDs != null && persistentUUIDs.size() != actualMembers.size()) {
         throw new IllegalArgumentException("There must be one persistent UUID for each actual member");
      }
      this.topologyId = topologyId;
      this.rebalanceId = rebalanceId;
      this.currentCH = WrappedMessages.orElseNull(currentCH);
      this.pendingCH = WrappedMessages.orElseNull(pendingCH);
      this.unionCH = WrappedMessages.orElseNull(unionCH);
      this.phase = phase;
      this.actualMembers = actualMembers;
      this.persistentUUIDs = persistentUUIDs;
   }

   @ProtoField(number = 8, collectionImplementation = ArrayList.class)
   List<JGroupsAddress> getJGroupsMembers() {
      return (List<JGroupsAddress>)(List<?>) actualMembers;
   }

   public int getTopologyId() {
      return topologyId;
   }

   /**
    * The current consistent hash.
    */
   public ConsistentHash getCurrentCH() {
      return WrappedMessages.unwrap(currentCH);
   }

   /**
    * The future consistent hash. Should be {@code null} if there is no rebalance in progress.
    */
   public ConsistentHash getPendingCH() {
      return WrappedMessages.unwrap(pendingCH);
   }

   /**
    * The union of the current and future consistent hashes. Should be {@code null} if there is no rebalance in progress.
    */
   public ConsistentHash getUnionCH() {
      return WrappedMessages.unwrap(unionCH);
   }

   /**
    * The id of the latest started rebalance.
    */
   public int getRebalanceId() {
      return rebalanceId;
   }

   /**
    * @return The nodes that are members in both consistent hashes (if {@code pendingCH != null},
    *    otherwise the members of the current CH).
    * @see #getActualMembers()
    */
   public List<Address> getMembers() {
      if (pendingCH != null)
         return getPendingCH().getMembers();
      else if (currentCH != null)
         return getCurrentCH().getMembers();
      else
         return Collections.emptyList();
   }

   /**
    * @return The nodes that are active members of the cache. It should be equal to {@link #getMembers()} when the
    *    cache is available, and a strict subset if the cache is in degraded mode.
    * @see org.infinispan.partitionhandling.AvailabilityMode
    */
   public List<Address> getActualMembers() {
      return actualMembers;
   }

   public List<PersistentUUID> getMembersPersistentUUIDs() {
      return persistentUUIDs;
   }

   /**
    * Read operations should always go to the "current" owners.
    */
   public ConsistentHash getReadConsistentHash() {
      switch (phase) {
         case CONFLICT_RESOLUTION:
         case NO_REBALANCE:
            assert pendingCH == null;
            assert unionCH == null;
            return getCurrentCH();
         case TRANSITORY:
            return getPendingCH();
         case READ_OLD_WRITE_ALL:
            assert pendingCH != null;
            assert unionCH != null;
            return getCurrentCH();
         case READ_ALL_WRITE_ALL:
            assert pendingCH != null;
            return getUnionCH();
         case READ_NEW_WRITE_ALL:
            assert unionCH != null;
            return getPendingCH();
         default:
            throw new IllegalStateException();
      }
   }

   /**
    * When there is a rebalance in progress, write operations should go to the union of the "current" and "future" owners.
    */
   public ConsistentHash getWriteConsistentHash() {
      switch (phase) {
         case CONFLICT_RESOLUTION:
         case NO_REBALANCE:
            assert pendingCH == null;
            assert unionCH == null;
            return getCurrentCH();
         case TRANSITORY:
            return getPendingCH();
         case READ_OLD_WRITE_ALL:
         case READ_ALL_WRITE_ALL:
         case READ_NEW_WRITE_ALL:
            assert pendingCH != null;
            assert unionCH != null;
            return getUnionCH();
         default:
            throw new IllegalStateException();
      }
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      CacheTopology topology = (CacheTopology) o;
      return topologyId == topology.topologyId &&
            rebalanceId == topology.rebalanceId &&
            Objects.equals(currentCH, topology.currentCH) &&
            Objects.equals(pendingCH, topology.pendingCH) &&
            Objects.equals(unionCH, topology.unionCH) &&
            phase == topology.phase &&
            Objects.equals(persistentUUIDs, topology.persistentUUIDs) &&
            Objects.equals(actualMembers, topology.actualMembers);
   }

   @Override
   public int hashCode() {
      return Objects.hash(topologyId, rebalanceId, currentCH, pendingCH, unionCH, phase, persistentUUIDs, actualMembers);
   }

   @Override
   public String toString() {
      return "CacheTopology{" +
            "id=" + topologyId +
            ", phase=" + phase +
            ", rebalanceId=" + rebalanceId +
            ", currentCH=" + getCurrentCH() +
            ", pendingCH=" + getPendingCH() +
            ", unionCH=" + getUnionCH() +
            ", actualMembers=" + actualMembers +
            ", persistentUUIDs=" + persistentUUIDs +
            '}';
   }

   public final void logRoutingTableInformation() {
      if (trace) {
         log.tracef("Current consistent hash's routing table: %s", getCurrentCH().getRoutingTableAsString());
         if (pendingCH != null) log.tracef("Pending consistent hash's routing table: %s", getPendingCH().getRoutingTableAsString());
         if (unionCH != null) log.tracef("Union consistent hash's routing table: %s", getUnionCH().getRoutingTableAsString());
      }
   }

   public Phase getPhase() {
      return phase;
   }

   /**
    * Phase of the rebalance process. Using four phases guarantees these properties:
    *
    * 1. T(x+1).writeCH contains all nodes from Tx.readCH (this is the requirement for ISPN-5021)
    * 2. Tx.readCH and T(x+1).readCH has non-empty subset of nodes (that will allow no blocking for read commands
    *    and reading only entries node owns according to readCH)
    *
    * Old entries should be wiped out only after coming to the {@link #NO_REBALANCE} phase.
    */
   @ProtoTypeId(ProtoStreamTypeIds.CACHE_TOPOLOGY_PHASE)
   public enum Phase {
      /**
       * Only currentCH should be set, this works as both readCH and writeCH
       */
      @ProtoEnumValue(number = 1)
      NO_REBALANCE(false),

      /**
       * Used by caches that don't use 4-phase topology change. PendingCH is used for both read and write.
       */
      @ProtoEnumValue(number = 2)
      TRANSITORY(true),

      /**
       * Interim state between NO_REBALANCE &rarr; READ_OLD_WRITE_ALL
       * readCh is set locally using previous Topology (of said node) readCH, whilst writeCH contains all members after merge
       */
      @ProtoEnumValue(number = 3)
      CONFLICT_RESOLUTION(false),

      /**
       * Used during state transfer: readCH == currentCH, writeCH = unionCH
       */
      @ProtoEnumValue(number = 4)
      READ_OLD_WRITE_ALL(true),

      /**
       * Used after state transfer completes: readCH == writeCH = unionCH
       */
      @ProtoEnumValue(number = 5)
      READ_ALL_WRITE_ALL(false),

      /**
       * Intermediate state that prevents ISPN-5021: readCH == pendingCH, writeCH = unionCH
       */
      @ProtoEnumValue(number = 6)
      READ_NEW_WRITE_ALL(false);

      private static final Phase[] values = Phase.values();
      private final boolean rebalance;


      Phase(boolean rebalance) {
         this.rebalance = rebalance;
      }

      public boolean isRebalance() {
         return rebalance;
      }

      public static Phase valueOf(int ordinal) {
         return values[ordinal];
      }
   }
}
