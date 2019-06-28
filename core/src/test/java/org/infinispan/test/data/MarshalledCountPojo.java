package org.infinispan.test.data;

import java.util.Objects;

import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

// TODO rename CountMarshallingPojo
public class MarshalledCountPojo {
   private static final Log log = LogFactory.getLog(MarshalledCountPojo.class);

   private static int marshallCount, unmarshallCount;
   private int i;

   public static void reset() {
      marshallCount = 0;
      unmarshallCount = 0;
   }

   public static int getMarshallCount() {
      return marshallCount;
   }

   public static int getUnmarshallCount() {
      return unmarshallCount;
   }

   public MarshalledCountPojo() {}

   public MarshalledCountPojo(int i) {
      this.i = i;
   }

   @ProtoField(number = 1, defaultValue = "0")
   public int getI() {
      int serCount = ++marshallCount;
      log.trace("marshallCount=" + serCount);
      return i;
   }

   public void setI(int i) {
      this.i = i;
      int deserCount = ++unmarshallCount;
      log.trace("unmarshallCount=" + deserCount);
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      MarshalledCountPojo that = (MarshalledCountPojo) o;
      return i == that.i;
   }

   @Override
   public int hashCode() {
      return Objects.hash(i);
   }

   @Override
   public String toString() {
      return "MarshalledCountPojo{" +
            "i=" + i +
            '}';
   }
}
