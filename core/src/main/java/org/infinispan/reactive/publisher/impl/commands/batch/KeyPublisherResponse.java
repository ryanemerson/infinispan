package org.infinispan.reactive.publisher.impl.commands.batch;

import java.util.Set;
import java.util.function.ObjIntConsumer;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.commons.util.IntSet;
import org.infinispan.marshall.protostream.impl.MarshallableCollection;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * A Publisher Response that is used when key tracking is enabled. This is used in cases when EXACTLY_ONCE delivery
 * guarantee is needed and a map (that isn't encoder based) or flat map operation is required.
 * <p>
 * The keys array will hold all of the original keys for the mapped/flatmapped values.
 * <p>
 * The extraObjects array will only be required when using flatMap based operation. This is required as some flat map
 * operations may return more than one value. In this case it is possible to overflow the results array (sized based on
 * batch size). However since we are tracking by key we must retain all values that map to a given key in the response.
 */
@ProtoTypeId(ProtoStreamTypeIds.KEY_PUBLISHER_RESPONSE)
public class KeyPublisherResponse extends PublisherResponse {
   @ProtoField(number = 6)
   final MarshallableCollection<Object> extraObjects;

   @ProtoField(number = 7)
   final MarshallableCollection<Object> keys;

   @ProtoField(number = 8, defaultValue = "-1")
   final int keySize;

   @ProtoFactory
   KeyPublisherResponse(MarshallableCollection<Object> results, Set<Integer> completedSegmentsWorkaround,
                        Set<Integer> lostSegmentsWorkaround, boolean complete, int segmentOffset,
                        MarshallableCollection<Object> extraObjects, MarshallableCollection<Object> keys, int keySize) {
      super(results, completedSegmentsWorkaround, lostSegmentsWorkaround, complete, segmentOffset);
      this.extraObjects = extraObjects;
      this.keys = keys;
      this.keySize = keySize;
   }

   public KeyPublisherResponse(Object[] results, IntSet completedSegments, IntSet lostSegments, int size,
                               boolean complete, Object[] extraObjects, int extraSize, Object[] keys, int keySize) {
      super(results, completedSegments, lostSegments, size, complete, extraSize);
      this.extraObjects = MarshallableCollection.create(extraObjects);
      this.keys = MarshallableCollection.create(keys);
      this.keySize = keySize;
   }

   // NOTE: extraSize is stored in the segmentOffset field since it isn't valid when using key tracking.
   // Normally segmentOffset is used to determine which key/entry(s) mapped to the current processing segment,
   // since we have the keys directly we don't need this field
   public int getExtraSize() {
      return segmentOffset;
   }

   public Object[] getExtraObjects() {
      return MarshallableCollection.unwrapAsArray(extraObjects, Object[]::new);
   }

   @Override
   public void forEachSegmentValue(ObjIntConsumer consumer, int segment) {
      for (Object key : MarshallableCollection.unwrap(keys))
         consumer.accept(key, segment);
   }

   @Override
   public String toString() {
      return "PublisherResponse{" +
            "size=" + size +
            ", extraSize=" + segmentOffset +
            ", keySize=" + keySize +
            ", completedSegments=" + completedSegments +
            ", lostSegments=" + lostSegments +
            ", complete=" + complete +
            '}';
   }
}
