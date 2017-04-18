package org.infinispan.conflict.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.infinispan.distribution.ch.ConsistentHash;
import org.infinispan.distribution.ch.ConsistentHashFactory;
import org.infinispan.partitionhandling.impl.AvailabilityStrategyContext;
import org.infinispan.remoting.transport.Address;
import org.infinispan.topology.CacheTopology;

/**
 * @author Ryan Emerson
 * @since 9.1
 */
public class PartitionMergeInfo {
   private final Map<Address, CacheTopology> nodeTopologies;
   private final ConsistentHash mergeHash;

   public static PartitionMergeInfo create(AvailabilityStrategyContext context, ConsistentHash maxStableTopologyHash, Map<Address, CacheTopology> nodeTopologies) {
      List<Address> members = new ArrayList<>(nodeTopologies.keySet());
      ConsistentHashFactory chFactory = context.getJoinInfo().getConsistentHashFactory();
      ConsistentHash updatedMembersCH = chFactory.updateMembers(maxStableTopologyHash, members, context.getCapacityFactors());
      ConsistentHash mergeHash = chFactory.rebalance(updatedMembersCH);
      return new PartitionMergeInfo(nodeTopologies, mergeHash);
   }

   private PartitionMergeInfo(Map<Address, CacheTopology> nodeTopologies, ConsistentHash mergeHash) {
      this.nodeTopologies = nodeTopologies;
      this.mergeHash = mergeHash;
   }

   public int getTopologyId(Address address) {
      CacheTopology topology = nodeTopologies.get(address);
      return topology == null ? -1 : topology.getTopologyId();
   }

   public Map<Address, CacheTopology> getNodeTopologies() {
      return nodeTopologies;
   }

   public ConsistentHash getMergeHash() {
      return mergeHash;
   }
}
