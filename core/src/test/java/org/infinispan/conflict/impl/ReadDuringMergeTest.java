package org.infinispan.conflict.impl;

import static org.infinispan.test.TestingUtil.extractGlobalComponent;
import static org.infinispan.test.TestingUtil.replaceComponent;
import static org.infinispan.test.TestingUtil.wrapInboundInvocationHandler;
import static org.infinispan.topology.CacheTopology.Phase.READ_OLD_WRITE_ALL;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.stream.IntStream;

import org.infinispan.commands.ReplicableCommand;
import org.infinispan.commands.remote.CacheRpcCommand;
import org.infinispan.context.Flag;
import org.infinispan.distribution.MagicKey;
import org.infinispan.remoting.inboundhandler.DeliverOrder;
import org.infinispan.remoting.inboundhandler.InboundInvocationHandler;
import org.infinispan.remoting.inboundhandler.PerCacheInboundInvocationHandler;
import org.infinispan.remoting.inboundhandler.Reply;
import org.infinispan.remoting.transport.Address;
import org.infinispan.statetransfer.StateResponseCommand;
import org.infinispan.test.TestingUtil;
import org.infinispan.test.fwk.TestInternalCacheEntryFactory;
import org.infinispan.topology.CacheTopologyControlCommand;
import org.infinispan.xsite.XSiteReplicateCommand;

/**
 1. do a split, and let k -> A, k -> B in the two partitions
 2. initiate a conflict resolution, with merge policy saying that merge A,B = C
 3. check that members from each partition read A (in p1) or B (in p2)
 4. let someone from p1 issue a write k -> D, check that both p1 and p2 now reads D
 5. let the actual merge proceed (be ignored)
 6. check that all nodes still read D
 7. let state transfer proceed and check that D is still in

 For sanity check, you should be able to disable the write of D and see C everywhere instead.
 And the same should work for removal as well (merge should not overwrite removal), though I think that CommitManager will behave in the same way.
 * @author Ryan Emerson
 * @since 9.1
 */
public class ReadDuringMergeTest extends BaseMergePolicyTest {

   MagicKey conflictKey;

   public ReadDuringMergeTest() {
      super();
      this.mergePolicy = ((preferredEntry, otherEntries) -> TestInternalCacheEntryFactory.create(conflictKey, "C"));
   }


   @Override
   void beforeSplit() {
      conflictKey = new MagicKey(cache(2), cache(0));
   }

   @Override
   void duringSplit() {
      cache(0).put(conflictKey, "A");
      cache(2).put(conflictKey, "B");

      assertCacheGet("A", 0, 1);
      assertCacheGet("B", 2, 3);
   }

   @Override
   protected void performMerge() {
      CountDownLatch conflictLatch = new CountDownLatch(1);
      CountDownLatch stateTransferLatch = new CountDownLatch(1);
      IntStream.range(0, numMembersInCluster).forEach(i -> {
         wrapInboundInvocationHandler(cache(i), handler -> new BlockStateResponseCommandHandler(handler, conflictLatch));

         InboundInvocationHandler handler = extractGlobalComponent(manager(i), InboundInvocationHandler.class);
         BlockingInboundInvocationHandler ourHandler = new BlockingInboundInvocationHandler(handler, stateTransferLatch);
         replaceComponent(manager(i), InboundInvocationHandler.class, ourHandler, true);
      });

      partition(0).merge(partition(1), false);
      assertCacheGet("A", 0, 1);
      assertCacheGet("B", 2, 3);
      cache(0).put(conflictKey, "D");

      assertCacheGet("D", 0, 1, 2, 3);
      conflictLatch.countDown();
      assertCacheGet("D", 0, 1, 2, 3);

      stateTransferLatch.countDown();
      TestingUtil.waitForNoRebalance(caches());
      assertCacheGet("D", 0, 1, 2, 3);
   }

   @Override
   void afterMerge() {
      assertCacheGet("D", 0, 1, 2, 3);
   }

   private void assertCacheGet(Object value, int... caches) {
      for (int index : caches) {
         System.err.println(String.format("Value=%s, Cache=%s", value, index));
         assertEquals(value, advancedCache(index).get(conflictKey));
      }
   }

   private class BlockingInboundInvocationHandler implements InboundInvocationHandler {
      final InboundInvocationHandler delegate;
      final CountDownLatch latch;

      public BlockingInboundInvocationHandler(InboundInvocationHandler delegate, CountDownLatch latch) {
         this.delegate = delegate;
         this.latch = latch;
      }

      @Override
      public void handleFromCluster(Address origin, ReplicableCommand command, Reply reply, DeliverOrder order) {
         if (command instanceof CacheTopologyControlCommand) {
            CacheTopologyControlCommand cmd = (CacheTopologyControlCommand) command;
            if (cmd.getType() == CacheTopologyControlCommand.Type.CH_UPDATE && cmd.getPhase() == READ_OLD_WRITE_ALL) {
               try {
                  latch.await();
               } catch (InterruptedException ignore) {
               }
            }
         }
         delegate.handleFromCluster(origin, command, reply, order);
      }

      @Override
      public void handleFromRemoteSite(String origin, XSiteReplicateCommand command, Reply reply, DeliverOrder order) {
         delegate.handleFromRemoteSite(origin, command, reply, order);
      }
   }

   private class BlockStateResponseCommandHandler implements PerCacheInboundInvocationHandler {
      final PerCacheInboundInvocationHandler delegate;
      final CountDownLatch latch;

      BlockStateResponseCommandHandler(PerCacheInboundInvocationHandler delegate, CountDownLatch latch) {
         this.delegate = delegate;
         this.latch = latch;
      }

      @Override
      public void handle(CacheRpcCommand command, Reply reply, DeliverOrder order) {
         if (command instanceof StateResponseCommand) {
            try {
               latch.await();
            } catch (InterruptedException ignore) {
            }
         }
         delegate.handle(command, reply, order);
      }
   }
}
