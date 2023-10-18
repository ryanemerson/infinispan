package org.infinispan.marshall.protostream.impl.marshallers;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.DoubleSummaryStatistics;

public class DoubleSummaryStatisticsMarshaller extends AbstractMessageMarshaller<DoubleSummaryStatistics> {

   private final static String CONSTRUCTOR_CALL_ERROR_MSG =
         "Unable to create instance of %s via [%s] with parameters (%s, %s, %s, %s)";

   static final Field countField;
   static final Field sumField;
   static final Field minField;
   static final Field maxField;

   static final boolean canSerialize;
   static final Constructor<DoubleSummaryStatistics> constructor;

   static {
      constructor = SecurityActions.getConstructor(DoubleSummaryStatistics.class, long.class, double.class, double.class, double.class);

      if (constructor != null) {
         // since JDK 10, *SummaryStatistics have parametrized constructors, so reflection can be avoided

         countField = null;
         sumField = null;
         minField = null;
         maxField = null;

         canSerialize = true;
      } else {
         countField = SecurityActions.getField(DoubleSummaryStatistics.class, "count");
         sumField = SecurityActions.getField(DoubleSummaryStatistics.class, "sum");
         minField = SecurityActions.getField(DoubleSummaryStatistics.class, "min");
         maxField = SecurityActions.getField(DoubleSummaryStatistics.class, "max");

         // We can only properly serialize if all of the fields are non null
         canSerialize = countField != null && sumField != null && minField != null && maxField != null;
      }
   }

   public DoubleSummaryStatisticsMarshaller(String typeName) {
      super(typeName, DoubleSummaryStatistics.class);
   }

   @Override
   public DoubleSummaryStatistics readFrom(ProtoStreamReader reader) throws IOException {
      final DoubleSummaryStatistics summaryStatistics;

      final long count = reader.readLong("count");
      final double sum = reader.readDouble("sum");
      final double min = reader.readDouble("min");
      final double max = reader.readDouble("max");

      if (constructor != null) {
         // JDK 10+, pass values to constructor
         try {
            summaryStatistics = constructor.newInstance(count, min, max, sum);
         } catch (ReflectiveOperationException e) {
            throw new IOException(String.format(CONSTRUCTOR_CALL_ERROR_MSG,
                  DoubleSummaryStatistics.class, constructor.toString(), count, min, max, sum), e);
         }
      } else {
         // JDK 9 or older, fall back to reflection
         summaryStatistics = new DoubleSummaryStatistics();
         try {
            countField.setLong(summaryStatistics, count);
            sumField.setDouble(summaryStatistics, sum);
            minField.setDouble(summaryStatistics, min);
            maxField.setDouble(summaryStatistics, max);
         } catch (IllegalAccessException e) {
            // This can't happen as we force accessibility in the getField
            throw new IOException(e);
         }
      }
      return summaryStatistics;
   }

   @Override
   public void writeTo(ProtoStreamWriter writer, DoubleSummaryStatistics stats) throws IOException {
      writer.writeLong("count", stats.getCount());
      writer.writeDouble("sum", stats.getSum());
      writer.writeDouble("min", stats.getMin());
      writer.writeDouble("max", stats.getMax());
   }
}
