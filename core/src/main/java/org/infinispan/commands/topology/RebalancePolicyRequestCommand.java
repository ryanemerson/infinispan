package org.infinispan.commands.topology;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.infinispan.commons.marshall.MarshallUtil;
import org.infinispan.factories.GlobalComponentRegistry;

/**
 * Check whether rebalancing is enabled.
 *
 * @author Ryan Emerson
 * @since 11.0
 */
public class RebalancePolicyRequestCommand extends AbstractCacheControlCommand {

   public static final byte COMMAND_ID = 91;

   private String cacheName;

   // For CommandIdUniquenessTest only
   public RebalancePolicyRequestCommand() {
      super(COMMAND_ID);
   }

   public RebalancePolicyRequestCommand(String cacheName) {
      super(COMMAND_ID);
      this.cacheName = cacheName;
   }

   @Override
   public CompletionStage<Boolean> invokeAsync(GlobalComponentRegistry gcr) throws Throwable {
      boolean enabled = gcr.getClusterTopologyManager().isRebalancingEnabled(cacheName);
      return CompletableFuture.completedFuture(enabled);
   }

   @Override
   public void writeTo(ObjectOutput output) throws IOException {
      MarshallUtil.marshallString(cacheName, output);
   }

   @Override
   public void readFrom(ObjectInput input) throws IOException, ClassNotFoundException {
      cacheName = MarshallUtil.unmarshallString(input);
   }

   @Override
   public String toString() {
      return "RebalancePolicyCommand{" +
            "cacheName='" + cacheName + '\'' +
            '}';
   }
}
