package org.infinispan.stream.impl.intops.primitive.l;

import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.infinispan.stream.impl.intops.IntermediateOperation;

import io.reactivex.Flowable;

/**
 * Performs boxed operation on a {@link LongStream}
 */
public class BoxedLongOperation implements IntermediateOperation<Long, LongStream, Long, Stream<Long>> {
   private static final BoxedLongOperation OPERATION = new BoxedLongOperation();
   private BoxedLongOperation() { }

   public static BoxedLongOperation getInstance() {
      return OPERATION;
   }

   @Override
   public Stream<Long> perform(LongStream stream) {
      return stream.boxed();
   }

   @Override
   public Flowable<Long> mapFlowable(Flowable<Long> input) {
      return input;
   }
}
