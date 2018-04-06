package org.infinispan.conflict.impl;

import static org.infinispan.configuration.cache.CacheMode.DIST_SYNC;
import static org.infinispan.test.TestingUtil.wrapInboundInvocationHandler;
import static org.testng.AssertJUnit.assertNotNull;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.infinispan.AdvancedCache;
import org.infinispan.commands.remote.CacheRpcCommand;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.conflict.MergePolicy;
import org.infinispan.distribution.MagicKey;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.inboundhandler.DeliverOrder;
import org.infinispan.remoting.inboundhandler.PerCacheInboundInvocationHandler;
import org.infinispan.remoting.inboundhandler.Reply;
import org.infinispan.statetransfer.StateRequestCommand;
import org.infinispan.test.TestingUtil;
import org.infinispan.test.fwk.TransportFlags;


/**
 * 1. Partition cluster
 * 2. When coordinator sends InboundTransferTask for segment crash a node from that segment
 * 3. CR should timeout and the rebalance should occur as normal
 * 4. Assert that conflict resolution has not occurred
 */
public class CrashedNodeDuringConflictResolutionTest extends BaseMergePolicyTest {

   private static final String PARTITION_0_VAL = "A";
   private static final String PARTITION_1_VAL = "B";

   public CrashedNodeDuringConflictResolutionTest() {
      super(DIST_SYNC, null, new int[]{0, 1}, new int[]{2, 3});
      this.mergePolicy = MergePolicy.REMOVE_ALL;
      this.valueAfterMerge = "INITIAL VALUE";
   }

   @Override
   protected void createCacheManagers() {
      ConfigurationBuilder dcc = cacheConfiguration();
      dcc.clustering().cacheMode(cacheMode).partitionHandling().whenSplit(partitionHandling).mergePolicy(mergePolicy)
            // Must be less than timeout used in TestingUtil::waitForNoRebalance
            .stateTransfer().timeout(10, TimeUnit.SECONDS);
      createClusteredCaches(numMembersInCluster, dcc, new TransportFlags().withFD(true).withMerge(true));
      waitForClusterToForm();
   }

   @Override
   protected void beforeSplit() {
      conflictKey = new MagicKey(cache(p0.node(0)), cache(p1.node(0)));
   }

   @Override
   protected void duringSplit(AdvancedCache preferredPartitionCache, AdvancedCache otherCache) {
      cache(p0.node(0)).put(conflictKey, PARTITION_0_VAL);
      cache(p1.node(0)).put(conflictKey, PARTITION_1_VAL);

      assertCacheGet(conflictKey, PARTITION_0_VAL, p0.getNodes());
      assertCacheGet(conflictKey, PARTITION_1_VAL, p1.getNodes());
   }

   @Override
   protected void performMerge() {
      int segment = advancedCache(2).getDistributionManager().getCacheTopology().getSegment(conflictKey);
      EmbeddedCacheManager manager = manager(2);

      // Here we add a handler to kill a node once a request has been received for the segment containing the conflictkey
      wrapInboundInvocationHandler(cache(2), handler -> new KillNodeOnSegmentRequestHandler(handler, segment, manager));

      assertCacheGet(conflictKey, PARTITION_0_VAL, p0.getNodes());
      assertCacheGet(conflictKey, PARTITION_1_VAL, p1.getNodes());
      partition(0).merge(partition(1), false);

      // Eventually the segment request sent to node 2 should timeout resulting in the CR failing with a TimeoutException
      // and the rebalance continuing
      TestingUtil.waitForNoRebalance(cache(0), cache(1), cache(3));
   }

   @Override
   protected void afterConflictResolutionAndMerge() {
      // We expect all three nodes to not be null because the CR REMOVE_ALL policy should not have been invoked
      Arrays.stream(new int[]{0, 1, 3}).forEach(i -> assertNotNull(cache(0).get(conflictKey)));
   }

   private class KillNodeOnSegmentRequestHandler implements PerCacheInboundInvocationHandler {
      final PerCacheInboundInvocationHandler delegate;
      final int segment;
      final EmbeddedCacheManager manager;

      KillNodeOnSegmentRequestHandler(PerCacheInboundInvocationHandler delegate, int segment, EmbeddedCacheManager manager) {
         this.delegate = delegate;
         this.segment = segment;
         this.manager = manager;
      }

      @Override
      public void handle(CacheRpcCommand command, Reply reply, DeliverOrder order) {
         if (command instanceof StateRequestCommand && ((StateRequestCommand) command).getSegments().contains(segment)) {
            TestingUtil.crashCacheManagers(manager);
            return;
         }
         delegate.handle(command, reply, order);
      }
   }
}
