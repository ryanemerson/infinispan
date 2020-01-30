package org.infinispan.reactive.publisher.impl.commands.batch;

import java.util.HashSet;
import java.util.Set;
import java.util.function.ObjIntConsumer;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.commons.util.IntSet;
import org.infinispan.commons.util.IntSets;
import org.infinispan.commons.util.Util;
import org.infinispan.marshall.protostream.impl.MarshallableCollection;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * The response for a cache publisher request to a given node. It contains an array with how many results there were,
 * which segments were completed or lost during processing, whether the operation has sent all values (complete), and
 * also an offset into the results array of which elements don't map to any of the completed segments. Note that
 * the results will never contain values for a segment that was lost in the same response.
 */
@ProtoTypeId(ProtoStreamTypeIds.PUBLISHER_RESPONSE)
public class PublisherResponse {

   @ProtoField(number = 1)
   final MarshallableCollection<Object> results;

   // The completed segments after this request - This may be null
   final IntSet completedSegments;

   // TODO remove once IPROTO-131 issue fixed
   @ProtoField(number = 2, collectionImplementation = HashSet.class)
   final Set<Integer> completedSegmentsWorkaround;

   // The segments that were lost mid processing - This may be null
   final IntSet lostSegments;

   // TODO remove once IPROTO-131 issue fixed
   @ProtoField(number = 3, collectionImplementation = HashSet.class)
   final Set<Integer> lostSegmentsWorkaround;

   @ProtoField(number = 4, defaultValue = "false")
   final boolean complete;

   @ProtoField(number = 5, defaultValue = "-1")
   final int segmentOffset;

   // How many elements are in the results
   // Note that after being deserialized this is always equal to results.length - local this will be how many entries
   // are in the array
   transient final int size;

   @ProtoFactory
   PublisherResponse(MarshallableCollection<Object> results, Set<Integer> completedSegmentsWorkaround,
                     Set<Integer> lostSegmentsWorkaround, boolean complete, int segmentOffset) {
      this.results = results;
      this.completedSegments = completedSegmentsWorkaround == null ? null : IntSets.from(completedSegmentsWorkaround);
      this.completedSegmentsWorkaround = completedSegmentsWorkaround;
      this.lostSegments = lostSegmentsWorkaround == null ? null : IntSets.from(lostSegmentsWorkaround);
      this.lostSegmentsWorkaround = lostSegmentsWorkaround;
      this.complete = complete;
      this.segmentOffset = segmentOffset;
      this.size = results.get().size();
   }

   public PublisherResponse(Object[] results, IntSet completedSegments, IntSet lostSegments, int size, boolean complete,
                            int segmentOffset) {
      this.results = MarshallableCollection.create(results);
      this.completedSegments = completedSegments;
      this.completedSegmentsWorkaround = completedSegments;
      this.lostSegments = lostSegments;
      this.lostSegmentsWorkaround = lostSegments;
      this.size = size;
      this.complete = complete;
      this.segmentOffset = segmentOffset;
   }

   public static PublisherResponse emptyResponse(IntSet completedSegments, IntSet lostSegments) {
      return new PublisherResponse(Util.EMPTY_OBJECT_ARRAY, completedSegments, lostSegments, 0, true, 0);
   }

   public Object[] getResults() {
      return MarshallableCollection.unwrapAsArray(results, Object[]::new);
   }

   public IntSet getCompletedSegments() {
      return completedSegments;
   }

   public IntSet getLostSegments() {
      return lostSegments;
   }

   public int getSize() {
      return size;
   }

   public boolean isComplete() {
      return complete;
   }

   public void forEachSegmentValue(ObjIntConsumer consumer, int segment) {
      Object[] results = getResults();
      for (int i = segmentOffset; i < results.length; ++i) {
         consumer.accept(results[i], segment);
      }
   }

   @Override
   public String toString() {
      return "PublisherResponse{" +
            "size=" + size +
            ", completedSegments=" + completedSegments +
            ", lostSegments=" + lostSegments +
            ", complete=" + complete +
            ", segmentOffset=" + segmentOffset +
            '}';
   }
}
