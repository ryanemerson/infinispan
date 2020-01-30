package org.infinispan.reactive.publisher.impl.commands.reduction;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import org.infinispan.commands.TopologyAffectedCommand;
import org.infinispan.commands.functional.functions.InjectableComponent;
import org.infinispan.commands.remote.BaseRpcCommand;
import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.commons.util.IntSet;
import org.infinispan.commons.util.IntSets;
import org.infinispan.commons.util.Util;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.marshall.protostream.impl.MarshallableObject;
import org.infinispan.marshall.protostream.impl.MarshallableUserCollection;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.reactive.publisher.impl.DeliveryGuarantee;
import org.infinispan.reactive.publisher.impl.LocalPublisherManager;
import org.infinispan.util.ByteString;

/**
 * Stream request command that is sent to remote nodes handle execution of remote intermediate and terminal operations.
 * @param <K> the key type
 */
@ProtoTypeId(ProtoStreamTypeIds.REDUCTION_PUBLISHER_REQUEST_COMMAND)
public class ReductionPublisherRequestCommand<K> extends BaseRpcCommand implements TopologyAffectedCommand {
   public static final byte COMMAND_ID = 31;

   @ProtoField(number = 2, defaultValue = "-1")
   int topologyId;

   @ProtoField(number = 3, defaultValue = "false")
   final boolean parallelStream;

   @ProtoField(number = 4)
   final DeliveryGuarantee deliveryGuarantee;

//   @ProtoField(number = 5)
   final IntSet segments;

   // TODO remove once IPROTO-131 issue fixed
   @ProtoField(number = 5, collectionImplementation = HashSet.class)
   final Set<Integer> segmentsWorkaround;

   @ProtoField(number = 6, defaultValue = "false")
   final boolean includeLoader;

   @ProtoField(number = 7, defaultValue = "false")
   final boolean entryStream;

   private Set<K> keys;
   private Set<K> excludedKeys;
   private Function<?, ?> transformer;
   private Function<?, ?> finalizer;

   @ProtoFactory
   ReductionPublisherRequestCommand(ByteString cacheName, int topologyId, boolean parallelStream,
                                    DeliveryGuarantee deliveryGuarantee, Set<Integer> segmentsWorkaround,
                                    boolean includeLoader, boolean entryStream, MarshallableUserCollection<K> keys,
                                    MarshallableUserCollection<K> excludedKeys, MarshallableObject<Function<?, ?>> transformer,
                                    MarshallableObject<Function<?, ?>> finalizer) {
      this(cacheName, parallelStream, deliveryGuarantee, segmentsWorkaround == null ? null : IntSets.from(segmentsWorkaround),
            MarshallableUserCollection.unwrapAsSet(keys), MarshallableUserCollection.unwrapAsSet(excludedKeys), includeLoader,
            entryStream, MarshallableObject.unwrap(transformer), MarshallableObject.unwrap(finalizer));
      this.topologyId = topologyId;
   }

   public ReductionPublisherRequestCommand(ByteString cacheName, boolean parallelStream, DeliveryGuarantee deliveryGuarantee,
         IntSet segments, Set<K> keys, Set<K> excludedKeys, boolean includeLoader, boolean entryStream,
         Function<?, ?> transformer, Function<?, ?> finalizer) {
      super(cacheName);
      this.parallelStream = parallelStream;
      this.deliveryGuarantee = deliveryGuarantee;
      this.segments = segments;
      this.keys = keys;
      this.excludedKeys = excludedKeys;
      this.includeLoader = includeLoader;
      this.entryStream = entryStream;
      this.transformer = transformer;
      this.finalizer = finalizer;
      this.segmentsWorkaround = new HashSet<>(segments);
   }

   @ProtoField(number = 8)
   MarshallableUserCollection<K> getKeys() {
      return MarshallableUserCollection.create(keys);
   }

   @ProtoField(number = 9)
   MarshallableUserCollection<K> getExcludedKeys() {
      return MarshallableUserCollection.create(excludedKeys);
   }

   @ProtoField(number = 10)
   MarshallableObject<Function<?, ?>> getTransformer() {
      // If transformer is the same as the finalizer, then only set the finalizer field
      return transformer == finalizer ? null : MarshallableObject.create(transformer);
   }

   @ProtoField(number = 11)
   MarshallableObject<Function<?, ?>>  getFinalizer() {
      return MarshallableObject.create(finalizer);
   }

   @Override
   public CompletionStage<?> invokeAsync(ComponentRegistry componentRegistry) throws Throwable {
      if (transformer instanceof InjectableComponent) {
         ((InjectableComponent) transformer).inject(componentRegistry);
      }
      if (finalizer instanceof InjectableComponent) {
         ((InjectableComponent) finalizer).inject(componentRegistry);
      }
      LocalPublisherManager lpm = componentRegistry.getLocalPublisherManager().running();
      if (entryStream) {
         return lpm.entryReduction(parallelStream, segments, keys, excludedKeys,
               includeLoader, deliveryGuarantee, transformer, finalizer);
      } else {
         return lpm.keyReduction(parallelStream, segments, keys, excludedKeys,
               includeLoader, deliveryGuarantee, transformer, finalizer);
      }
   }

   @Override
   public byte getCommandId() {
      return COMMAND_ID;
   }

   @Override
   public int getTopologyId() {
      return topologyId;
   }

   @Override
   public void setTopologyId(int topologyId) {
      this.topologyId = topologyId;
   }

   @Override
   public boolean isReturnValueExpected() {
      return true;
   }

   @Override
   public String toString() {
      return "PublisherRequestCommand{" +
            ", includeLoader=" + includeLoader +
            ", topologyId=" + topologyId +
            ", segments=" + segments +
            ", keys=" + Util.toStr(keys) +
            ", excludedKeys=" + Util.toStr(excludedKeys) +
            ", transformer= " + transformer +
            ", finalizer=" + finalizer +
            '}';
   }
}
