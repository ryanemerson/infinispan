package org.infinispan.conflict.resolution;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.infinispan.Cache;
import org.infinispan.commands.CommandsFactory;
import org.infinispan.commons.hash.MurmurHash3;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.container.DataContainer;
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
import org.infinispan.statetransfer.StateRequestCommand;
import org.infinispan.test.AbstractInfinispanTest;
import org.infinispan.topology.CacheTopology;
import org.infinispan.topology.PersistentUUID;
import org.infinispan.topology.PersistentUUIDManager;
import org.infinispan.topology.PersistentUUIDManagerImpl;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author Ryan Emerson
 * @since 9.0
 */
public class StateReceiverTest extends AbstractInfinispanTest {

   private static final Log log = LogFactory.getLog(StateReceiverTest.class);

   private ExecutorService pooledExecutorService;
   @Spy
   private StateReceiverImpl stateReceiver;

   @AfterMethod
   public void tearDown() {
      if (pooledExecutorService != null) {
         pooledExecutorService.shutdownNow();
      }
   }


   public void testGetReplicaException() {

   }

   @Test(expectedExceptions = IllegalStateException.class)
   public void testConcurrentInvocations() {
      InboundTransferTask task = mock(InboundTransferTask.class);
      when(task.requestSegments()).thenReturn(new CompletableFuture<>());
      doReturn(task).when(stateReceiver).createTransferTask(any(Integer.class), any(Address.class));

      stateReceiver.getAllReplicasForSegment(0);
      stateReceiver.getAllReplicasForSegment(1);
   }

   public void testTopologyChangeDuringSegmentRequest() {
      // TODO
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


      // create list of 6 members
      PersistentUUIDManager persistentUUIDManager = new PersistentUUIDManagerImpl();
      Address[] addresses = new Address[4];
      for (int i = 0; i < 4; i++) {
         addresses[i] = new TestAddress(i);
         persistentUUIDManager.addPersistentAddressMapping(addresses[i], PersistentUUID.randomUUID());
      }
      List<Address> members1 = Arrays.asList(addresses[0], addresses[1], addresses[2], addresses[3]);

      // create CHes
      DefaultConsistentHashFactory chf = new DefaultConsistentHashFactory();
      DefaultConsistentHash ch1 = chf.create(MurmurHash3.getInstance(), 2, 40, members1, null);

      StateReceiverImpl stateReceiver = new StateReceiverImpl();
      stateReceiver.init(cache, commandsFactory, configuration, dataContainer, rpcManager);
      stateReceiver.onTopologyUpdate(new CacheTopology(1, 1, ch1, null, ch1.getMembers(),
            persistentUUIDManager.mapAddresses(ch1.getMembers())), false);

      this.stateReceiver = spy(stateReceiver);
   }
}
