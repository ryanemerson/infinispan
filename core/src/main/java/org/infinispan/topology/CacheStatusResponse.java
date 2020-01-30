package org.infinispan.topology;

import java.io.Serializable;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.partitionhandling.AvailabilityMode;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
* @author Dan Berindei
* @since 7.0
*/
@ProtoTypeId(ProtoStreamTypeIds.CACHE_STATUS_RESPONSE)
public class CacheStatusResponse implements Serializable {

   @ProtoField(number = 1)
   final CacheJoinInfo cacheJoinInfo;

   @ProtoField(number = 2)
   final CacheTopology cacheTopology;

   @ProtoField(number = 3)
   final CacheTopology stableTopology;

   @ProtoField(number = 4)
   final AvailabilityMode availabilityMode;

   @ProtoFactory
   public CacheStatusResponse(CacheJoinInfo cacheJoinInfo, CacheTopology cacheTopology, CacheTopology stableTopology,
         AvailabilityMode availabilityMode) {
      this.cacheJoinInfo = cacheJoinInfo;
      this.cacheTopology = cacheTopology;
      this.stableTopology = stableTopology;
      this.availabilityMode = availabilityMode;
   }

   public CacheJoinInfo getCacheJoinInfo() {
      return cacheJoinInfo;
   }

   public CacheTopology getCacheTopology() {
      return cacheTopology;
   }

   /**
    * @see org.infinispan.partitionhandling.impl.AvailabilityStrategyContext#getStableTopology()
    */
   public CacheTopology getStableTopology() {
      return stableTopology;
   }

   public AvailabilityMode getAvailabilityMode() {
      return availabilityMode;
   }

   @Override
   public String toString() {
      return "StatusResponse{" +
            "cacheJoinInfo=" + cacheJoinInfo +
            ", cacheTopology=" + cacheTopology +
            ", stableTopology=" + stableTopology +
            '}';
   }
}
