package org.infinispan.topology;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.protostream.annotations.ProtoEnumValue;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * RebalancingStatus.
 *
 * @author Tristan Tarrant
 * @since 8.1
 */
@ProtoTypeId(ProtoStreamTypeIds.REBALANCE_STATUS)
public enum RebalancingStatus {
   @ProtoEnumValue(number = 1)
   SUSPENDED,
   @ProtoEnumValue(number = 2)
   PENDING,
   @ProtoEnumValue(number = 3)
   IN_PROGRESS,
   @ProtoEnumValue(number = 4)
   COMPLETE
}
