package org.infinispan.stream.impl.intops.primitive.d;

import java.util.function.DoubleConsumer;
import java.util.stream.DoubleStream;

import org.infinispan.factories.ComponentRegistry;
import org.infinispan.marshall.protostream.impl.MarshallableObject;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.stream.CacheAware;
import org.infinispan.stream.impl.intops.IntermediateOperation;

import io.reactivex.rxjava3.core.Flowable;

/**
 * Performs peek operation on a {@link DoubleStream}
 */
public class PeekDoubleOperation implements IntermediateOperation<Double, DoubleStream, Double, DoubleStream> {
   private final DoubleConsumer consumer;

   public PeekDoubleOperation(DoubleConsumer consumer) {
      this.consumer = consumer;
   }

   @ProtoFactory
   PeekDoubleOperation(MarshallableObject<DoubleConsumer> consumer) {
      this.consumer = MarshallableObject.unwrap(consumer);
   }

   @ProtoField(number = 1)
   MarshallableObject<DoubleConsumer> getConsumer() {
      return MarshallableObject.create(consumer);
   }

   @Override
   public DoubleStream perform(DoubleStream stream) {
      return stream.peek(consumer);
   }

   @Override
   public void handleInjection(ComponentRegistry registry) {
      if (consumer instanceof CacheAware) {
         ((CacheAware) consumer).injectCache(registry.getCache().running());
      } else {
         registry.wireDependencies(consumer);
      }
   }

   @Override
   public Flowable<Double> mapFlowable(Flowable<Double> input) {
      return input.doOnNext(consumer::accept);
   }
}
