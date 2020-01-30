package org.infinispan.stream.impl.intops.object;

import java.util.function.Consumer;
import java.util.stream.Stream;

import org.infinispan.factories.ComponentRegistry;
import org.infinispan.marshall.protostream.impl.MarshallableObject;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.stream.CacheAware;
import org.infinispan.stream.impl.intops.IntermediateOperation;

import io.reactivex.Flowable;

/**
 * Performs peek operation on a regular {@link Stream}
 */
public class PeekOperation<S> implements IntermediateOperation<S, Stream<S>, S, Stream<S>> {
   private final Consumer<? super S> consumer;

   public PeekOperation(Consumer<? super S> consumer) {
      this.consumer = consumer;
   }

   @ProtoFactory
   PeekOperation(MarshallableObject<Consumer<? super S>> consumer) {
      this.consumer = MarshallableObject.unwrap(consumer);
   }

   @ProtoField(number = 1)
   MarshallableObject<Consumer<? super S>> getConsumer() {
      return MarshallableObject.create(consumer);
   }

   @Override
   public Stream<S> perform(Stream<S> stream) {
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
   public Flowable<S> mapFlowable(Flowable<S> input) {
      return input.doOnNext(consumer::accept);
   }
}
