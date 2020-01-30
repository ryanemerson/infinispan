package org.infinispan.marshall.protostream.impl.marshallers;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.LongSummaryStatistics;

public class LongSummaryStatisticsMarshaller extends AbstractMessageMarshaller<LongSummaryStatistics> {

   private final static String CONSTRUCTOR_CALL_ERROR_MSG =
         "Unable to create instance of %s via [%s] with parameters (%s, %s, %s, %s)";

   static final Field countField;
   static final Field sumField;
   static final Field minField;
   static final Field maxField;

   static final boolean canSerialize;
   static final Constructor<LongSummaryStatistics> constructor;

   static {
      constructor = SecurityActions.getConstructor(LongSummaryStatistics.class, long.class, long.class, long.class, long.class);

      if (constructor != null) {
         // since JDK 10, *SummaryStatistics have parametrized constructors, so reflection can be avoided

         countField = null;
         sumField = null;
         minField = null;
         maxField = null;

         canSerialize = true;
      } else {
         countField = SecurityActions.getField(LongSummaryStatistics.class, "count");
         sumField = SecurityActions.getField(LongSummaryStatistics.class, "sum");
         minField = SecurityActions.getField(LongSummaryStatistics.class, "min");
         maxField = SecurityActions.getField(LongSummaryStatistics.class, "max");

         // We can only properly serialize if all of the fields are non null
         canSerialize = countField != null && sumField != null && minField != null && maxField != null;
      }
   }

   public LongSummaryStatisticsMarshaller(String typeName) {
      super(typeName, LongSummaryStatistics.class);
   }

   @Override
   public LongSummaryStatistics readFrom(ProtoStreamReader reader) throws IOException {
      final LongSummaryStatistics summaryStatistics;

      final long count = reader.readLong("count");
      final long sum = reader.readLong("sum");
      final long min = reader.readLong("min");
      final long max = reader.readLong("max");

      if (constructor != null) {
         // JDK 10+, pass values to constructor
         try {
            summaryStatistics = constructor.newInstance(count, min, max, sum);
         } catch (ReflectiveOperationException e) {
            throw new IOException(String.format(CONSTRUCTOR_CALL_ERROR_MSG,
                  LongSummaryStatistics.class, constructor.toString(), count, min, max, sum), e);
         }
      } else {
         // JDK 9 or older, fall back to reflection
         summaryStatistics = new LongSummaryStatistics();
         try {
            countField.setLong(summaryStatistics, count);
            sumField.setLong(summaryStatistics, sum);
            minField.setLong(summaryStatistics, min);
            maxField.setLong(summaryStatistics, max);
         } catch (IllegalAccessException e) {
            // This can't happen as we force accessibility in the getField
            throw new IOException(e);
         }
      }
      return summaryStatistics;
   }

   @Override
   public void writeTo(ProtoStreamWriter writer, LongSummaryStatistics stats) throws IOException {
      writer.writeLong("count", stats.getCount());
      writer.writeLong("sum", stats.getSum());
      writer.writeLong("min", stats.getMin());
      writer.writeLong("max", stats.getMax());
   }
}
