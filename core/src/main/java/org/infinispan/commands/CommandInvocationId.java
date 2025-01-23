package org.infinispan.commands;

import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.remoting.transport.Address;
import org.infinispan.remoting.transport.LocalModeAddress;
import org.infinispan.remoting.transport.jgroups.JGroupsAddress;
import org.infinispan.remoting.transport.jgroups.JGroupsAddressCache;

/**
 * Represents an unique identified for non-transaction write commands.
 * <p>
 * It is used to lock the key for a specific command.
 * <p>
 * This class is final to prevent issues as it is usually not marshalled
 * as polymorphic object but directly using {@link #writeTo(ObjectOutput, CommandInvocationId)}
 * and {@link #readFrom(ObjectInput)}.
 *
 * @author Pedro Ruivo
 * @since 8.0
 */
@ProtoTypeId(ProtoStreamTypeIds.COMMAND_INVOCATION_ID)
public final class CommandInvocationId {
   public static final CommandInvocationId DUMMY_INVOCATION_ID = new CommandInvocationId(null, 0, -1, -1);

   private static final AtomicLong nextId = new AtomicLong(0);

   private final Address address;
   private final long id;
   private final long mostSignificantBits;
   private final long leastSignificantBits;

   CommandInvocationId(Address address, long id, long mostSignificantBits, long leastSignificantBits) {
      this.address = address;
      this.id = id;
      this.mostSignificantBits = mostSignificantBits;
      this.leastSignificantBits = leastSignificantBits;
   }

   @ProtoFactory
   CommandInvocationId(long id, long addressMostSignificantBits, long addressLeastSignificantBits) {
      this.id = id;
      this.address = JGroupsAddressCache.fromUUID(addressMostSignificantBits, addressLeastSignificantBits);
      this.mostSignificantBits = addressMostSignificantBits;
      this.leastSignificantBits = addressLeastSignificantBits;
   }

   @ProtoField(number = 1, defaultValue = "-1")
   public long getId() {
      return id;
   }

   @ProtoField(number = 2, defaultValue = "-1")
   long getAddressMostSignificantBits() {
      return mostSignificantBits;
   }

   @ProtoField(number = 3, defaultValue = "-1")
   long getAddressLeastSignificantBits() {
      return leastSignificantBits;
   }

   public Address getAddress() {
      return address;
   }

   public static CommandInvocationId generateId(Address address) {
      if (address instanceof LocalModeAddress)
         return new CommandInvocationId(address, nextId.getAndIncrement(), -1, -1);

      if (!(address instanceof JGroupsAddress addr)) {
         throw new IllegalArgumentException("Address must be a JGroupsAddress");
      }
      if (!(addr.getJGroupsAddress() instanceof org.jgroups.util.UUID uuidAddr)) {
         throw new IllegalArgumentException("Address must be a org.jgroups.util.UUID");
      }
      return new CommandInvocationId(addr, nextId.getAndIncrement(), uuidAddr.getMostSignificantBits(), uuidAddr.getLeastSignificantBits());
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }

      CommandInvocationId that = (CommandInvocationId) o;

      return id == that.id && Objects.equals(address, that.address);

   }

   @Override
   public int hashCode() {
      int result = address != null ? address.hashCode() : 0;
      result = 31 * result + (int) (id ^ (id >>> 32));
      return result;
   }

   @Override
   public String toString() {
      return "CommandInvocation:" + Objects.toString(address, "local") + ":" + id;
   }

   public static String show(CommandInvocationId id) {
      return id == DUMMY_INVOCATION_ID ? "" : id.toString();
   }
}
