package org.infinispan.reactive.publisher.impl.commands.reduction;

import java.util.HashSet;
import java.util.Set;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.commons.util.IntSet;
import org.infinispan.commons.util.IntSets;
import org.infinispan.marshall.protostream.impl.MarshallableObject;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * A PublisherResult that was performed due to segments only
 * @author wburns
 * @since 10.0
 */
@ProtoTypeId(ProtoStreamTypeIds.SEGMENT_PUBLISHER_RESULT)
public class SegmentPublisherResult<R> implements PublisherResult<R> {

//      @ProtoField(number = 1)
   final IntSet suspectedSegments;

   // TODO remove once IPROTO-131 issue fixed
   @ProtoField(number = 1, collectionImplementation = HashSet.class)
   final Set<Integer> suspectedSegmentsWorkaround;

   @ProtoField(number = 2)
   final MarshallableObject<R> result;

   @ProtoFactory
   SegmentPublisherResult(Set<Integer> suspectedSegmentsWorkaround, MarshallableObject<R> result) {
      this.suspectedSegmentsWorkaround = suspectedSegmentsWorkaround;
      this.suspectedSegments = IntSets.from(suspectedSegmentsWorkaround);
      this.result = result;
   }

   public SegmentPublisherResult(IntSet suspectedSegments, R result) {
      this.suspectedSegments = suspectedSegments;
      this.suspectedSegmentsWorkaround = suspectedSegments == null ? null : IntSets.from(suspectedSegments);
      this.result = MarshallableObject.create(result);
   }

   @Override
   public IntSet getSuspectedSegments() {
      return suspectedSegments;
   }

   @Override
   public Set<?> getSuspectedKeys() {
      return null;
   }

   @Override
   public R getResult() {
      return MarshallableObject.unwrap(result);
   }

   @Override
   public String toString() {
      return "SegmentPublisherResult{" +
            "result=" + getResult() +
            ", suspectedSegments=" + suspectedSegments +
            '}';
   }
}
