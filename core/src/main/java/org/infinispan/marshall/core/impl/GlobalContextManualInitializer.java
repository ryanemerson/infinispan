package org.infinispan.marshall.core.impl;

import java.io.UncheckedIOException;
import java.util.DoubleSummaryStatistics;
import java.util.IntSummaryStatistics;
import java.util.LongSummaryStatistics;

import org.infinispan.marshall.protostream.impl.marshallers.DoubleSummaryStatisticsMarshaller;
import org.infinispan.marshall.protostream.impl.marshallers.IntSummaryStatisticsMarshaller;
import org.infinispan.marshall.protostream.impl.marshallers.LongSummaryStatisticsMarshaller;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;

public class GlobalContextManualInitializer implements SerializationContextInitializer {

   public static final SerializationContextInitializer INSTANCE = new GlobalContextManualInitializer();

   private static String type(String message) {
      return String.format("org.infinispan.global.m.core.%s", message);
   }

   private GlobalContextManualInitializer() {}

   @Override
   public String getProtoFileName() {
      return String.format("global.m.core.proto");
   }

   @Override
   public String getProtoFile() throws UncheckedIOException {
      return FileDescriptorSource.getResourceAsString(getClass(), "/proto/" + getProtoFileName());
   }

   @Override
   public void registerSchema(SerializationContext serCtx) {
      serCtx.registerProtoFiles(org.infinispan.protostream.FileDescriptorSource.fromString(getProtoFileName(), getProtoFile()));
   }

   @Override
   public void registerMarshallers(SerializationContext serCtx) {
      serCtx.registerMarshaller(new DoubleSummaryStatisticsMarshaller(type(DoubleSummaryStatistics.class.getSimpleName())));
      serCtx.registerMarshaller(new IntSummaryStatisticsMarshaller(type(IntSummaryStatistics.class.getSimpleName())));
      serCtx.registerMarshaller(new LongSummaryStatisticsMarshaller(type(LongSummaryStatistics.class.getSimpleName())));
   }
}
