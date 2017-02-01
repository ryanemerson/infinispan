package org.infinispan.conflict.resolution;

import static org.infinispan.test.TestingUtil.wrapPerCacheInboundInvocationHandler;
import static org.testng.AssertJUnit.assertTrue;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import org.infinispan.AdvancedCache;
import org.infinispan.commands.remote.CacheRpcCommand;
import org.infinispan.commands.remote.ClusteredGetCommand;
import org.infinispan.commons.CacheException;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.partitionhandling.BasePartitionHandlingTest;
import org.infinispan.remoting.inboundhandler.DeliverOrder;
import org.infinispan.remoting.inboundhandler.PerCacheInboundInvocationHandler;
import org.infinispan.remoting.inboundhandler.Reply;
import org.infinispan.remoting.transport.Address;
import org.infinispan.statetransfer.StateResponseCommand;
import org.infinispan.test.TestingUtil;
import org.testng.annotations.Test;

@Test(groups = "functional", testName = "conflict.resolution.ConflictManagerDuringStateTransferTest")
public class ConflictManagerDuringStateTransferTest extends BasePartitionHandlingTest {

   private static final String CACHE_NAME = "conflict-cache";

   public ConflictManagerDuringStateTransferTest() {
      this.cacheMode = CacheMode.DIST_SYNC;
      this.partitionHandling = false;
   }

   @Override
   protected void createCacheManagers() throws Throwable {
      super.createCacheManagers();
      ConfigurationBuilder builder = getDefaultClusteredCacheConfig(CacheMode.DIST_SYNC);
      builder.clustering().remoteTimeout(5000).partitionHandling().enabled(false).stateTransfer().fetchInMemoryState(true);
      defineConfigurationOnAllManagers(CACHE_NAME, builder);
   }

   @Test(expectedExceptions = IllegalStateException.class,
         expectedExceptionsMessageRegExp = ".* Unable to retrieve all versions of key .*")
   public void testGetAllVersionsDuringStateTransfer() {
      createAndSplitCluster();
      blockStateTransferCompletion();
      partition(0).merge(partition(1));
      getCache(0).getConflictResolutionManager().getAllVersions("Test");
   }

   @Test(expectedExceptions = CacheException.class,
         expectedExceptionsMessageRegExp = ".* encountered when trying to receive all versions .*")
   public void testGetAllVersionsTimeout() throws Throwable {
      waitForClusterToForm(CACHE_NAME);
      dropClusteredGetCommands();
      try {
         fork(() -> getCache(0).getConflictResolutionManager().getAllVersions("Test")).get();
      } catch (ExecutionException e) {
         throw e.getCause();
      }
   }

   @Test(expectedExceptions = CacheException.class,
         expectedExceptionsMessageRegExp = ".* Aborting attempt to retrieve all versions of key .*")
   public void testGetAllVersionsWithRebalance() throws Throwable {
      createAndSplitCluster();
      dropClusteredGetCommands();

      Future f = fork(() -> getCache(0).getConflictResolutionManager().getAllVersions("Test"));
      fork(() -> partition(0).merge(partition(1)));
      try {
         f.get();
      } catch (ExecutionException e) {
         throw e.getCause();
      }
   }

   @Test(expectedExceptions = IllegalStateException.class,
         expectedExceptionsMessageRegExp = ".* Unable to retrieve conflicts as StateTransfer is currently in progress for cache .*")
   public void testGetConflictsDuringStateTransfer() throws Throwable {
      createAndSplitCluster();
      blockStateTransferCompletion();
      partition(0).merge(partition(1));
      getCache(0).getConflictResolutionManager().getConflicts();
   }

   private void createAndSplitCluster() {
      waitForClusterToForm(CACHE_NAME);
      List<Address> members = getCache(0).getRpcManager().getMembers();

      TestingUtil.waitForRehashToComplete(caches());
      assertTrue(members.size() == 4);

      splitCluster(new int[]{0, 1}, new int[]{2, 3});
      TestingUtil.blockUntilViewsChanged(10000, 2, cache(0), cache(1), cache(2), cache(3));
   }

   private void dropClusteredGetCommands() {
      IntStream.range(0, numMembersInCluster).forEach(i ->
            wrapPerCacheInboundInvocationHandler(getCache(i), (wrapOn, current) -> new DropClusteredGetCommandHandler(current), true));
   }

   private void blockStateTransferCompletion() {
      IntStream.range(0, numMembersInCluster).forEach(i ->
            wrapPerCacheInboundInvocationHandler(getCache(i), (wrapOn, current) -> new DropStateRequestCommandHandler(current), true));
   }

   private class DropStateRequestCommandHandler implements PerCacheInboundInvocationHandler {
      final PerCacheInboundInvocationHandler delegate;

      DropStateRequestCommandHandler(PerCacheInboundInvocationHandler delegate) {
         this.delegate = delegate;
      }

      @Override
      public void handle(CacheRpcCommand command, Reply reply, DeliverOrder order) {
         if (!(command instanceof StateResponseCommand)) {
            delegate.handle(command, reply, order);
         }
      }
   }

   private class DropClusteredGetCommandHandler implements PerCacheInboundInvocationHandler {
      final PerCacheInboundInvocationHandler delegate;

      DropClusteredGetCommandHandler(PerCacheInboundInvocationHandler delegate) {
         this.delegate = delegate;
      }

      @Override
      public void handle(CacheRpcCommand command, Reply reply, DeliverOrder order) {
         if (!(command instanceof ClusteredGetCommand)) {
            delegate.handle(command, reply, order);
         }
      }
   }

   private AdvancedCache<Object, Object> getCache(int index) {
      return advancedCache(index, CACHE_NAME);
   }
}
