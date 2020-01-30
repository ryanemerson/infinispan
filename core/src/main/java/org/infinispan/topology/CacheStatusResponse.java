package org.infinispan.topology;

import java.io.Serializable;
import java.util.List;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.marshall.protostream.impl.MarshallableCollection;
import org.infinispan.partitionhandling.AvailabilityMode;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.remoting.transport.Address;

/**
* @author Dan Berindei
* @since 7.0
*/
@ProtoTypeId(ProtoStreamTypeIds.CACHE_STATUS_RESPONSE)
public class CacheStatusResponse implements Serializable {

   @ProtoField(1)
   final CacheJoinInfo cacheJoinInfo;

   @ProtoField(2)
   final CacheTopology cacheTopology;

   @ProtoField(3)
   final CacheTopology stableTopology;

   @ProtoField(4)
   final AvailabilityMode availabilityMode;

   final List<Address> current;

   public CacheStatusResponse(CacheJoinInfo cacheJoinInfo, CacheTopology cacheTopology, CacheTopology stableTopology,
                              AvailabilityMode availabilityMode, List<Address> current) {
      this.cacheJoinInfo = cacheJoinInfo;
      this.cacheTopology = cacheTopology;
      this.stableTopology = stableTopology;
      this.availabilityMode = availabilityMode;
      this.current = current;
   }

   @ProtoFactory
   public CacheStatusResponse(CacheJoinInfo cacheJoinInfo, CacheTopology cacheTopology, CacheTopology stableTopology,
                              AvailabilityMode availabilityMode, MarshallableCollection<Address> current) {
      this(cacheJoinInfo, cacheTopology, stableTopology, availabilityMode, MarshallableCollection.unwrapAsList(current));
   }

   @ProtoField(5)
   MarshallableCollection<Address> getCurrent() {
      return MarshallableCollection.create(current);
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

   public List<Address> joinedMembers() {
      return current;
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
