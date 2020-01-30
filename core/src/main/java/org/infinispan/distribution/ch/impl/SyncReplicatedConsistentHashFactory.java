package org.infinispan.distribution.ch.impl;

import static org.infinispan.util.logging.Log.CONTAINER;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.distribution.ch.ConsistentHashFactory;
import org.infinispan.globalstate.ScopedPersistentState;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.remoting.transport.Address;

/**
 * {@link SyncConsistentHashFactory} adapted for replicated caches, so that the primary owner of a key
 * is the same in replicated and distributed caches.
 *
 * @author Dan Berindei
 * @since 8.2
 */
@ProtoTypeId(ProtoStreamTypeIds.SYNC_REPLICATED_CONSISTENT_HASH)
public class SyncReplicatedConsistentHashFactory implements ConsistentHashFactory<ReplicatedConsistentHash> {

   private static final SyncConsistentHashFactory syncCHF = new SyncConsistentHashFactory();

   @Override
   public ReplicatedConsistentHash create(int numOwners, int numSegments,
         List<Address> members, Map<Address, Float> capacityFactors) {
      DefaultConsistentHash dch = syncCHF.create(1, numSegments, members, null);
      return replicatedFromDefault(dch);
   }

   @Override
   public ReplicatedConsistentHash fromPersistentState(ScopedPersistentState state) {
      String consistentHashClass = state.getProperty("consistentHash");
      if (!ReplicatedConsistentHash.class.getName().equals(consistentHashClass))
         throw CONTAINER.persistentConsistentHashMismatch(this.getClass().getName(), consistentHashClass);
      return new ReplicatedConsistentHash(state);
   }

   private ReplicatedConsistentHash replicatedFromDefault(DefaultConsistentHash dch) {
      int numSegments = dch.getNumSegments();
      List<Address> members = dch.getMembers();
      int[] primaryOwners = new int[numSegments];
      for (int segment = 0; segment < numSegments; segment++) {
         primaryOwners[segment] = members.indexOf(dch.locatePrimaryOwnerForSegment(segment));
      }
      return new ReplicatedConsistentHash(members, primaryOwners);
   }

   @Override
   public ReplicatedConsistentHash updateMembers(ReplicatedConsistentHash baseCH, List<Address> newMembers,
         Map<Address, Float> actualCapacityFactors) {
      DefaultConsistentHash baseDCH = defaultFromReplicated(baseCH);
      DefaultConsistentHash dch = syncCHF.updateMembers(baseDCH, newMembers, null);
      return replicatedFromDefault(dch);
   }

   private DefaultConsistentHash defaultFromReplicated(ReplicatedConsistentHash baseCH) {
      int numSegments = baseCH.getNumSegments();
      List<Address>[] baseSegmentOwners = new List[numSegments];
      for (int segment = 0; segment < numSegments; segment++) {
         baseSegmentOwners[segment] = Collections.singletonList(baseCH.locatePrimaryOwnerForSegment(segment));
      }
      return new DefaultConsistentHash(1,
            numSegments, baseCH.getMembers(), null, baseSegmentOwners);
   }

   @Override
   public ReplicatedConsistentHash rebalance(ReplicatedConsistentHash baseCH) {
      DefaultConsistentHash baseDCH = defaultFromReplicated(baseCH);
      DefaultConsistentHash dch = syncCHF.rebalance(baseDCH);
      return replicatedFromDefault(dch);
   }

   @Override
   public ReplicatedConsistentHash union(ReplicatedConsistentHash ch1, ReplicatedConsistentHash ch2) {
      return ch1.union(ch2);
   }
}
