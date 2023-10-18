package org.infinispan.marshall.protostream.impl.marshallers;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.IntSummaryStatistics;

public class IntSummaryStatisticsMarshaller extends AbstractMessageMarshaller<IntSummaryStatistics> {

   private final static String CONSTRUCTOR_CALL_ERROR_MSG =
         "Unable to create instance of %s via [%s] with parameters (%s, %s, %s, %s)";

   static final Field countField;
   static final Field sumField;
   static final Field minField;
   static final Field maxField;

   static final boolean canSerialize;
   static final Constructor<IntSummaryStatistics> constructor;

   static {
      constructor = SecurityActions.getConstructor(IntSummaryStatistics.class, long.class, int.class, int.class, long.class);

      if (constructor != null) {
         // since JDK 10, *SummaryStatistics have parametrized constructors, so reflection can be avoided

         countField = null;
         sumField = null;
         minField = null;
         maxField = null;

         canSerialize = true;
      } else {
         countField = SecurityActions.getField(IntSummaryStatistics.class, "count");
         sumField = SecurityActions.getField(IntSummaryStatistics.class, "sum");
         minField = SecurityActions.getField(IntSummaryStatistics.class, "min");
         maxField = SecurityActions.getField(IntSummaryStatistics.class, "max");

         // We can only properly serialize if all of the fields are non null
         canSerialize = countField != null && sumField != null && minField != null && maxField != null;
      }
   }

   public IntSummaryStatisticsMarshaller(String typeName) {
      super(typeName, IntSummaryStatistics.class);
   }

   @Override
   public IntSummaryStatistics readFrom(ProtoStreamReader reader) throws IOException {
      final IntSummaryStatistics summaryStatistics;

      final long count = reader.readLong("count");
      final long sum = reader.readLong("sum");
      final int min = reader.readInt("min");
      final int max = reader.readInt("max");

      if (constructor != null) {
         // JDK 10+, pass values to constructor
         try {
            summaryStatistics = constructor.newInstance(count, min, max, sum);
         } catch (ReflectiveOperationException e) {
            throw new IOException(String.format(CONSTRUCTOR_CALL_ERROR_MSG,
                  IntSummaryStatistics.class, constructor.toString(), count, min, max, sum), e);
         }
      } else {
         // JDK 9 or older, fall back to reflection
         summaryStatistics = new IntSummaryStatistics();
         try {
            countField.setLong(summaryStatistics, count);
            sumField.setLong(summaryStatistics, sum);
            minField.setInt(summaryStatistics, min);
            maxField.setInt(summaryStatistics, max);
         } catch (IllegalAccessException e) {
            // This can't happen as we force accessibility in the getField
            throw new IOException(e);
         }
      }
      return summaryStatistics;
   }

   @Override
   public void writeTo(ProtoStreamWriter writer, IntSummaryStatistics stats) throws IOException {
      writer.writeLong("count", stats.getCount());
      writer.writeLong("sum", stats.getSum());
      writer.writeInt("min", stats.getMin());
      writer.writeInt("max", stats.getMax());
   }
}
