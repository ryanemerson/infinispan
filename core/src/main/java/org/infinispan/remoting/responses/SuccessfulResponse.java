package org.infinispan.remoting.responses;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.marshall.protostream.impl.MarshallableArray;
import org.infinispan.marshall.protostream.impl.MarshallableCollection;
import org.infinispan.marshall.protostream.impl.MarshallableMap;
import org.infinispan.marshall.protostream.impl.MarshallableObject;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * A successful response
 *
 * @author Manik Surtani
 * @since 4.0
 */
// TODO expose methods to get Marshallable wrappers so that we don't have to rely on casting
// getResponseObject, getResponseCollection, getResponseMap, getResponseArray
@ProtoTypeId(ProtoStreamTypeIds.SUCCESSFUL_RESPONSE)
public class SuccessfulResponse extends ValidResponse {
   public static final SuccessfulResponse SUCCESSFUL_EMPTY_RESPONSE = new SuccessfulResponse((Object) null);


   @ProtoFactory
   static SuccessfulResponse protoFactory(MarshallableObject<?> object, MarshallableCollection<?> collection,
                                          MarshallableMap<?, ?> map, MarshallableArray<?> array) {
      return object == null && collection == null && map == null && array == null ?
            SUCCESSFUL_EMPTY_RESPONSE :
            new SuccessfulResponse(object, collection, map, array);
   }

   public static SuccessfulResponse create(Object responseValue) {
      if (responseValue == null) return SUCCESSFUL_EMPTY_RESPONSE;
      if (responseValue instanceof Collection)
         return new SuccessfulResponse(null, MarshallableCollection.create((Collection<?>) responseValue),null, null);
      else if (responseValue.getClass().isArray())
         return new SuccessfulResponse(null, null, null, MarshallableArray.create((Object[]) responseValue));
      else if (responseValue instanceof Map)
         return new SuccessfulResponse(null, null, MarshallableMap.create((Map<?, ?>) responseValue), null);
      return new SuccessfulResponse(responseValue);
   }

   protected SuccessfulResponse(MarshallableObject<?> object, MarshallableCollection<?> collection, MarshallableMap<?, ?> map,
                              MarshallableArray<?> array) {
      super(object, collection, map, array);
   }

   protected SuccessfulResponse(Object responseValue) {
      this(MarshallableObject.create(responseValue), null, null, null);
   }

   @Override
   public boolean isSuccessful() {
      return true;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      SuccessfulResponse that = (SuccessfulResponse) o;
      return Objects.equals(object, that.object) &&
            Objects.equals(collection, that.collection) &&
            Objects.equals(map, that.map);
   }

   @Override
   public int hashCode() {
      return Objects.hash(object, collection, map);
   }

   @Override
   public String toString() {
      return "SuccessfulResponse{" +
            "object=" + object +
            ", collection=" + collection +
            ", map=" + map +
            ", array=" + array +
            '}';
   }
}
