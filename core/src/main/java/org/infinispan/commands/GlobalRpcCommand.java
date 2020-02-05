package org.infinispan.commands;

import java.util.concurrent.CompletionStage;

import org.infinispan.factories.GlobalComponentRegistry;

/**
 * Commands correspond to specific areas of functionality in the cluster, and can be replicated using the {@link
 * org.infinispan.remoting.inboundhandler.GlobalInboundInvocationHandler}. Implementations of this interface should not
 * rely on calls to {@link GlobalComponentRegistry#wireDependencies(Object)}, as all components should be accessed via
 * the passed {@link GlobalComponentRegistry} in the {@link #invokeAsync(GlobalComponentRegistry)} method.
 *
 * @author Ryan Emerson
 * @since 11.0
 */
public interface GlobalRpcCommand extends ReplicableCommand {
   /**
    * Invoke the command asynchronously.
    */
   default CompletionStage<?> invokeAsync(GlobalComponentRegistry globalComponentRegistry) throws Throwable {
      return invokeAsync();
   }
}
