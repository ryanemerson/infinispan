package org.infinispan.remoting.responses;

import java.util.Objects;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.commons.util.Util;
import org.infinispan.marshall.protostream.impl.MarshallableObject;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * An unsuccessful response
 *
 * @author Manik Surtani
 * @since 4.0
 */
@ProtoTypeId(ProtoStreamTypeIds.UNSUCCESSFUL_RESPONSE)
public class UnsuccessfulResponse extends ValidResponse {
   public static final UnsuccessfulResponse EMPTY = new UnsuccessfulResponse(null);

   @ProtoField(number = 1)
   final MarshallableObject<?> responseValue;

   @ProtoFactory
   UnsuccessfulResponse(MarshallableObject<?> responseValue) {
      this.responseValue = responseValue;
   }

   public static UnsuccessfulResponse create(Object value) {
      return value == null ? EMPTY : new UnsuccessfulResponse(new MarshallableObject<>(value));
   }

   @Override
   public boolean isSuccessful() {
      return false;
   }

   public Object getResponseValue() {
      return MarshallableObject.unwrap(responseValue);
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      UnsuccessfulResponse that = (UnsuccessfulResponse) o;
      return Objects.equals(responseValue, that.responseValue);
   }

   @Override
   public int hashCode() {
      return Objects.hash(responseValue);
   }

   @Override
   public String toString() {
      return "UnsuccessfulResponse{responseValue=" + Util.toStr(getResponseValue()) + "} ";
   }
}
