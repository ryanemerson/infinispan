package org.infinispan.statetransfer;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.infinispan.commands.write.WriteCommand;
import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.marshall.protostream.impl.MarshallableCollection;
import org.infinispan.marshall.protostream.impl.MarshallableUserCollection;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.transaction.xa.GlobalTransaction;

/**
 * A representation of a transaction that is suitable for transferring between a StateProvider and a StateConsumer
 * running on different members of the same cache.
 *
 * @author anistor@redhat.com
 * @since 5.2
 */
@ProtoTypeId(ProtoStreamTypeIds.TRANSACTION_INFO)
public class TransactionInfo {

   @ProtoField(number = 1)
   final GlobalTransaction globalTransaction;

   @ProtoField(number = 2)
   final MarshallableCollection<WriteCommand> modifications;

   @ProtoField(number = 3)
   final MarshallableUserCollection<Object> lockedKeys;

   @ProtoField(number = 4, defaultValue = "-1")
   final int topologyId;

   @ProtoFactory
   TransactionInfo(GlobalTransaction globalTransaction, int topologyId,
                   MarshallableCollection<WriteCommand> modifications, MarshallableUserCollection<Object> lockedKeys) {
      this.globalTransaction = globalTransaction;
      this.modifications = modifications;
      this.lockedKeys = lockedKeys;
      this.topologyId = topologyId;
   }

   public TransactionInfo(GlobalTransaction globalTransaction, int topologyId, List<WriteCommand> modifications, Set<Object> lockedKeys) {
      this(globalTransaction, topologyId,
            modifications.isEmpty() ? null : MarshallableCollection.create(modifications),
            MarshallableUserCollection.create(lockedKeys));
   }

   public GlobalTransaction getGlobalTransaction() {
      return globalTransaction;
   }

   public WriteCommand[] getModifications() {
      Collection<WriteCommand> modifications = MarshallableCollection.unwrap(this.modifications);
      return modifications == null ? null : modifications.toArray(new WriteCommand[0]);
   }

   public Collection<Object> getLockedKeys() {
      return MarshallableUserCollection.unwrap(lockedKeys);
   }

   public int getTopologyId() {
      return topologyId;
   }

   @Override
   public String toString() {
      return "TransactionInfo{" +
            "globalTransaction=" + globalTransaction +
            ", topologyId=" + topologyId +
            ", modifications=" + modifications +
            ", lockedKeys=" + lockedKeys +
            '}';
   }
}
