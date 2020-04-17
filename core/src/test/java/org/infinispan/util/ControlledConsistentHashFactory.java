package org.infinispan.util;

import static org.testng.AssertJUnit.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import org.infinispan.Cache;
import org.infinispan.distribution.ch.ConsistentHash;
import org.infinispan.distribution.ch.impl.AbstractConsistentHash;
import org.infinispan.distribution.ch.impl.DefaultConsistentHash;
import org.infinispan.distribution.ch.impl.ScatteredConsistentHash;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.remoting.transport.Address;
import org.infinispan.remoting.transport.jgroups.JGroupsAddress;
import org.infinispan.test.TestDataSCI;
import org.infinispan.topology.ClusterTopologyManager;

/**
 * ConsistentHashFactory implementation that allows the user to control who the owners are.
 *
* @author Dan Berindei
* @since 7.0
*/
public abstract class ControlledConsistentHashFactory<CH extends ConsistentHash> extends BaseControlledConsistentHashFactory<CH> {
   protected volatile int[][] ownerIndexes;

   protected volatile List<Address> membersToUse;

   /**
    * Create a consistent hash factory with a single segment.
    */
   public ControlledConsistentHashFactory(Trait<CH> trait, int primaryOwnerIndex, int... backupOwnerIndexes) {
      super(trait, 1);
      setOwnerIndexes(primaryOwnerIndex, backupOwnerIndexes);
   }

   /**
    * Create a consistent hash factory with multiple segments.
    */
   public ControlledConsistentHashFactory(Trait<CH> trait, int[][] segmentOwners) {
      super(trait, segmentOwners.length);
      if (segmentOwners.length == 0)
         throw new IllegalArgumentException("Need at least one set of owners");
      setOwnerIndexes(segmentOwners);
   }

   @ProtoField(number = 2)
   AbstractConsistentHash.SegmentOwnership[] getSegmentOwners() {
      return Arrays.stream(ownerIndexes).map(AbstractConsistentHash.SegmentOwnership::new).toArray(AbstractConsistentHash.SegmentOwnership[]::new);
   }

   @ProtoField(number = 3)
   List<JGroupsAddress> getJGroupsMembers() {
      return (List<JGroupsAddress>)(List<?>) membersToUse;
   }

   public void setOwnerIndexes(int primaryOwnerIndex, int... backupOwnerIndexes) {
      int[] firstSegmentOwners = concatOwners(primaryOwnerIndex, backupOwnerIndexes);
      setOwnerIndexes(new int[][]{firstSegmentOwners});
   }

   private int[] concatOwners(int primaryOwnerIndex, int[] backupOwnerIndexes) {
      int[] firstSegmentOwners;
      if (backupOwnerIndexes == null || backupOwnerIndexes.length == 0) {
         firstSegmentOwners = new int[]{primaryOwnerIndex};
      } else {
         firstSegmentOwners = new int[backupOwnerIndexes.length + 1];
         firstSegmentOwners[0] = primaryOwnerIndex;
         for (int i = 0; i < backupOwnerIndexes.length; i++) {
            firstSegmentOwners[i + 1] = backupOwnerIndexes[i];
         }
      }
      return firstSegmentOwners;
   }

   public void setOwnerIndexes(int[][] segmentOwners) {
      this.ownerIndexes = Arrays.stream(segmentOwners)
                                .map(owners -> Arrays.copyOf(owners, owners.length))
                                .toArray(int[][]::new);
   }

   public void triggerRebalance(Cache<?, ?> cache) throws Exception {
      EmbeddedCacheManager cacheManager = cache.getCacheManager();
      ClusterTopologyManager clusterTopologyManager = cacheManager
            .getGlobalComponentRegistry().getComponent(ClusterTopologyManager.class);
      assertTrue("triggerRebalance must be called on the coordinator node",
            cacheManager.getTransport().isCoordinator());
      clusterTopologyManager.forceRebalance(cache.getName());
   }

