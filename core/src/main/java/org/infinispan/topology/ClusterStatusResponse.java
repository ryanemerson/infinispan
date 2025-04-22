package org.infinispan.topology;

import java.util.Map;
import java.util.stream.Collectors;

import org.infinispan.protostream.WrappedMessage;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.remoting.transport.Address;
import org.infinispan.upgrade.ManagerVersion;

// TODO add ProtoTypeId
public class ClusterStatusResponse {
   final boolean rebalancingEnabled;
   final Map<Address, ManagerVersion> nodeVersions;

   public ClusterStatusResponse(boolean rebalancingEnabled, Map<Address, ManagerVersion> nodeVersions) {
      this.rebalancingEnabled = rebalancingEnabled;
      this.nodeVersions = nodeVersions;
   }

   ClusterStatusResponse(Map<WrappedMessage, ManagerVersion> wrappedNodeVersions, boolean rebalancingEnabled) {
      this.nodeVersions = wrappedNodeVersions.entrySet().stream().collect(Collectors.toMap(e -> (Address) e.getKey().getValue(), Map.Entry::getValue));
      this.rebalancingEnabled = rebalancingEnabled;
   }

   @ProtoField(1)
   public boolean isRebalancingEnabled() {
      return rebalancingEnabled;
   }

   @ProtoField(2)
   Map<WrappedMessage, ManagerVersion> getWrappedNodeVersions() {
      return nodeVersions.entrySet().stream().collect(Collectors.toMap(WrappedMessage::new, Map.Entry::getValue));
   }
}
