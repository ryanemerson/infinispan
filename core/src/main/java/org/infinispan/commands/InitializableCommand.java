package org.infinispan.commands;

import org.infinispan.Cache;
import org.infinispan.commons.time.TimeService;
import org.infinispan.conflict.impl.StateReceiver;
import org.infinispan.container.impl.InternalDataContainer;
import org.infinispan.container.impl.InternalEntryFactory;
import org.infinispan.context.InvocationContextFactory;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.interceptors.AsyncInterceptorChain;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.notifications.cachelistener.CacheNotifier;
import org.infinispan.notifications.cachelistener.cluster.ClusterCacheNotifier;
import org.infinispan.persistence.manager.OrderedUpdatesManager;
import org.infinispan.reactive.publisher.impl.LocalPublisherManager;
import org.infinispan.remoting.rpc.RpcManager;
import org.infinispan.scattered.BiasManager;
import org.infinispan.statetransfer.StateConsumer;
import org.infinispan.statetransfer.StateProvider;
import org.infinispan.statetransfer.StateTransferLock;
import org.infinispan.statetransfer.StateTransferManager;
import org.infinispan.stream.impl.ClusterStreamManager;
import org.infinispan.stream.impl.IteratorHandler;
import org.infinispan.stream.impl.LocalStreamManager;
import org.infinispan.transaction.impl.TransactionTable;
import org.infinispan.transaction.xa.recovery.RecoveryManager;
import org.infinispan.util.ByteString;
import org.infinispan.util.concurrent.CommandAckCollector;
import org.infinispan.util.concurrent.locks.LockManager;
import org.infinispan.xsite.BackupSender;
import org.infinispan.xsite.statetransfer.XSiteStateConsumer;
import org.infinispan.xsite.statetransfer.XSiteStateProvider;
import org.infinispan.xsite.statetransfer.XSiteStateTransferManager;

/**
 * An interface to be implemented by Commands which require an intialized state after deserialization.
 *
 * @author Ryan Emerson
 * @since 10.0
 */
public interface InitializableCommand {

   void init(CommandContext context, boolean isRemote);

   /**
    * Interface that exposes frequently used components in order to avoid repeated hash lookups for common components.
    * As the purpose of this interface is to avoid hash lookups, calls to {@link #getComponentRegistry()} should be
    * avoided where possible.
    */
   interface CommandContext {

      AsyncInterceptorChain getInterceptorChain();

      BackupSender getBackupSender();

      BiasManager getBiasManager();

      ByteString getCacheName();

      Cache getCache();

      CacheNotifier getCacheNotifier();

      CancellationService getCancellationService();

      CommandsFactory getCommandsFactory();

      ClusterCacheNotifier getClusterCacheNotifier();

      ClusterStreamManager getClusterStreamManager();

      CommandAckCollector getCommandAckCollector();

      ComponentRegistry getComponentRegistry();

      DistributionManager getDistributionManager();

      EmbeddedCacheManager getCacheManager();

      InternalDataContainer getDataContainer();

      InternalEntryFactory getInternalEntryFactory();

      InvocationContextFactory getInvocationContextFactory();

      IteratorHandler getIteratorHandler();

      LocalPublisherManager getLocalPublisherManager();

      LocalStreamManager getLocalStreamManager();

      LockManager getLockManager();

      OrderedUpdatesManager getOrderedUpdatesManager();

      RecoveryManager getRecoveryManager();

      RpcManager getRpcManager();

      StateConsumer getStateConsumer();

      StateProvider getStateProvider();

      StateReceiver getStateReceiver();

      StateTransferManager getStateTransferManager();

      StateTransferLock getStateTransferLock();

      TimeService getTimeService();

      TransactionTable getTransactionTable();

      XSiteStateConsumer getXSiteStateConsumer();

      XSiteStateProvider getXSiteStateProvider();

      XSiteStateTransferManager getXSiteStateTransferManager();
   }
}
