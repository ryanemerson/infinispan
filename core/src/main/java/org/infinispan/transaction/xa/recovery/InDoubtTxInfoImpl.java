package org.infinispan.transaction.xa.recovery;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.transaction.xa.Xid;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.commons.tx.XidImpl;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.remoting.transport.Address;

/**
* @author Mircea Markus
* @since 5.0
*/
@ProtoTypeId(ProtoStreamTypeIds.IN_DOUBT_TX_INFO)
public class InDoubtTxInfoImpl implements RecoveryManager.InDoubtTxInfo {
   private Xid xid;
   private long internalId;
   private int status;
   private transient Set<Address> owners = new HashSet<>();
   private transient boolean isLocal;

   public InDoubtTxInfoImpl(Xid xid, long internalId, int status) {
      this.xid = xid;
      this.internalId = internalId;
      this.status = status;
   }

   public InDoubtTxInfoImpl(Xid xid, long internalId) {
      this(xid, internalId, -1);
   }

   @ProtoFactory
   InDoubtTxInfoImpl(XidImpl xid, long internalId, int status) {
      this((Xid) xid, internalId, status);
   }

   @Override
   @ProtoField(number = 1, javaType = XidImpl.class)
   public Xid getXid() {
      return xid;
   }

   @Override
   @ProtoField(number = 2, defaultValue = "-1")
   public long getInternalId() {
      return internalId;
   }

   @Override
   @ProtoField(number = 3, defaultValue = "-1")
   public int getStatus() {
      return status;
   }

   @Override
   public Set<Address> getOwners() {
      return owners;
   }

   public void addStatus(int status) {
      this.status = status;
   }

   public void addOwner(Address owner) {
      owners.add(owner);
   }

   @Override
   public boolean isLocal() {
      return isLocal;
   }

   public void setLocal(boolean local) {
      isLocal = local;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      InDoubtTxInfoImpl that = (InDoubtTxInfoImpl) o;
      return internalId == that.internalId &&
            status == that.status &&
            isLocal == that.isLocal &&
            Objects.equals(xid, that.xid) &&
            Objects.equals(owners, that.owners);
   }

   @Override
   public int hashCode() {
      return Objects.hash(xid, internalId, status, owners, isLocal);
   }

   @Override
   public String toString() {
      return getClass().getSimpleName() +
            "{xid=" + xid +
            ", internalId=" + internalId +
            ", status=" + status +
            ", owners=" + owners +
            ", isLocal=" + isLocal +
            '}';
   }
}
