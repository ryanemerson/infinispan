package org.infinispan.transaction.xa.recovery;

import javax.transaction.xa.Xid;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.commons.tx.XidImpl;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.remoting.transport.Address;
import org.infinispan.remoting.transport.jgroups.JGroupsAddress;
import org.infinispan.transaction.xa.GlobalTransaction;

/**
 * GlobalTransaction that also holds xid information, required for recovery.
 *
 * @author Mircea.Markus@jboss.com
 * @since 5.0
 */
@ProtoTypeId(ProtoStreamTypeIds.RECOVERY_AWARE_GLOBAL_TRANSACTION)
public class RecoveryAwareGlobalTransaction extends GlobalTransaction implements RecoverableTransactionIdentifier {

   private volatile Xid xid;

   private volatile long internalId;

   public RecoveryAwareGlobalTransaction() {
      super();
   }

   public RecoveryAwareGlobalTransaction(Address addr, boolean remote) {
      super(addr, remote);
   }

   @ProtoFactory
   RecoveryAwareGlobalTransaction(long id, JGroupsAddress address, XidImpl xid, long internalId) {
      super(id, address);
      this.xid = xid;
      this.internalId = internalId;
   }

   @Override
   @ProtoField(number = 3, javaType = XidImpl.class)
   public Xid getXid() {
      return xid;
   }

   @Override
   public void setXid(Xid xid) {
      this.xid = xid;
   }

   @Override
   @ProtoField(number = 4, defaultValue = "-1")
   public long getInternalId() {
      return internalId;
   }

   @Override
   public void setInternalId(long internalId) {
      this.internalId = internalId;
   }

   @Override
   public String toString() {
      return getClass().getSimpleName() +
            "{xid=" + xid +
            ", internalId=" + internalId +
            "} " + super.toString();
   }
}
