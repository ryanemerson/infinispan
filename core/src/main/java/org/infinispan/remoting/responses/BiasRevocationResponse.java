package org.infinispan.remoting.responses;

import java.util.Collection;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.marshall.protostream.impl.MarshallableArray;
import org.infinispan.marshall.protostream.impl.MarshallableCollection;
import org.infinispan.marshall.protostream.impl.MarshallableMap;
import org.infinispan.marshall.protostream.impl.MarshallableObject;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.remoting.transport.Address;

@ProtoTypeId(ProtoStreamTypeIds.BIAS_REVOCATION_RESPONSE)
public class BiasRevocationResponse extends SuccessfulResponse {

   public BiasRevocationResponse(Object responseValue, Collection<Address> waitFor) {
      super(MarshallableObject.create(responseValue), MarshallableCollection.create(waitFor), null, null);
   }

   @ProtoFactory
   BiasRevocationResponse(MarshallableObject<?> object, MarshallableCollection<?> collection,
                          MarshallableMap<?, ?> map, MarshallableArray<?> array) {
      super(object, collection, null, null);
   }

   @SuppressWarnings("unchecked")
   public Collection<Address> getWaitList() {
      return (Collection<Address>) MarshallableCollection.unwrap(collection);
   }

   @Override
   public Object getResponseValue() {
      return MarshallableObject.unwrap(object);
   }
}
