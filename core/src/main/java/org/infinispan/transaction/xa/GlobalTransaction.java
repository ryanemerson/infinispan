package org.infinispan.transaction.xa;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
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

   protected long id = -1;
   protected Address address = null;
   private int hashCode = -1;  // in the worst case, hashCode() returns 0, then increases, so we're safe here
   private boolean remote = false;

   protected GlobalTransaction() {
   }

   protected GlobalTransaction(Address address, boolean remote) {
      this.id = sid.incrementAndGet();
      this.address = address;
      this.remote = remote;
   }

   @ProtoFactory
   protected GlobalTransaction(long id, JGroupsAddress address) {
      this.id = id;
      this.address = address;
   }

   @ProtoField(number = 1, javaType = JGroupsAddress.class)
   public Address getAddress() {
      return address;
   }

   @ProtoField(number = 2, defaultValue = "-1")
   public long getId() {
      return id;
   }

   public boolean isRemote() {
      return remote;
   }

   public void setRemote(boolean remote) {
      this.remote = remote;
   }

   @Override
   public int hashCode() {
      if (hashCode == -1) {
         hashCode = (address != null ? address.hashCode() : 0) + (int) id;
      }
      return hashCode;
   }

   @Override
   public boolean equals(Object other) {
      if (this == other)
         return true;
      if (!(other instanceof GlobalTransaction))
         return false;

      GlobalTransaction otherGtx = (GlobalTransaction) other;
      return Objects.equals(address, otherGtx.address) && (id == otherGtx.id);
   }

   @Override
   public String toString() {
      return "GlobalTx:" + Objects.toString(address, "local") + ":" + id;
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
      this.address = address;
   }

   @Override
   public Object clone() {
      try {
         return super.clone();
      } catch (CloneNotSupportedException e) {
         throw new IllegalStateException("Impossible!");
      }
   }
}
