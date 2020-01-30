package org.infinispan.transaction.xa;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import javax.transaction.xa.Xid;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.commons.tx.XidImpl;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.remoting.transport.Address;
import org.infinispan.remoting.transport.jgroups.JGroupsAddress;


/**
 * Uniquely identifies a transaction that spans all JVMs in a cluster. This is used when replicating all modifications
 * in a transaction; the PREPARE and COMMIT (or ROLLBACK) messages have to have a unique identifier to associate the
 * changes with<br>. GlobalTransaction should be instantiated thorough {@link TransactionFactory} class,
 * as their type depends on the runtime configuration.
 *
 * @author <a href="mailto:bela@jboss.org">Bela Ban</a> Apr 12, 2003
 * @author <a href="mailto:manik@jboss.org">Manik Surtani (manik@jboss.org)</a>
 * @author Mircea.Markus@jboss.com
 * @since 4.0
 */
@ProtoTypeId(ProtoStreamTypeIds.GLOBAL_TRANSACTION)
public class GlobalTransaction implements Cloneable {

   private static final AtomicLong sid = new AtomicLong(0);

   private long id;
   private Address addr;
   private int hash_code = -1;  // in the worst case, hashCode() returns 0, then increases, so we're safe here
   private boolean remote = false;
   private volatile Xid xid = null;
   private volatile Long internalId;

   public GlobalTransaction(Address addr, boolean remote) {
      this.id = sid.incrementAndGet();
      this.addr = addr;
      this.remote = remote;
   }

   @ProtoFactory
   GlobalTransaction(long id, JGroupsAddress address, XidImpl xid, Long internalId) {
      this.id = id;
      this.addr = address;
      this.xid = xid;
      this.internalId = internalId;
   }

   @ProtoField(number = 1, javaType = JGroupsAddress.class)
   public Address getAddress() {
      return addr;
   }

   @ProtoField(number = 2, defaultValue = "-1")
   public long getId() {
      return id;
   }

   @ProtoField(number = 3, javaType = XidImpl.class)
   public Xid getXid() {
      return xid;
   }

   @ProtoField(number = 4)
   public Long getInternalId() {
      return internalId;
   }

   public boolean isRemote() {
      return remote;
   }

   public void setRemote(boolean remote) {
      this.remote = remote;
   }

   @Override
   public int hashCode() {
      if (hash_code == -1) {
         hash_code = (addr != null ? addr.hashCode() : 0) + (int) id;
      }
      return hash_code;
   }

   @Override
   public boolean equals(Object other) {
      if (this == other)
         return true;
      if (!(other instanceof GlobalTransaction))
         return false;

      GlobalTransaction otherGtx = (GlobalTransaction) other;
      boolean aeq = (addr == null) ? (otherGtx.addr == null) : addr.equals(otherGtx.addr);
      return aeq && (id == otherGtx.id);
   }

   /**
    * Returns a simplified representation of the transaction.
    */
   public final String globalId() {
      return getAddress() + ":" + getId();
   }

   public void setId(long id) {
      this.id = id;
   }

   public void setAddress(Address address) {
      this.addr = address;
   }

   public void setXid(Xid xid) {
      this.xid = XidImpl.copy(xid);
   }

   public void setInternalId(Long internalId) {
      this.internalId = internalId;
   }

   @Override
   public Object clone() {
      try {
         return super.clone();
      } catch (CloneNotSupportedException e) {
         throw new IllegalStateException("Impossible!");
      }
   }

   @Override
   public String toString() {
      return "GlobalTransaction{" +
            "id=" + id +
            ", addr=" + Objects.toString(addr, "local") +
            ", remote=" + remote +
            ", xid=" + xid +
            ", internalId=" + internalId +
            '}';
   }
}
