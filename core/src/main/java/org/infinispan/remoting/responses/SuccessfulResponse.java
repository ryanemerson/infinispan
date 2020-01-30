package org.infinispan.remoting.responses;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.marshall.protostream.impl.MarshallableCollection;
import org.infinispan.marshall.protostream.impl.MarshallableObject;
import org.infinispan.marshall.protostream.impl.MarshallableUserMap;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * A successful response
 *
 * @author Manik Surtani
 * @since 4.0
 */
@ProtoTypeId(ProtoStreamTypeIds.SUCCESSFUL_RESPONSE)
public class SuccessfulResponse extends ValidResponse {
   public static final SuccessfulResponse SUCCESSFUL_EMPTY_RESPONSE = new SuccessfulResponse((Object) null);

   @ProtoField(number = 1)
   final MarshallableObject<?> object;

   @ProtoField(number = 2)
   final MarshallableCollection<?> collection;

   // TODO do we need MarshallableMap for this to handle internal types?
   @ProtoField(number = 3)
   final MarshallableUserMap<?, ?> map;

   @ProtoFactory
   static SuccessfulResponse protoFactory(MarshallableObject<?> object, MarshallableCollection<?> collection,
                                          MarshallableUserMap<?, ?> map) {
      return object == null && collection == null && map == null? SUCCESSFUL_EMPTY_RESPONSE : new SuccessfulResponse(object, collection, map);
   }

   public static SuccessfulResponse create(Object responseValue) {
      if (responseValue == null) return SUCCESSFUL_EMPTY_RESPONSE;
      if (responseValue instanceof Collection)
         return new SuccessfulResponse((Collection<?>) responseValue);
      else if (responseValue instanceof Map)
         return new SuccessfulResponse((Map<?, ?>) responseValue);
      return new SuccessfulResponse(responseValue);
   }

   private SuccessfulResponse(MarshallableObject<?> object, MarshallableCollection<?> collection, MarshallableUserMap<?, ?> map) {
      this.object = object;
      this.collection = collection;
      this.map = map;
   }

   protected SuccessfulResponse(Object responseValue) {
      this(MarshallableObject.create(responseValue), null, null);
   }

   private SuccessfulResponse(Collection<?> collection) {
      this(null, MarshallableCollection.create(collection),null);
   }

   private SuccessfulResponse(Map<?, ?> map) {
      this(null, null, MarshallableUserMap.create(map));
   }

   @Override
   public boolean isSuccessful() {
      return true;
   }

   public Object getResponseValue() {
      if (object != null)
         return object.get();

      if (collection != null)
         return collection.get();

      return map == null ? null: map.get();
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
            '}';
   }
}
