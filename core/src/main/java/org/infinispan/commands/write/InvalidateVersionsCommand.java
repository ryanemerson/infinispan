package org.infinispan.commands.write;

import java.util.concurrent.CompletionStage;

import org.infinispan.commands.VisitableCommand;
import org.infinispan.commands.remote.BaseRpcCommand;
import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.container.DataContainer;
import org.infinispan.container.versioning.InequalVersionComparisonResult;
import org.infinispan.container.versioning.SimpleClusteredVersion;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.marshall.protostream.impl.MarshallableCollection;
import org.infinispan.persistence.manager.OrderedUpdatesManager;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.scattered.BiasManager;
import org.infinispan.statetransfer.StateTransferLock;
import org.infinispan.util.ByteString;
import org.infinispan.util.concurrent.CompletableFutures;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * Must be {@link VisitableCommand} as we want to catch it in persistence handling etc.
 *
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
@ProtoTypeId(ProtoStreamTypeIds.INVALIDATE_VERSIONS_COMMAND)
public class InvalidateVersionsCommand extends BaseRpcCommand {
   private static final Log log = LogFactory.getLog(InvalidateVersionsCommand.class);
   private static final boolean trace = log.isTraceEnabled();

   public static final int COMMAND_ID = 67;

   private final Object[] keys;

   // This is the topologyId in which this command is valid (in case that it comes from the state transfer)
   @ProtoField(number = 2, defaultValue = "-1")
   final int topologyId;

   @ProtoField(number = 3)
   final int[] topologyIds;

   @ProtoField(number = 4)
   final long[] versions;

   // Removed means that the comparison will remove current versions as well
   @ProtoField(number = 5, defaultValue = "false")
   final boolean removed;

   @ProtoFactory
   InvalidateVersionsCommand(ByteString cacheName, int topologyId, MarshallableCollection<Object> keys,
                             int[] topologyIds, long[] versions, boolean removed) {
      this(cacheName, topologyId, MarshallableCollection.unwrapAsArray(keys, Object[]::new), topologyIds, versions, removed);
   }

   @ProtoField(number = 6)
   MarshallableCollection<Object> getKeys() {
      return MarshallableCollection.create(keys);
   }

   public InvalidateVersionsCommand(ByteString cacheName, int topologyId, Object[] keys, int[] topologyIds,
                                    long[] versions, boolean removed) {
      super(cacheName);
      this.topologyId = topologyId;
      this.keys = keys;
      this.topologyIds = topologyIds;
      this.versions = versions;
      this.removed = removed;
   }

   @Override
   public CompletionStage<?> invokeAsync(ComponentRegistry componentRegistry) {
      StateTransferLock stateTransferLock = componentRegistry.getStateTransferLock();
      if (stateTransferLock != null) {
         stateTransferLock.acquireSharedTopologyLock();
      }
      try {
         DistributionManager distributionManager = componentRegistry.getDistributionManager();
         if (topologyId >= 0 && distributionManager != null) {
            int currentTopologyId = distributionManager.getCacheTopology().getTopologyId();
            if (topologyId > currentTopologyId) {
               if (trace) {
                  log.tracef("Delaying command %s, current topology id %d", this, currentTopologyId);
               }
               return stateTransferLock.topologyFuture(topologyId).thenCompose(nil -> invokeAsync(componentRegistry));
            } else if (topologyId < currentTopologyId) {
               log.ignoringInvalidateVersionsFromOldTopology(this.topologyId, currentTopologyId);
               if (trace) {
                  log.tracef("Ignored command is %s", this);
               }
               return CompletableFutures.completedNull();
            }
         }
         for (int i = 0; i < keys.length; ++i) {
            Object key = keys[i];
            if (key == null) break;
            SimpleClusteredVersion version = new SimpleClusteredVersion(topologyIds[i], versions[i]);
            BiasManager biasManager = componentRegistry.getBiasManager().running();
            if (biasManager != null) {
               biasManager.revokeLocalBias(key);
            }
            DataContainer dataContainer = componentRegistry.getInternalDataContainer().running();
            dataContainer.compute(key, (k, oldEntry, factory) -> {
               if (oldEntry == null) {
                  return null;
               }
               SimpleClusteredVersion localVersion = (SimpleClusteredVersion) oldEntry.getMetadata().version();
               InequalVersionComparisonResult result = localVersion.compareTo(version);
               if (result == InequalVersionComparisonResult.BEFORE || (removed && result == InequalVersionComparisonResult.EQUAL)) {
                  return null;
               } else {
                  return oldEntry;
               }
            });
         }
      } finally {
         if (stateTransferLock != null) {
            stateTransferLock.releaseSharedTopologyLock();
         }
      }
      OrderedUpdatesManager orderedUpdatesManager = componentRegistry.getOrderedUpdatesManager().running();
      return orderedUpdatesManager.invalidate(keys).thenApply(nil -> null).toCompletableFuture();
   }

   @Override
   public byte getCommandId() {
      return COMMAND_ID;
   }

   @Override
   public boolean isReturnValueExpected() {
      return false;
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("InvalidateVersionsCommand{topologyId=").append(topologyId)
            .append(", removed=").append(removed).append(": ");
      if (keys.length > 0 && keys[0] != null) {
         sb.append(keys[0]).append(" -> ").append(versions[0]);
      } else {
         sb.append("<no-keys>");
      }
      for (int i = 1; i < keys.length; ++i) {
         if (keys[i] == null) break;
         sb.append(", ").append(keys[i]).append(" -> ").append(versions[i]);
      }
      return sb.append("}").toString();
   }
}
