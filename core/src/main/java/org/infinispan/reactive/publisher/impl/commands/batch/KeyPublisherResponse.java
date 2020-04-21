package org.infinispan.reactive.publisher.impl.commands.batch;

import java.util.Set;
import java.util.function.ObjIntConsumer;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.commons.util.IntSet;
import org.infinispan.marshall.protostream.impl.MarshallableArray;
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
   final Object[] extraObjects;
   final Object[] keys;
   final int keySize;

   public KeyPublisherResponse(Object[] results, IntSet completedSegments, IntSet lostSegments, int size,
                               boolean complete, Object[] extraObjects, int extraSize, Object[] keys, int keySize) {
      super(results, completedSegments, lostSegments, size, complete, extraSize);
      this.extraObjects = extraObjects;
      this.keys = keys;
      this.keySize = keySize;
   }

   @ProtoFactory
   KeyPublisherResponse(MarshallableArray<Object> wrappedResults, Set<Integer> completedSegmentsWorkaround,
                        Set<Integer> lostSegmentsWorkaround, boolean complete, int segmentOffset,
                        MarshallableArray<Object> wrappedExtraObjects, MarshallableArray<Object> keys, int keySize) {
      super(wrappedResults, completedSegmentsWorkaround, lostSegmentsWorkaround, complete, segmentOffset);
      this.extraObjects = MarshallableArray.unwrap(wrappedExtraObjects, new Object[0]);
      this.keys = MarshallableArray.unwrap(keys, new Object[0]);
      this.keySize = keySize;
   }

   @ProtoField(number = 6, name = "extraObjects")
   MarshallableArray<Object> getWrappedExtraObjects() {
      return MarshallableArray.create(extraObjects);
   }

   @ProtoField(number = 7)
   MarshallableArray<Object> getKeys() {
      return MarshallableArray.create(keys);
   }

   @ProtoField(number = 8, defaultValue = "-1")
   int getKeySize() {
      return keySize;
   }

   // NOTE: extraSize is stored in the segmentOffset field since it isn't valid when using key tracking.
   // Normally segmentOffset is used to determine which key/entry(s) mapped to the current processing segment,
   // since we have the keys directly we don't need this field
   public int getExtraSize() {
      return segmentOffset;
   }

   public Object[] getExtraObjects() {
      return extraObjects;
   }

   @Override
   public void forEachSegmentValue(ObjIntConsumer consumer, int segment) {
      for (int i = 0; i < keySize; ++i) {
         consumer.accept(keys[i], segment);
      }
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
