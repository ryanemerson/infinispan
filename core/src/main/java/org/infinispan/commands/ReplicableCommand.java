package org.infinispan.commands;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.infinispan.context.InvocationContext;
import org.infinispan.marshall.core.MarshalledEntryFactory;
import org.infinispan.marshall.core.UserAwareObjectOutput;
import org.infinispan.remoting.transport.Address;
import org.infinispan.util.concurrent.CompletableFutures;

/**
 * The core of the command-based cache framework.  Commands correspond to specific areas of functionality in the cache,
 * and can be replicated using the {@link org.infinispan.remoting.rpc.RpcManager}
 *
 * @author Mircea.Markus@jboss.com
 * @author Manik Surtani
 * @since 4.0
 */
public interface ReplicableCommand {
   /**
    * Invoke the command asynchronously.
    * <p>
    * <p>This method replaces {@link #perform(InvocationContext)} for remote execution.
    * The default implementation and {@link #perform(InvocationContext)} will be removed in future versions.
    * </p>
    *
    * @since 9.0
    */
   default CompletableFuture<Object> invokeAsync() throws Throwable {
      return CompletableFuture.completedFuture(perform(null));
   }

   /**
    * Invoke the command synchronously.
    * <p>
    * <p>This method is optional. Unless your command never blocks, please implement {@link #invokeAsync()} instead.</p>
    *
    * @since 9.0
    */
   default Object invoke() throws Throwable {
      try {
         return invokeAsync().join();
      } catch (CompletionException e) {
         throw CompletableFutures.extractException(e);
      }
   }

   /**
    * Performs the primary function of the command.  Please see specific implementation classes for details on what is
    * performed as well as return types. <b>Important</b>: this method will be invoked at the end of interceptors chain.
    * It should never be called directly from a custom interceptor.
    *
    * @param ctx invocation context
    * @return arbitrary return value generated by performing this command
    * @throws Throwable in the event of problems.
    * @deprecated Since 9.0, split into {@link #invokeAsync()} and {@link VisitableCommand#perform(InvocationContext)}.
    */
   @Deprecated
   default Object perform(InvocationContext ctx) throws Throwable {
      return invoke();
   }

   /**
    * Used by marshallers to convert this command into an id for streaming.
    *
    * @return the method id of this command.  This is compatible with pre-2.2.0 MethodCall ids.
    */
   byte getCommandId();

   /**
    * If true, a return value will be provided when performed remotely.  Otherwise, a remote {@link
    * org.infinispan.remoting.responses.ResponseGenerator} may choose to simply return null to save on marshalling
    * costs.
    *
    * @return true or false
    */
   boolean isReturnValueExpected();

   /**
    * If true, a return value will be marshalled as a {@link org.infinispan.remoting.responses.SuccessfulResponse},
    * otherwise it will be marshalled as a {@link org.infinispan.remoting.responses.UnsuccessfulResponse}.
    */
   default boolean isSuccessful() {
      return true;
   }

   /**
    * If true, the command is processed asynchronously in a thread provided by an Infinispan thread pool. Otherwise,
    * the command is processed directly in the JGroups thread.
    * <p/>
    * This feature allows to avoid keep a JGroups thread busy that can originate discard of messages and
    * retransmissions. So, the commands that can block (waiting for some state, acquiring locks, etc.) should return
    * true.
    *
    * @return {@code true} if the command can block/wait, {@code false} otherwise
    */
   boolean canBlock();

   /**
    * Writes this instance to the {@link ObjectOutput}.
    *
    * @param output the stream.
    * @throws IOException if an error occurred during the I/O.
    * @deprecated since 9.4 use {@link #writeTo(UserAwareObjectOutput, MarshalledEntryFactory)} instead
    */
   @Deprecated
   default void writeTo(ObjectOutput output) throws IOException {
      // no-op
   }

   /**
    * Writes this instance to the {@link ObjectOutput}.
    *
    * @since 9.4
    * @param output the stream.
    * @param entryFactory the {@link MarshalledEntryFactory} that should be used to marshall all user objects such as key/entries/metadata
    * @throws IOException if an error occurred during the I/O.
    */
   default void writeTo(UserAwareObjectOutput output, MarshalledEntryFactory entryFactory) throws IOException {
      writeTo(output);
   }

   /**
    * Reads this instance from the stream written by {@link #writeTo(UserAwareObjectOutput, MarshalledEntryFactory)}.
    *
    * @param input the stream to read.
    * @throws IOException            if an error occurred during the I/O.
    * @throws ClassNotFoundException if it tries to load an undefined class.
    */
   void readFrom(ObjectInput input) throws IOException, ClassNotFoundException;

   /**
    * Sets the sender's {@link Address}.
    * <p>
    * By default, it doesn't set anything. Implement this method if the sender's {@link Address} is needed.
    *
    * @param origin the sender's {@link Address}
    */
   default void setOrigin(Address origin) {
      //no-op by default
   }
}
