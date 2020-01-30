package org.infinispan.reactive.publisher.impl.commands.batch;

import java.util.List;
import java.util.Set;
import java.util.function.ObjIntConsumer;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.commons.util.IntSet;
import org.infinispan.marshall.protostream.impl.MarshallableArray;
import org.infinispan.marshall.protostream.impl.MarshallableCollection;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.reactive.publisher.impl.PublisherHandler;

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
   final int extraSize;
   final Object[] keys;
   // Note that after being deserialized this is always equal to keySize.length - local this will be how many entries
   // are in the array
   final int keySize;

   public KeyPublisherResponse(Object[] results, IntSet completedSegments, IntSet lostSegments, int size,
         boolean complete, List<PublisherHandler.SegmentResult> segmentResults, Object[] extraObjects, int extraSize,
         Object[] keys, int keySize) {
      super(results, completedSegments, lostSegments, size, complete, segmentResults);
      this.extraObjects = extraObjects;
      this.extraSize = extraSize;
      this.keys = keys;
      this.keySize = keySize;
   }

   @ProtoFactory
   KeyPublisherResponse(MarshallableArray<Object> wrappedResults, Set<Integer> completedSegmentsSet,
                        Set<Integer> lostSegmentsSet, boolean complete,
                        MarshallableCollection<PublisherHandler.SegmentResult> wrappedSegmentResults,
                        MarshallableArray<Object> keys) {
      super(wrappedResults, completedSegmentsSet, lostSegmentsSet, complete, wrappedSegmentResults);
      this.extraObjects = null;
      this.extraSize = 0;
      this.keys = MarshallableArray.unwrap(keys, new Object[0]);
      this.keySize = this.keys.length;
   }

   @Override
   MarshallableArray<Object> wrappedResults() {
      if (extraObjects.length > 0) {
         Object[] r = new Object[this.results.length + extraObjects.length];
         int i = 0;
         for (Object result : this.results) r[i++] = result;
         for (Object result : this.extraObjects) r[i++] = result;
         return MarshallableArray.create(r);
      }
      return MarshallableArray.create(results);
   }

   @ProtoField(6)
   MarshallableArray<Object> getKeys() {
      return MarshallableArray.create(keys);
   }


   // NOTE: extraSize is stored in the segmentOffset field since it isn't valid when using key tracking.
   // Normally segmentOffset is used to determine which key/entry(s) mapped to the current processing segment,
   // since we have the keys directly we don't need this field
   public int getExtraSize() {
      return extraSize;
   }

   public Object[] getExtraObjects() {
      return extraObjects;
   }

   @Override
   public void keysForNonCompletedSegments(ObjIntConsumer consumer) {
      int size = segmentResults.size();
      if (size == 0) {
         return;
      }
      PublisherHandler.SegmentResult segmentResult = segmentResults.get(segmentResults.size() - 1);
      int segment = segmentResult.getSegment();
      for (int i = 0; i < keySize; ++i) {
         consumer.accept(keys[i], segment);
      }
   }

   @Override
   public String toString() {
      return "KeyPublisherResponse{" +
            "size=" + size +
            ", completedSegments=" + completedSegments +
            ", lostSegments=" + lostSegments +
            ", complete=" + complete +
            ", segmentResults=" + segmentResults +
            ", extraSize=" + extraSize +
            ", keySize=" + keySize +
            '}';
   }
}
