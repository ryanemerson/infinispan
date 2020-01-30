package org.infinispan.distribution.ch.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.commons.util.IntSet;
import org.infinispan.commons.util.IntSets;
import org.infinispan.distribution.ch.ConsistentHash;
import org.infinispan.globalstate.ScopedPersistentState;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.remoting.transport.Address;
import org.infinispan.remoting.transport.jgroups.JGroupsAddress;
import org.infinispan.topology.ClusterCacheStatus;
import org.infinispan.topology.PersistentUUID;

import net.jcip.annotations.Immutable;

/**
 * CH used by scattered caches. Allows no owners for segments (until the CH is balanced).
 *
 * We cannot have an owner for each segment assigned all the time, because after one node crashes,
 * {@link org.infinispan.distribution.ch.ConsistentHashFactory#updateMembers} is called and the result
 * is sent in CH_UPDATE. Such topology is installed and later during rebalance, a diff of segments
 * between the installed and new (rebalancing) topology is computed. If we assigned all the owners
 * right in updateMembers, this diff would be empty.
 *
 * @since 9.1
 */
@Immutable
@ProtoTypeId(ProtoStreamTypeIds.SCATTERED_CONSISTENT_HASH)
public class ScatteredConsistentHash extends AbstractConsistentHash {
   private PersistentUUID ZERO_UUID = new PersistentUUID(0, 0);

   private static final String STATE_SEGMENT_OWNER = "segmentOwner.%d";
   private static final String REBALANCED = "rebalanced";

   /**
    * The routing table.
    */
   private final Address[] segmentOwners;
   private final List<Address>[] segmentOwnerLists;
   /**
    * Scattered cache guarantees that there will be always one owner of any segment.
    * However rebalance is started in {@link ClusterCacheStatus#startQueuedRebalance()}  based on equality
    * of CH got from {@link ScatteredConsistentHashFactory#updateMembers(ConsistentHash, List, Map)} vs.
    * the on got from {@link ScatteredConsistentHashFactory#rebalance(ScatteredConsistentHash)}.
    * So this works as a flag to trigger the rebalance.
    */
   private final boolean isRebalanced;

   public ScatteredConsistentHash(int numSegments, List<Address> members, Map<Address, Float> capacityFactors,
                                  Address[] segmentOwners, boolean isRebalanced) {
      super(numSegments, members, capacityFactors);
      this.segmentOwners = Arrays.copyOf(segmentOwners, segmentOwners.length);
      this.segmentOwnerLists = Stream.of(segmentOwners).map(ScatteredConsistentHash::toList).toArray(List[]::new);
      this.isRebalanced = isRebalanced;
   }

   private static List<Address> toList(Address address) {
      return address == null ? Collections.emptyList() : Collections.singletonList(address);
   }

   ScatteredConsistentHash(ScopedPersistentState state) {
      super(state);
      int numSegments = parseNumSegments(state);
      this.segmentOwners = new Address[numSegments];
      this.segmentOwnerLists = new List[numSegments];
      for (int i = 0; i < segmentOwners.length; i++) {
         PersistentUUID persistentUUID = PersistentUUID.fromString(state.getProperty(String.format(STATE_SEGMENT_OWNER, i)));
         if (persistentUUID.getMostSignificantBits() == 0 && persistentUUID.getLeastSignificantBits() == 0) {
            segmentOwners[i] = null;
            segmentOwnerLists[i] = Collections.emptyList();
         } else {
            segmentOwners[i] = persistentUUID;
            segmentOwnerLists[i] = Collections.singletonList(persistentUUID);
         }
      }
      this.isRebalanced = Boolean.parseBoolean(state.getProperty(REBALANCED));
   }

   @ProtoFactory
   ScatteredConsistentHash(List<JGroupsAddress> jGroupsMembers, float[] capacityFactorsArray, int[] segmentOwners,
                           boolean rebalanced) {
      super(segmentOwners.length, (List<Address>)(List<?>) jGroupsMembers, capacityFactorsArray);
      this.isRebalanced = rebalanced;

      int numSegments = segmentOwners.length;
      this.segmentOwners = new Address[numSegments];
      for (int i = 0; i < numSegments; i++) {
         int ownerIndex = segmentOwners[i];
         if (ownerIndex >= 0) {
            this.segmentOwners[i] = members.get(ownerIndex);
         }
      }
      this.segmentOwnerLists = Stream.of(this.segmentOwners).map(ScatteredConsistentHash::toList).toArray(List[]::new);
   }

   @ProtoField(number = 1, name = "members", collectionImplementation = ArrayList.class)
   List<JGroupsAddress> getJGroupsMembers() {
      return (List<JGroupsAddress>)(List<?>) members;
   }

   @ProtoField(number = 2, name = "capacityFactors")
   float[] getCapacityFactorsArray() {
      return capacityFactors;
   }

