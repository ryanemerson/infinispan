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

   @ProtoField(number = 2)
   final String requestId;

   @ProtoField(number = 3)
   final DeliveryGuarantee deliveryGuarantee;

   @ProtoField(number = 4, defaultValue = "-1")
   final int batchSize;

   // TODO workaround
//   @ProtoField(number = 5)
   final IntSet segments;

   // TODO remove once IPROTO-131 issue fixed
   @ProtoField(number = 5, collectionImplementation = HashSet.class)
   protected Set<Integer> segmentsWorkaround;

   @ProtoField(number = 6)
   final MarshallableCollection<K> keys;

   @ProtoField(number = 7)
   final MarshallableCollection<K> excludedKeys;

   @ProtoField(number = 8, defaultValue = "false")
   final boolean includeLoader;

   @ProtoField(number = 9, defaultValue = "false")
   final boolean entryStream;

   @ProtoField(number = 10, defaultValue = "false")
   final boolean trackKeys;

   @ProtoField(number = 11)
   final MarshallableObject<Function<? super Publisher<I>, ? extends Publisher<R>>> transformer;

   @ProtoField(number = 12, defaultValue = "-1")
   int topologyId = -1;

   @ProtoFactory
   InitialPublisherCommand(ByteString cacheName, String requestId, DeliveryGuarantee deliveryGuarantee, int batchSize,
                           Set<Integer> segmentsWorkaround, MarshallableCollection<K> keys, MarshallableCollection<K> excludedKeys,
                           boolean includeLoader, boolean entryStream, boolean trackKeys,
                           MarshallableObject<Function<? super Publisher<I>, ? extends Publisher<R>>> transformer, int topologyId) {
      super(cacheName);
      this.requestId = requestId;
      this.deliveryGuarantee = deliveryGuarantee;
      this.batchSize = batchSize;
      this.segments = segmentsWorkaround == null ? null : IntSets.from(segmentsWorkaround);
      this.segmentsWorkaround = segmentsWorkaround;
      this.keys = keys;
      this.excludedKeys = excludedKeys;
      this.includeLoader = includeLoader;
      this.entryStream = entryStream;
      this.trackKeys = trackKeys;
      this.transformer = transformer;
      this.topologyId = topologyId;
   }

   public InitialPublisherCommand(ByteString cacheName, String requestId, DeliveryGuarantee deliveryGuarantee,
                                  int batchSize, IntSet segments, Set<K> keys, Set<K> excludedKeys, boolean includeLoader, boolean entryStream,
                                  boolean trackKeys, Function<? super Publisher<I>, ? extends Publisher<R>> transformer) {
      super(cacheName);
      this.requestId = requestId;
      this.deliveryGuarantee = deliveryGuarantee;
      this.batchSize = batchSize;
      this.segments = segments;
      this.segmentsWorkaround = new HashSet<>(segments);
      this.keys = MarshallableCollection.create(keys);
      this.excludedKeys = MarshallableCollection.create(excludedKeys);
      this.includeLoader = includeLoader;
      this.entryStream = entryStream;
      this.trackKeys = trackKeys;
      this.transformer = MarshallableObject.create(transformer);
   }

   public String getRequestId() {
      return requestId;
   }

   public DeliveryGuarantee getDeliveryGuarantee() {
      return deliveryGuarantee;
   }

   public int getBatchSize() {
      return batchSize;
   }

   public IntSet getSegments() {
      return segments;
   }

   public Set<K> getKeys() {
      return MarshallableCollection.unwrapAsSet(keys);
   }

   public Set<K> getExcludedKeys() {
      return MarshallableCollection.unwrapAsSet(excludedKeys);
   }

   public boolean isIncludeLoader() {
      return includeLoader;
   }

   public boolean isEntryStream() {
      return entryStream;
   }

   public boolean isTrackKeys() {
      return trackKeys;
   }

   public Function<? super Publisher<I>, ? extends Publisher<R>> getTransformer() {
      return MarshallableObject.unwrap(transformer);
   }

   @Override
   public CompletionStage<?> invokeAsync(ComponentRegistry componentRegistry) throws Throwable {
      if (transformer instanceof InjectableComponent) {
         ((InjectableComponent) transformer).inject(componentRegistry);
      }

      PublisherHandler publisherHandler = componentRegistry.getPublisherHandler().running();
      return publisherHandler.register(this);
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
   public byte getCommandId() {
      return COMMAND_ID;
   }

   @Override
   public boolean isReturnValueExpected() {
      return true;
   }
}
