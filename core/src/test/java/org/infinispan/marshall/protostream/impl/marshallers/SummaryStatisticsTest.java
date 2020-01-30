package org.infinispan.marshall.protostream.impl.marshallers;

import static org.testng.AssertJUnit.assertEquals;

import java.io.IOException;
import java.util.DoubleSummaryStatistics;
import java.util.IntSummaryStatistics;
import java.util.LongSummaryStatistics;

import org.infinispan.marshall.core.impl.GlobalContextManualInitializer;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.testng.annotations.Test;

@Test(groups = "unit", testName = "marshall.SummaryStatisticsTest")
public class SummaryStatisticsTest {

   private final SerializationContext ctx;

   public SummaryStatisticsTest() {
      this.ctx = ProtobufUtil.newSerializationContext();
      GlobalContextManualInitializer.INSTANCE.registerSchema(ctx);
      GlobalContextManualInitializer.INSTANCE.registerMarshallers(ctx);
   }

   private <T> T deserialize(T object) throws IOException {
      byte[] bytes = ProtobufUtil.toWrappedByteArray(ctx, object);
      return ProtobufUtil.fromWrappedByteArray(ctx, bytes);
   }

   public void testDoubleFiniteStats() throws IOException {
      DoubleSummaryStatistics stats = new DoubleSummaryStatistics();
      stats.accept(10.0/3);
      stats.accept(-0.1);

      DoubleSummaryStatistics deserialized = deserialize(stats);
      assertStatsAreEqual(stats, deserialized);
   }

   public void testDoublePositiveNegativeInfinites() throws IOException {
      // In JDK 10+, we expect the externalizer to pass inner state values through constructor, instead of via
      // reflection. The state of this statistics instance however doesn't pass constructor validations, so
      // deserialization of this instance fails in JDK 10+.

      DoubleSummaryStatistics stats = new DoubleSummaryStatistics();
      stats.accept(Double.POSITIVE_INFINITY);
      stats.accept(Double.NEGATIVE_INFINITY);

      try {
         DoubleSummaryStatistics deserialized = deserialize(stats);
         assertStatsAreEqual(stats, deserialized);
      } catch (IOException e) {
         // JDK 10+, ignore
         if (SecurityActions.getConstructor(DoubleSummaryStatistics.class,
               long.class, double.class, double.class, double.class) == null) {
            throw e;
         }
      }
   }

   public void testDoubleNaN() throws IOException {
      DoubleSummaryStatistics stats = new DoubleSummaryStatistics();
      stats.accept(-1);
      stats.accept(Double.NaN);

      DoubleSummaryStatistics deserialized = deserialize(stats);
      assertStatsAreEqual(stats, deserialized);
   }

   public void testDoubleInfinity() throws Exception {
      DoubleSummaryStatistics stats = new DoubleSummaryStatistics();
      stats.accept(Double.POSITIVE_INFINITY);
      stats.accept(-1);

      DoubleSummaryStatistics deserialized = deserialize(stats);
      assertStatsAreEqual(stats, deserialized);
   }

   public void testIntStatsAreMarshallable() throws IOException {
      IntSummaryStatistics original = new IntSummaryStatistics();
      original.accept(1);
      original.accept(-Integer.MAX_VALUE);

      IntSummaryStatistics deserialized = deserialize(original);
      assertEquals(original.getCount(), deserialized.getCount());
      assertEquals(original.getMin(), deserialized.getMin());
      assertEquals(original.getMax(), deserialized.getMax());
      assertEquals(original.getSum(), deserialized.getSum());
      assertEquals(original.getAverage(), deserialized.getAverage());
   }

   public void testLongStatsAreMarshallable() throws IOException {
      LongSummaryStatistics original = new LongSummaryStatistics();
      original.accept(1);
      original.accept(-Long.MAX_VALUE);

      LongSummaryStatistics deserialized = deserialize(original);
      assertEquals(original.getCount(), deserialized.getCount());
      assertEquals(original.getMin(), deserialized.getMin());
      assertEquals(original.getMax(), deserialized.getMax());
      assertEquals(original.getSum(), deserialized.getSum());
      assertEquals(original.getAverage(), deserialized.getAverage());
   }

   private void assertStatsAreEqual(DoubleSummaryStatistics original, DoubleSummaryStatistics deserialized) {
      assertEquals(original.getCount(), deserialized.getCount());
      assertEquals(original.getMin(), deserialized.getMin());
      assertEquals(original.getMax(), deserialized.getMax());
      assertEquals(original.getSum(), deserialized.getSum());
      assertEquals(original.getAverage(), deserialized.getAverage());
   }
}