   @ProtoField(number = 3)
   int[] getSegmentOwners() {
      // Avoid computing the identityHashCode for every ImmutableListCopy/Address
      int[] indexes = new int[segmentOwners.length];
      HashMap<Address, Integer> memberIndexes = getMemberIndexMap(members);
      for (int i = 0; i < segmentOwners.length; i++)
         indexes[i] = segmentOwners[i] == null ? -1 : memberIndexes.get(segmentOwners[i]);
      return indexes;
   }

   @ProtoField(number = 4, defaultValue = "false")
   boolean isRebalanced() {
      return isRebalanced;
   }

   @Override
   public int getNumSegments() {
      return segmentOwners.length;
   }

   @Override
   public Set<Integer> getSegmentsForOwner(Address owner) {
      if (owner == null) {
         throw new IllegalArgumentException("owner cannot be null");
      }
      if (!members.contains(owner)) {
         throw new IllegalArgumentException("Node " + owner + " is not a member");
      }

      IntSet segments = IntSets.mutableEmptySet(segmentOwners.length);
      for (int segment = 0; segment < segmentOwners.length; segment++) {
         if (Objects.equals(segmentOwners[segment], owner)) {
            segments.set(segment);
         }
      }
      return segments;
   }

   @Override
   public Set<Integer> getPrimarySegmentsForOwner(Address owner) {
      return getSegmentsForOwner(owner);
   }

   @Override
   public List<Address> locateOwnersForSegment(int segmentId) {
      return segmentOwnerLists[segmentId];
   }

   @Override
   public Address locatePrimaryOwnerForSegment(int segmentId) {
      return segmentOwners[segmentId];
   }

   @Override
   public boolean isSegmentLocalToNode(Address nodeAddress, int segmentId) {
      return Objects.equals(segmentOwners[segmentId], nodeAddress);
   }

   @Override
   public int hashCode() {
      int result = members.hashCode() + (isRebalanced ? 1 : 0);
      result = 31 * result + Arrays.hashCode(segmentOwners);
      return result;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      ScatteredConsistentHash that = (ScatteredConsistentHash) o;

      if (isRebalanced != that.isRebalanced) return false;
      if (segmentOwners.length != that.segmentOwners.length) return false;
      if (!members.equals(that.members)) return false;
      if (!Arrays.equals(segmentOwners, that.segmentOwners)) return false;

      return true;
   }

   @Override
   public String toString() {
      OwnershipStatistics stats = new OwnershipStatistics(this, members);
      StringBuilder sb = new StringBuilder("ScatteredConsistentHash{");
      sb.append("ns=").append(segmentOwners.length);
      sb.append(", rebalanced=").append(isRebalanced);
      sb.append(", owners = (").append(members.size()).append(")[");
      boolean first = true;
      for (Address a : members) {
         if (first) {
            first = false;
         } else {
            sb.append(", ");
         }
         int primaryOwned = stats.getPrimaryOwned(a);
         sb.append(a).append(": ").append(primaryOwned);
      }
      sb.append("]}");
      return sb.toString();
   }

   @Override
   public String getRoutingTableAsString() {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < segmentOwners.length; i++) {
         if (i > 0) {
            sb.append(", ");
         }
         sb.append(i).append(": ").append(members.indexOf(segmentOwners[i]));
      }
      return sb.toString();
   }

   /**
    * Prefers owners from the second hash -> effectively this will make pendingCH == unionCH (I think)
    */
   public ScatteredConsistentHash union(ScatteredConsistentHash sch2) {
      checkSameHashAndSegments(sch2);

      List<Address> unionMembers = new ArrayList<>(this.members);
      mergeLists(unionMembers, sch2.getMembers());

      Address[] unionSegmentOwners = new Address[segmentOwners.length];
      for (int i = 0; i < unionSegmentOwners.length; i++) {
         unionSegmentOwners[i] = Optional.ofNullable(sch2.locatePrimaryOwnerForSegment(i)).orElse(locatePrimaryOwnerForSegment(i));
      }

      Map<Address, Float> unionCapacityFactors = unionCapacityFactors(sch2);
      return new ScatteredConsistentHash(unionSegmentOwners.length, unionMembers, unionCapacityFactors, unionSegmentOwners, false);
   }

   @Override
   public void toScopedState(ScopedPersistentState state) {
      super.toScopedState(state);
      for (int i = 0; i < segmentOwners.length; i++) {
         state.setProperty(String.format(STATE_SEGMENT_OWNER, i), (segmentOwners[i] == null ? ZERO_UUID : segmentOwners[i]).toString());
      }
      state.setProperty(REBALANCED, String.valueOf(isRebalanced));
   }

   @Override
   public ConsistentHash remapAddresses(UnaryOperator<Address> remapper) {
      List<Address> remappedMembers = remapMembers(remapper);
      if (remappedMembers == null) return null;
      Map<Address, Float> remappedCapacityFactors = remapCapacityFactors(remapper);
      Address[] remappedSegmentOwners = Stream.of(segmentOwners).map(remapper).toArray(Address[]::new);
      return new ScatteredConsistentHash(segmentOwners.length, remappedMembers,
            remappedCapacityFactors, remappedSegmentOwners, isRebalanced);
   }
}
