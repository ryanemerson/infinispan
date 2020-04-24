package org.infinispan.reactive.publisher.impl.commands.batch;

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
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.marshall.protostream.impl.MarshallableCollection;
import org.infinispan.marshall.protostream.impl.MarshallableObject;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.reactive.publisher.impl.DeliveryGuarantee;
import org.infinispan.reactive.publisher.impl.PublisherHandler;
import org.infinispan.util.ByteString;
import org.reactivestreams.Publisher;

@ProtoTypeId(ProtoStreamTypeIds.INITIAL_PUBLISHER_COMMAND)
public class InitialPublisherCommand<K, I, R> extends BaseRpcCommand implements TopologyAffectedCommand {
   public static final byte COMMAND_ID = 18;

   private String requestId;
   private DeliveryGuarantee deliveryGuarantee;
   private int batchSize;
   private IntSet segments;
   private Set<K> keys;
   private Set<K> excludedKeys;
   private boolean includeLoader;
   private boolean entryStream;
   private boolean trackKeys;
   private Function<? super Publisher<I>, ? extends Publisher<R>> transformer;
   private int topologyId = -1;
   public InitialPublisherCommand(ByteString cacheName, String requestId, DeliveryGuarantee deliveryGuarantee,
         int batchSize, IntSet segments, Set<K> keys, Set<K> excludedKeys, boolean includeLoader, boolean entryStream,
         boolean trackKeys, Function<? super Publisher<I>, ? extends Publisher<R>> transformer) {
      super(cacheName);
      this.requestId = requestId;
      this.deliveryGuarantee = deliveryGuarantee;
      this.batchSize = batchSize;
      this.segments = segments;
      this.keys = keys;
      this.excludedKeys = excludedKeys;
      this.includeLoader = includeLoader;
      this.entryStream = entryStream;
      this.trackKeys = trackKeys;
      this.transformer = transformer;
   }


   @ProtoFactory
   InitialPublisherCommand(ByteString cacheName, String requestId, DeliveryGuarantee deliveryGuarantee, int batchSize,
                           Set<Integer> segmentsWorkaround, MarshallableCollection<K> wrappedKeys, MarshallableCollection<K> wrappedExcludedKeys,
                           boolean includeLoader, boolean entryStream, boolean trackKeys, int topologyId,
                           MarshallableObject<Function<? super Publisher<I>, ? extends Publisher<R>>> wrappedTransformer) {
      super(cacheName);
      this.requestId = requestId;
      this.deliveryGuarantee = deliveryGuarantee;
      this.batchSize = batchSize;
      this.segments = segmentsWorkaround == null ? null : IntSets.from(segmentsWorkaround);
      this.keys = MarshallableCollection.unwrapAsSet(wrappedKeys);
      this.excludedKeys = MarshallableCollection.unwrapAsSet(wrappedExcludedKeys);
      this.includeLoader = includeLoader;
      this.entryStream = entryStream;
      this.trackKeys = trackKeys;
      this.transformer = MarshallableObject.unwrap(wrappedTransformer);
      this.topologyId = topologyId;
   }

   @ProtoField(number = 2)
   public String getRequestId() {
      return requestId;
   }

   @ProtoField(number = 3)
   public DeliveryGuarantee getDeliveryGuarantee() {
      return deliveryGuarantee;
   }

   @ProtoField(number = 4, defaultValue = "-1")
   public int getBatchSize() {
      return batchSize;
   }

   // TODO workaround
//   @ProtoField(number = 5)
   public IntSet getSegments() {
      return segments;
   }

   @ProtoField(number = 5, collectionImplementation = HashSet.class)
   protected Set<Integer> getSegmentsWorkaround() {
      return new HashSet<>(segments);
   }

   public Set<K> getKeys() {
      return keys;
   }

   @ProtoField(number = 6, name = "keys")
   MarshallableCollection<K> getWrappedKeys() {
      return MarshallableCollection.create(keys);
   }

   public Set<K> getExcludedKeys() {
      return excludedKeys;
   }

   @ProtoField(number = 7, name = "excludedKeys")
   MarshallableCollection<K> getWrappedExcludedKeys() {
      return MarshallableCollection.create(excludedKeys);
   }

   @ProtoField(number = 8, defaultValue = "false")
   public boolean isIncludeLoader() {
      return includeLoader;
   }

   @ProtoField(number = 9, defaultValue = "false")
   public boolean isEntryStream() {
      return entryStream;
   }

   @ProtoField(number = 10, defaultValue = "false")
   public boolean isTrackKeys() {
      return trackKeys;
   }

   public Function<? super Publisher<I>, ? extends Publisher<R>> getTransformer() {
      return transformer;
   }

   @ProtoField(number = 11, name = "transformer")
   MarshallableObject<Function<? super Publisher<I>, ? extends Publisher<R>>> getWrappedTransformer() {
      return MarshallableObject.create(transformer);
   }

   @Override
   @ProtoField(number = 12, defaultValue = "-1")
   public int getTopologyId() {
      return topologyId;
   }

   @Override
   public void setTopologyId(int topologyId) {
      this.topologyId = topologyId;
   }

   @Override
   public byte getCommandId() {
      return COMMAND_ID;
   }

   @Override
   public boolean isReturnValueExpected() {
      return true;
   }

   @Override
   public CompletionStage<?> invokeAsync(ComponentRegistry componentRegistry) throws Throwable {
      if (transformer instanceof InjectableComponent) {
         ((InjectableComponent) transformer).inject(componentRegistry);
      }

      PublisherHandler publisherHandler = componentRegistry.getPublisherHandler().running();
      return publisherHandler.register(this);
   }
}
