package org.infinispan.remoting.rpc;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import org.infinispan.commands.ReplicableCommand;
import org.infinispan.commands.remote.CacheRpcCommand;
import org.infinispan.remoting.inboundhandler.DeliverOrder;
import org.infinispan.remoting.responses.Response;
import org.infinispan.remoting.transport.Address;
import org.infinispan.remoting.transport.ResponseCollector;
import org.infinispan.remoting.transport.Transport;
import org.infinispan.remoting.transport.XSiteResponse;
import org.infinispan.xsite.XSiteBackup;
import org.infinispan.xsite.commands.remote.XSiteCacheRequest;

/**
 * Provides a mechanism for communicating with other caches in the cluster, by formatting and passing requests down to
 * the registered {@link Transport}.
 *
 * @author Manik Surtani
 * @author Mircea.Markus@jboss.com
 * @since 4.0
 */
public interface RpcManager {
   /**
    * Invoke a command on a single node and pass the response to a {@link ResponseCollector}.
    *
    * If the target is the local node, the command is never executed and {@link ResponseCollector#finish()} is called directly.
    *
    * @since 9.2
    */
   <T> CompletionStage<T> invokeCommand(Address target, CacheRpcCommand command,
                                        ResponseCollector<Address, T> collector, RpcOptions rpcOptions);

   /**
    * Invoke a command on a collection of node and pass the responses to a {@link ResponseCollector}.
    *
    * If one of the targets is the local node, it is ignored. The command is only executed on the remote nodes.
    *
    * @since 9.2
    */
   <T> CompletionStage<T> invokeCommand(Collection<Address> targets, CacheRpcCommand command,
                                        ResponseCollector<Address, T> collector, RpcOptions rpcOptions);

   /**
    * Invoke a command on all the nodes in the cluster and pass the responses to a {@link ResponseCollector}.
    *
    * The command is not executed locally and it is not sent across RELAY2 bridges to remote sites.
    *
    * @since 9.2
    */
   <T> CompletionStage<T> invokeCommandOnAll(CacheRpcCommand command, ResponseCollector<Address, T> collector,
                                             RpcOptions rpcOptions);

   /**
    * Invoke a command on a collection of nodes and pass the responses to a {@link ResponseCollector}.
    *
    * The command is only sent immediately to the first target, and there is an implementation-dependent
    * delay before sending the command to each target. There is no delay if the target responds or leaves
    * the cluster. The remaining targets are skipped if {@link ResponseCollector#addResponse(Object, Response)}
    * returns a non-{@code null} value.
    *
    * The command is only executed on the remote nodes.
    *
    * @since 9.2
    */
   <T> CompletionStage<T> invokeCommandStaggered(Collection<Address> targets, CacheRpcCommand command,
                                                 ResponseCollector<Address, T> collector, RpcOptions rpcOptions);

   /**
    * Invoke different commands on a collection of nodes and pass the responses to a {@link ResponseCollector}.
    *
    * The command is only executed on the remote nodes and it is not executed in the local node even if it is in the {@code targets}.
    *
    * @since 9.2
    */
   <T> CompletionStage<T> invokeCommands(Collection<Address> targets,
                                         Function<Address, CacheRpcCommand> commandGenerator,
                                         ResponseCollector<Address, T> collector, RpcOptions rpcOptions);

   /**
    * Block on a request and return its result.
    *
    * @since 9.2
    */
   <T> T blocking(CompletionStage<T> request);

   /**
    * Asynchronously sends the {@link ReplicableCommand} to the destination using the specified {@link DeliverOrder}.
    *
    * @param destination  the destination's {@link Address}.
    * @param command      the {@link ReplicableCommand} to send.
    * @param deliverOrder the {@link DeliverOrder} to use.
    */
   void sendTo(Address destination, CacheRpcCommand command, DeliverOrder deliverOrder);

   /**
    * Asynchronously sends the {@link ReplicableCommand} to the set of destination using the specified {@link
    * DeliverOrder}.
    *
    * @param destinations the collection of destination's {@link Address}. If {@code null}, it sends to all the members
    *                     in the cluster.
    * @param command      the {@link ReplicableCommand} to send.
    * @param deliverOrder the {@link DeliverOrder} to use.
    */
   void sendToMany(Collection<Address> destinations, CacheRpcCommand command, DeliverOrder deliverOrder);

   /**
    * Asynchronously sends the {@link ReplicableCommand} to the entire cluster.
    *
    * @since 9.2
    */
   void sendToAll(CacheRpcCommand command, DeliverOrder deliverOrder);

   /**
    * Sends the {@link XSiteCacheRequest} to a remote site.
    * <p>
    * If {@link XSiteBackup#isSync()} returns {@code false}, the {@link XSiteResponse} is only completed when the an
    * ACK from the remote site is received. The invoker needs to make sure not to wait for the {@link XSiteResponse}.
    *
    * @param backup  The site to where the command is sent.
    * @param command The command to send.
    * @return A {@link XSiteResponse} that is completed when the request is completed.
    */
   <O> XSiteResponse<O> invokeXSite(XSiteBackup backup, XSiteCacheRequest<O> command);

   /**
    * @return a reference to the underlying transport.
    */
   Transport getTransport();


   /**
    * Returns members of a cluster scoped to the cache owning this RpcManager. Note that this List
    * is always a subset of {@link Transport#getMembers()}
    *
    * @return a list of cache scoped cluster members
    */
   List<Address> getMembers();

   /**
    * Returns the address associated with this RpcManager or null if not part of the cluster.
    */
   Address getAddress();

   /**
    * Returns the current topology id. As opposed to the viewId which is updated whenever the cluster changes,
    * the topologyId is updated when a new cache instance is started or removed - this doesn't necessarily coincide
    * with a node being added/removed to the cluster.
    */
   int getTopologyId();

   /**
    * @return The default options for synchronous remote invocations.
    */
   RpcOptions getSyncRpcOptions();

   /**
    * @return The default options for total order remote invocations.
    */
   default RpcOptions getTotalSyncRpcOptions() {
      throw new UnsupportedOperationException();
   }
}
