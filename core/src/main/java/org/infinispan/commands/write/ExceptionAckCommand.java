package org.infinispan.commands.write;

import org.infinispan.commons.CacheException;
import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.marshall.protostream.impl.MarshallableThrowable;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.remoting.transport.ResponseCollectors;
import org.infinispan.util.ByteString;
import org.infinispan.util.concurrent.CommandAckCollector;

/**
 * A command that represents an exception acknowledge sent by any owner.
 * <p>
 * The acknowledge represents an unsuccessful execution of the operation.
 *
 * @author Pedro Ruivo
 * @since 9.0
 */
@ProtoTypeId(ProtoStreamTypeIds.EXCEPTION_ACK_COMMAND)
public class ExceptionAckCommand extends BackupAckCommand {
   public static final byte COMMAND_ID = 42;

   @ProtoField(number = 4)
   MarshallableThrowable throwable;

   @ProtoFactory
   ExceptionAckCommand(ByteString cacheName, long id, int topologyId, MarshallableThrowable throwable) {
      super(cacheName, id, topologyId);
      this.throwable = throwable;
   }

   public ExceptionAckCommand(ByteString cacheName, long id, Throwable throwable, int topologyId) {
      super(cacheName, id, topologyId);
      this.throwable = MarshallableThrowable.create(throwable);
   }

   @Override
   public void ack(CommandAckCollector ackCollector) {
      CacheException remoteException = ResponseCollectors.wrapRemoteException(getOrigin(), throwable.get());
      ackCollector.completeExceptionally(id, remoteException, topologyId);
   }

   @Override
   public byte getCommandId() {
      return COMMAND_ID;
   }

   @Override
   public String toString() {
      return "ExceptionAckCommand{" +
            "id=" + id +
            ", throwable=" + throwable +
            ", topologyId=" + topologyId +
            '}';
   }
}
