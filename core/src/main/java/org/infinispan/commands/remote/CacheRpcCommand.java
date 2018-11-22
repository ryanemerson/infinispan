package org.infinispan.commands.remote;

import org.infinispan.Cache;
import org.infinispan.commands.ReplicableCommand;
import org.infinispan.commons.time.TimeService;
import org.infinispan.container.DataContainer;
import org.infinispan.container.impl.InternalEntryFactory;
import org.infinispan.context.InvocationContextFactory;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.interceptors.AsyncInterceptorChain;
import org.infinispan.notifications.cachelistener.CacheNotifier;
import org.infinispan.persistence.manager.OrderedUpdatesManager;
import org.infinispan.remoting.transport.Address;
import org.infinispan.scattered.BiasManager;
import org.infinispan.statetransfer.StateConsumer;
import org.infinispan.statetransfer.StateProvider;
import org.infinispan.statetransfer.StateTransferLock;
import org.infinispan.statetransfer.StateTransferManager;
import org.infinispan.stream.impl.ClusterStreamManager;
import org.infinispan.stream.impl.LocalStreamManager;
import org.infinispan.transaction.TransactionTable;
import org.infinispan.transaction.xa.recovery.RecoveryManager;
import org.infinispan.util.ByteString;
import org.infinispan.util.concurrent.CommandAckCollector;
import org.infinispan.xsite.BackupSender;
import org.infinispan.xsite.statetransfer.XSiteStateConsumer;
import org.infinispan.xsite.statetransfer.XSiteStateProvider;

/**
 * The {@link org.infinispan.remoting.rpc.RpcManager} only replicates commands wrapped in a {@link CacheRpcCommand}.
 *
 * @author Manik Surtani
 * @author Mircea.Markus@jboss.com
 * @since 4.0
 */
public interface CacheRpcCommand extends ReplicableCommand {

   /**
    * @return the name of the cache that produced this command.  This will also be the name of the cache this command is
    *         intended for.
    */
   ByteString getCacheName();

   /**
    * Set the origin of the command
    */
   void setOrigin(Address origin);

   /**
    * Get the origin of the command
    */
   Address getOrigin();

   /**
    * An optional method that allows a {@link ReplicableCommand} to initialise it's state after deserialization.
    */
   default void init(InitializationContext context, boolean isRemote) {
      ReplicableCommand.super.init(context, isRemote);
   }

   /**
    * Interface that exposes frequently used components in order to avoid repeated hash lookups for common components.
    * As the purpose of this interface is to avoid hash lookups, calls to {@link #getComponentRegistry()} should be
    * avoided where possible.
    */
   interface InitializationContext extends ReplicableCommand.InitializationContext {

      AsyncInterceptorChain getInterceptorChain();

      BackupSender getBackupSender();

      BiasManager getBiasManager();

      Cache getCache();

      DataContainer getDataContainer();

      CacheNotifier getCacheNotifier();

      ClusterStreamManager getClusterStreamManager();

      CommandAckCollector getCommandAckCollector();

      ComponentRegistry getComponentRegistry();

      DistributionManager getDistributionManager();

      InternalEntryFactory getInternalEntryFactory();

      InvocationContextFactory getInvocationContextFactory();

      LocalStreamManager getLocalStreamManager();

      OrderedUpdatesManager getOrderedUpdatesManager();

      RecoveryManager getRecoveryManager();

      StateConsumer getStateConsumer();

      StateProvider getStateProvider();

      StateTransferManager getStateTransferManager();

      StateTransferLock getStateTransferLock();

      TimeService getTimeService();

      TransactionTable getTransactionTable();

      XSiteStateConsumer getXSiteStateConsumer();

      XSiteStateProvider getXSiteStateProvider();
   }
}
