package org.infinispan.remoting.responses;

import java.util.Objects;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.container.entries.ImmortalCacheValue;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * @author Ryan Emerson
 * @since 16.0
 */
@ProtoTypeId(ProtoStreamTypeIds.SUCCESSFUL_IMMORTAL_CACHE_VALUE_RESPONSE)
public class SuccessfulImmortalCacheValueResponse implements SuccessfulResponse<ImmortalCacheValue> {
   @ProtoField(1)
   final ImmortalCacheValue icv;

   @ProtoFactory
   SuccessfulImmortalCacheValueResponse(ImmortalCacheValue icv) {
      this.icv = icv;
   }

   public ImmortalCacheValue getResponseValue() {
      return icv;
   }

   @Override
   public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) return false;
      SuccessfulImmortalCacheValueResponse that = (SuccessfulImmortalCacheValueResponse) o;
      return Objects.equals(icv, that.icv);
   }

   @Override
   public int hashCode() {
      return Objects.hashCode(icv);
   }

   @Override
   public String toString() {
      return "SuccessfulImmortalCacheValueResponse{" +
            "icv=" + icv +
            '}';
   }
}
