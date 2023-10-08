package org.infinispan.marshall.protostream.impl;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.infinispan.protostream.annotations.ProtoEnumValue;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

// TODO add ID
public class MarshallableCollector {

   enum Characteristics {
      @ProtoEnumValue(0)
      CONCURRENT,

      @ProtoEnumValue(1)
      UNORDERED,

      @ProtoEnumValue(2)
      IDENTITY_FINISH
   }

   @ProtoField(1)
   MarshallableLambda supplier;

   @ProtoField(2)
   MarshallableLambda accumulator;

   @ProtoField(3)
   MarshallableLambda combiner;

   @ProtoField(4)
   MarshallableLambda finisher;

   @ProtoField(value = 5, collectionImplementation = HashSet.class)
   Set<Characteristics> characteristics;

   @ProtoFactory
   MarshallableCollector(MarshallableLambda supplier, MarshallableLambda accumulator, MarshallableLambda combiner,
                                MarshallableLambda finisher, HashSet<Characteristics> characteristics) {
      this.supplier = supplier;
      this.accumulator = accumulator;
      this.combiner = combiner;
      this.finisher = finisher;
      this.characteristics = characteristics;
   }

   public static <T, A, R> MarshallableCollector create(Collector<T, A, R> o) {
      return new MarshallableCollector(
            MarshallableLambda.create(o.supplier()),
            MarshallableLambda.create(o.accumulator()),
            MarshallableLambda.create(o.combiner()),
            MarshallableLambda.create(o.finisher()),
            (HashSet<Characteristics>) o.characteristics().stream().map(c -> Characteristics.valueOf(c.name())).collect(Collectors.toSet())
      );
   }
}
