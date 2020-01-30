package org.infinispan.remoting.responses;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.remoting.transport.Address;

@ProtoTypeId(ProtoStreamTypeIds.BIAS_REVOCATION_RESPONSE)
public class BiasRevocationResponse extends SuccessfulResponse {
   // TODO marshall
   private final Address[] waitFor;

   public BiasRevocationResponse(Object responseValue, Address[] waitFor) {
      super(responseValue);
      this.waitFor = waitFor;
   }

   public Address[] getWaitList() {
      return waitFor;
   }
}
