package org.infinispan.conflict.impl;

import static org.infinispan.configuration.cache.CacheMode.DIST_SYNC;
import static org.infinispan.test.TestingUtil.wrapInboundInvocationHandler;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.infinispan.AdvancedCache;
import org.infinispan.commands.remote.CacheRpcCommand;
import org.infinispan.conflict.MergePolicy;
import org.infinispan.distribution.MagicKey;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.partitionhandling.BasePartitionHandlingTest;
import org.infinispan.remoting.inboundhandler.DeliverOrder;
import org.infinispan.remoting.inboundhandler.PerCacheInboundInvocationHandler;
import org.infinispan.remoting.inboundhandler.Reply;
import org.infinispan.statetransfer.StateRequestCommand;
import org.infinispan.test.TestingUtil;
import org.testng.annotations.Test;


/**
 * 1. Partition cluster
 * 2. When coordinator sends InboundTransferTask for segment crash a node from that segment
 * 3. CR should timeout and the rebalance should occur as normal
 * 4. Assert that conflict resolution has not occurred
 */
@Test(groups = "functional", testName = "org.infinispan.conflict.impl.CrashedNodeDuringConflictResolutionTest")
public class CrashedNodeDuringConflictResolutionTest extends BaseMergePolicyTest {

   private static final String PARTITION_0_VAL = "A";
   private static final String PARTITION_1_VAL = "B";

   public CrashedNodeDuringConflictResolutionTest() {
      super(DIST_SYNC, null, new int[]{0, 1}, new int[]{2, 3});
      this.mergePolicy = MergePolicy.REMOVE_ALL;
      this.valueAfterMerge = null;
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
      int index = 2;
      CompletableFuture<StateRequestCommand> future = createStateRequestFuture(index)
            .whenComplete((requestCommand, throwable) -> {
               TestingUtil.crashCacheManagers(manager(index));
               BasePartitionHandlingTest.log.errorf("crashCacheManager on segment %s", requestCommand.getSegments());
            });
      assertCacheGet(conflictKey, PARTITION_0_VAL, p0.getNodes());
      assertCacheGet(conflictKey, PARTITION_1_VAL, p1.getNodes());
      fork(() -> partition(0).merge(partition(1), false));

      try {
         future.get(60, TimeUnit.SECONDS);
      } catch (ExecutionException | InterruptedException | TimeoutException e) {
         throw new IllegalStateException(e);
      }

      // Once the JGroups view has been updated to remove manager(index), then the CR should be restarted when the
      // coordinator continues to recover the cluster state
      TestingUtil.waitForNoRebalance(cache(0), cache(1), cache(3));
   }

   private CompletableFuture<StateRequestCommand> createStateRequestFuture(int index) {
      int segment = advancedCache(2).getDistributionManager().getCacheTopology().getSegment(conflictKey);
      CompletableFuture<StateRequestCommand> future = new CompletableFuture<>();
      wrapInboundInvocationHandler(cache(index), handler -> new CompleteFutureOnStateRequestHandler(handler, segment, manager(index), future));
      return future;
   }

   private class CompleteFutureOnStateRequestHandler implements PerCacheInboundInvocationHandler {
      final PerCacheInboundInvocationHandler delegate;
      final int segment;
      final EmbeddedCacheManager manager;
      final CompletableFuture<StateRequestCommand> future;

      CompleteFutureOnStateRequestHandler(PerCacheInboundInvocationHandler delegate, int segment, EmbeddedCacheManager manager,
                                          CompletableFuture<StateRequestCommand> future) {
         this.delegate = delegate;
         this.segment = segment;
         this.manager = manager;
         this.future = future;
      }

      @Override
      public void handle(CacheRpcCommand command, Reply reply, DeliverOrder order) {
         if (command instanceof StateRequestCommand) {
            StateRequestCommand src = (StateRequestCommand) command;
            if (src.getSegments().contains(segment)) {
               future.complete(src);
               return;
            }
         }
         delegate.handle(command, reply, order);
      }
   }
}