   @Override
   protected int[][] assignOwners(int numSegments, List<Address> members) {
      return Arrays.stream(ownerIndexes)
                   .map(indexes -> mapOwnersToCurrentMembers(members, indexes))
                   .toArray(int[][]::new);
   }

   private int[] mapOwnersToCurrentMembers(List<Address> members, int[] indexes) {
      int[] newIndexes = Arrays.stream(indexes).flatMap(index -> {
         if (membersToUse != null) {
            Address owner = membersToUse.get(index);
            int newIndex = members.indexOf(owner);
            if (newIndex >= 0) {
               return IntStream.of(newIndex);
            }
         } else if (index < members.size()) {
            return IntStream.of(index);
         }
         return IntStream.empty();
      }).toArray();

      // A DefaultConsistentHash segment must always have at least one owner
      if (newIndexes.length == 0 && trait.requiresPrimaryOwner()) {
         return new int[]{0};
      }

      return newIndexes;
   }

   /**
    * @param membersToUse Owner indexes will be in this list, instead of the current list of members
    */
   public void setMembersToUse(List<Address> membersToUse) {
      this.membersToUse = membersToUse;
   }

   public static class Default extends ControlledConsistentHashFactory<DefaultConsistentHash> {

      @ProtoFactory
      Default(int numSegments, AbstractConsistentHash.SegmentOwnership[] segmentOwners, List<JGroupsAddress> jGroupsMembers) {
         super(new DefaultTrait(),
               Arrays.stream(segmentOwners)
                     .map(AbstractConsistentHash.SegmentOwnership::getIndexes)
                     .toArray(int[][]::new));
         this.membersToUse = (List<Address>) (List<?>) jGroupsMembers;
      }

      public Default(int primaryOwnerIndex, int... backupOwnerIndexes) {
         super(new DefaultTrait(), primaryOwnerIndex, backupOwnerIndexes);
      }

      public Default(int[][] segmentOwners) {
         super(new DefaultTrait(), segmentOwners);
      }
   }

   /**
    * Ignores backup-owner part of the calls
    */
   public static class Scattered extends ControlledConsistentHashFactory<ScatteredConsistentHash> {

      @ProtoFactory
      Scattered(int numSegments, AbstractConsistentHash.SegmentOwnership[] segmentOwners, List<JGroupsAddress> jGroupsMembers) {
         this(segmentOwners[0].getIndexes());
         this.membersToUse = (List<Address>) (List<?>) jGroupsMembers;
      }

      public Scattered(int primaryOwnerIndex) {
         super(new ScatteredTrait(), primaryOwnerIndex);
      }

      public Scattered(int[] segmentOwners) {
         super(new ScatteredTrait(), Arrays.stream(segmentOwners).mapToObj(o -> new int[]{o}).toArray(int[][]::new));
      }

      @Override
      public void setOwnerIndexes(int primaryOwnerIndex, int... backupOwnerIndexes) {
         super.setOwnerIndexes(primaryOwnerIndex);
      }

      @Override
      public void setOwnerIndexes(int[][] segmentOwners) {
         super.setOwnerIndexes(segmentOwners);
      }
   }

   @AutoProtoSchemaBuilder(
         dependsOn = {
               org.infinispan.marshall.persistence.impl.PersistenceContextInitializer.class,
               org.infinispan.marshall.core.impl.GlobalContextInitializer.class,
               TestDataSCI.class
         },
         includeClasses = {
               Default.class,
               Scattered.class
         },
         schemaFileName = "test.core.ControlledConsistentHashFactory.proto",
         schemaFilePath = "proto/generated",
         schemaPackageName = "org.infinispan.test.core.ControlledConsistentHashFactory")
   public interface SCI extends SerializationContextInitializer {
      ControlledConsistentHashFactory.SCI INSTANCE = new SCIImpl();
   }
}
