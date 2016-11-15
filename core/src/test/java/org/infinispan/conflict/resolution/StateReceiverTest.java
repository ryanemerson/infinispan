package org.infinispan.conflict.resolution;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.infinispan.Cache;
import org.infinispan.commands.CommandsFactory;
import org.infinispan.commons.CacheException;
import org.infinispan.commons.hash.MurmurHash3;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.container.DataContainer;
import org.infinispan.container.entries.ImmortalCacheEntry;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.distribution.TestAddress;
import org.infinispan.distribution.ch.impl.DefaultConsistentHash;
import org.infinispan.distribution.ch.impl.DefaultConsistentHashFactory;
import org.infinispan.remoting.inboundhandler.DeliverOrder;
import org.infinispan.remoting.responses.Response;
import org.infinispan.remoting.responses.SuccessfulResponse;
import org.infinispan.remoting.rpc.ResponseMode;
import org.infinispan.remoting.rpc.RpcManager;
import org.infinispan.remoting.rpc.RpcOptions;
import org.infinispan.remoting.rpc.RpcOptionsBuilder;
import org.infinispan.remoting.transport.Address;
import org.infinispan.statetransfer.InboundTransferTask;
import org.infinispan.statetransfer.StateChunk;
import org.infinispan.statetransfer.StateRequestCommand;
import org.infinispan.test.AbstractInfinispanTest;
import org.infinispan.topology.CacheTopology;
import org.infinispan.topology.PersistentUUID;
import org.infinispan.topology.PersistentUUIDManager;
import org.infinispan.topology.PersistentUUIDManagerImpl;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author Ryan Emerson
 * @since 9.0
 */
@Test(groups = "functional", testName = "conflict.resolution.StateReceiverTest")
public class StateReceiverTest extends AbstractInfinispanTest {

   private StateReceiverImpl<Object, Object> stateReceiver;

   public void testGetReplicaException() throws Exception {
      CompletableFuture<Void> taskFuture = new CompletableFuture<>();
      taskFuture.completeExceptionally(new CacheException("Problem encountered retrieving state"));
      initTransferTaskMock(taskFuture);

      CompletableFuture<List<Map<Address, InternalCacheEntry<Object, Object>>>> cf = stateReceiver.getAllReplicasForSegment(0);
      try {
         cf.get();
         fail("Expected an ExecutionExceptions caused by a CacheException");
      } catch (ExecutionException e) {
         assertTrue(e.getCause() instanceof CacheException, "Expected an ExecutionExceptions caused by a CacheException.");
      }
   }

   @Test(expectedExceptions = IllegalStateException.class)
   public void testConcurrentInvocationsOfRequestSegments() {
      initTransferTaskMock(new CompletableFuture<>());

      stateReceiver.getAllReplicasForSegment(0);
      stateReceiver.getAllReplicasForSegment(1);
   }

   public void testTopologyChangeDuringSegmentRequest() throws Exception {
      initTransferTaskMock(new CompletableFuture<>());

      CompletableFuture<List<Map<Address, InternalCacheEntry<Object, Object>>>> cf = stateReceiver.getAllReplicasForSegment(0);
      stateReceiver.onTopologyUpdate(createCacheTopology(3, 2, 4), false);
      assertFalse(cf.isCancelled());
      assertFalse(cf.isCompletedExceptionally());

      // Reduce #nodes to less than numowners to force hash change
      stateReceiver.onTopologyUpdate(createCacheTopology(4, 3, 1), true);
      assertTrue(cf.isCompletedExceptionally());
      try {
         cf.get();
         fail("Expected the CompletableFuture to fail with CacheException");
      } catch (ExecutionException e) {
         assertTrue(e.getCause() instanceof CacheException);
      } catch (InterruptedException e) {
         fail("Unexpected exception from CompletableFuture", e);
      }

      stateReceiver.onTopologyUpdate(createCacheTopology(5, 4, 4), true);
      cf = stateReceiver.getAllReplicasForSegment(1);
      stateReceiver.onTopologyUpdate(createCacheTopology(6, 4, 4), true);
      assertFalse(cf.isCompletedExceptionally());
      assertFalse(cf.isCancelled());
   }

