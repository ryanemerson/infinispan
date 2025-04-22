package org.infinispan.topology;

import java.util.Map;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.protostream.annotations.Proto;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.upgrade.ManagerVersion;

/**
 * @author Dan Berindei
 * @since 7.1
 */
@Proto
@ProtoTypeId(ProtoStreamTypeIds.MANAGER_STATUS_RESPONSE)
// TODO remove isRebalancingEnabled?
public record ManagerStatusResponse(Map<String, CacheStatusResponse> caches, boolean rebalancingEnabled, ManagerVersion oldestClusterMember) {
}
