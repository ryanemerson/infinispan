package org.infinispan.xsite;

import java.util.concurrent.CompletionStage;

import org.infinispan.commands.VisitableCommand;
import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.marshall.protostream.impl.MarshallableObject;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.util.ByteString;

/**
 * RPC command to replicate cache operations (such as put, remove, replace, etc.) to the backup site.
 *
 * @author Pedro Ruivo
 * @since 7.0
 */
@ProtoTypeId(ProtoStreamTypeIds.XSITE_SINGLE_RPC_COMMAND)
public class SingleXSiteRpcCommand extends XSiteReplicateCommand {

   public static final byte COMMAND_ID = 40;
   private final VisitableCommand command;

   public SingleXSiteRpcCommand(ByteString cacheName, VisitableCommand command) {
      super(COMMAND_ID, cacheName);
      this.command = command;
   }

   @ProtoFactory
   SingleXSiteRpcCommand(ByteString cacheName, MarshallableObject<VisitableCommand> command) {
      this(cacheName, MarshallableObject.unwrap(command));
   }

   @ProtoField(number = 2)
   MarshallableObject<VisitableCommand> getCommand() {
      return MarshallableObject.create(command);
   }

   @Override
   public CompletionStage<Void> performInLocalSite(BackupReceiver receiver, boolean preserveOrder) {
      return receiver.handleRemoteCommand(command, preserveOrder);
   }

   @Override
   public CompletionStage<?> invokeAsync(ComponentRegistry componentRegistry) throws Throwable {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean isReturnValueExpected() {
      return command.isReturnValueExpected();
   }

   @Override
   public String toString() {
      return "SingleXSiteRpcCommand{" +
            "command=" + command +
            '}';
   }
}