   public void testOldAndInvalidStateIgnored() {
      initTransferTaskMock(new CompletableFuture<>());

      stateReceiver.getAllReplicasForSegment(0);
      List<Address> sourceAddresses = new ArrayList<>(stateReceiver.getTransferTaskMap().keySet());
      Map<Object, Map<Address, InternalCacheEntry<Object, Object>>> receiverKeyMap = stateReceiver.getKeyReplicaMap();
      assertEquals(receiverKeyMap.size(), 0);
      stateReceiver.receiveState(sourceAddresses.get(0), 2, createStateChunks("Key1", "Value1"));
      assertEquals(receiverKeyMap.size(), 1);
      stateReceiver.receiveState(new TestAddress(5), 2, createStateChunks("Key2", "Value2"));
      assertEquals(receiverKeyMap.size(), 1, "Expected the previous state to be ignored due to unknown sender");
      stateReceiver.receiveState(sourceAddresses.get(1), 1, createStateChunks("Key2", "Value2"));
      assertEquals(receiverKeyMap.size(), 1, "Expected the previous state to be ignored as it has an old topology id");
   }

   @BeforeMethod
   private void createAndInitStateReceiver() {
      Cache cache = mock(Cache.class);
      when(cache.getName()).thenReturn("testCache");

      CommandsFactory commandsFactory = mock(CommandsFactory.class);
      DataContainer dataContainer = mock(DataContainer.class);
      RpcManager rpcManager = mock(RpcManager.class);

      when(rpcManager.invokeRemotely(any(Collection.class), any(StateRequestCommand.class), any(RpcOptions.class)))
            .thenAnswer(new Answer<Map<Address, Response>>() {
               @Override
               public Map<Address, Response> answer(InvocationOnMock invocation) {
                  Collection<Address> recipients = (Collection<Address>) invocation.getArguments()[0];
                  Address recipient = recipients.iterator().next();
                  StateRequestCommand cmd = (StateRequestCommand) invocation.getArguments()[1];
                  Map<Address, Response> results = new HashMap<>(1);
                  if (cmd.getType().equals(StateRequestCommand.Type.START_CONSISTENCY_CHECK)
                        || cmd.getType().equals(StateRequestCommand.Type.CANCEL_CONSISTENCY_CHECK)) {
                     results.put(recipient, SuccessfulResponse.SUCCESSFUL_EMPTY_RESPONSE);
                  }
                  return results;
               }
            });

      when(rpcManager.getRpcOptionsBuilder(any(ResponseMode.class))).thenAnswer(new Answer<RpcOptionsBuilder>() {
         public RpcOptionsBuilder answer(InvocationOnMock invocation) {
            Object[] args = invocation.getArguments();
            return new RpcOptionsBuilder(10000, TimeUnit.MILLISECONDS, (ResponseMode) args[0], DeliverOrder.PER_SENDER);
         }
      });
      ConfigurationBuilder cb = new ConfigurationBuilder();
      cb.clustering().clustering().cacheMode(CacheMode.DIST_SYNC);
      Configuration configuration = cb.build();

      StateReceiverImpl stateReceiver = new StateReceiverImpl();
      stateReceiver.init(cache, commandsFactory, configuration, dataContainer, rpcManager);
      stateReceiver.onTopologyUpdate(createCacheTopology(2, 2, 4), false);

      this.stateReceiver = spy(stateReceiver);
   }

   private void initTransferTaskMock(CompletableFuture<Void> completableFuture) {
      InboundTransferTask task = mock(InboundTransferTask.class);
      when(task.requestSegments()).thenReturn(completableFuture);
      doReturn(task).when(stateReceiver).createTransferTask(any(Integer.class), any(Address.class));
   }

   private Collection<StateChunk> createStateChunks(Object key, Object value) {
      Collection<InternalCacheEntry> entries = Collections.singleton(new ImmortalCacheEntry(key, value));
      return Collections.singleton(new StateChunk(0, entries, true));
   }

   private CacheTopology createCacheTopology(int topologyId, int rebalanceId, int numberOfNodes) {
      PersistentUUIDManager persistentUUIDManager = new PersistentUUIDManagerImpl();
      List<Address> addresses = new ArrayList<>(numberOfNodes);
      for (int i = 0; i < numberOfNodes; i++) {
         Address address = new TestAddress(i);
         addresses.add(address);
         persistentUUIDManager.addPersistentAddressMapping(address, PersistentUUID.randomUUID());
      }

      DefaultConsistentHashFactory chf = new DefaultConsistentHashFactory();
      DefaultConsistentHash ch1 = chf.create(MurmurHash3.getInstance(), 2, 40, addresses, null);
      return new CacheTopology(topologyId, rebalanceId, ch1, null, ch1.getMembers(),persistentUUIDManager.mapAddresses(ch1.getMembers()));
   }
}
