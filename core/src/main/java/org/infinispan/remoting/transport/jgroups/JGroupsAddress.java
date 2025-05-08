package org.infinispan.remoting.transport.jgroups;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.function.Supplier;

import org.infinispan.commons.marshall.MarshallingException;
import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.remoting.transport.TopologyAwareAddress;
import org.jgroups.Address;
import org.jgroups.Constructable;
import org.jgroups.conf.ClassConfigurator;
import org.jgroups.util.ExtendedUUID;
import org.jgroups.util.NameCache;
import org.jgroups.util.UUID;
import org.jgroups.util.Util;

/**
 * An encapsulation of a JGroups {@link ExtendedUUID} with a site id, a rack id, and a machine id.
 *
 * @author Bela Ban
 * @since 5.0
 */
@ProtoTypeId(ProtoStreamTypeIds.JGROUPS_ADDRESS)
// TODO
// Write version first to allow possible migrations/changes in future
// Do we need keys? Just write string first
public class JGroupsAddress implements Constructable<JGroupsAddress>, TopologyAwareAddress {

   static {
      // Must not conflict with value in jg-magic-map.xml
      ClassConfigurator.add((short)1024, JGroupsAddress.class);
   }

   // TODO add version
   private static final byte SITE_INDEX = 0;
   private static final byte RACK_INDEX = 1;
   private static final byte MACHINE_INDEX = 2;

   public static final JGroupsAddress LOCAL = random();

   private org.jgroups.Address address;
   private int hashCode;
   private byte[][] values;
   private volatile byte[] bytes;

   public static JGroupsAddress random() {
      return random(null);
   }

   public static JGroupsAddress random(String name) {
      return random(name, null, null, null);
   }

   public static JGroupsAddress random(String name, String siteId, String rackId, String machineId) {
      var address = new JGroupsAddress(UUID.randomUUID(), siteId, rackId, machineId);
      if (name != null) {
         NameCache.add(address, name);
      }
      return address;
   }

   @ProtoFactory
   static JGroupsAddress protoFactory(byte[] bytes) throws IOException {
      try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
         // Note: Use org.jgroups.Address, not the concrete UUID class.
         // Otherwise applications that only use local caches would have to bundle the JGroups jar,
         // because the verifier needs to check the arguments of fromJGroupsAddress
         // even if this method is never called.
         org.jgroups.Address address = org.jgroups.util.Util.readAddress(in);
         return (JGroupsAddress) JGroupsAddressCache.fromJGroupsAddress(address);
      } catch (ClassNotFoundException e) {
         throw new MarshallingException(e);
      }
   }

   /**
    * Required so that new instances can be created via {@link org.jgroups.util.Streamable}.
    */
   @SuppressWarnings("unused")
   public JGroupsAddress() {
   }

   public JGroupsAddress(ExtendedUUID address) {
      if (address == null)
         throw new IllegalArgumentException("Address shall not be null");
      this.address = address;
      this.hashCode = address.hashCode();
   }

   // TODO add version
   private JGroupsAddress(UUID uuid, String siteId, String rackId, String machineId) {
      this.address = uuid;
      this.hashCode = uuid.hashCode();
      if (siteId == null && rackId == null && machineId == null) {
         this.values = null;
      } else {
         this.values = new byte[3][];
         this.values[0] = Util.stringToBytes(siteId);
         this.values[1] = Util.stringToBytes(rackId);
         this.values[2] = Util.stringToBytes(machineId);
      }
   }

   @Override
   public Supplier<? extends JGroupsAddress> create() {
      return JGroupsAddress::new;
   }

   @ProtoField(1)
   byte[] getBytes() throws IOException {
      if (bytes == null) {
         try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
              DataOutputStream out = new DataOutputStream(baos)) {
            org.jgroups.util.Util.writeAddress(this, out);
            bytes = baos.toByteArray();
         }
      }
      return bytes;
   }

   @Override
   public int serializedSize() {
      // TODO add version
      var topologySize = values == null ? 0 : values.length;
      return address.serializedSize() + Byte.BYTES + topologySize;
   }

   @Override
   public void writeTo(DataOutput out) throws IOException {
      // version
      // TODO


      // Address
      Util.writeAddress(address, out);

      // Topology information
      int len = values == null ? 0 : values.length;
      out.writeByte(len);
      for (int i = 0; i < len; i++) {
         byte[] v = values[i];
         len = v == null ? 0 : v.length;
         out.write(len);
         if (len > 0) {
            out.write(v);
         }
      }
   }

   @Override
   public void readFrom(DataInput in) throws ClassNotFoundException, IOException {
      // Version
      // TODO

      // Address
      address = Util.readAddress(in);
      hashCode = address.hashCode();

      // Topology Information
      int len = in.readByte();
      values = new byte[len][];
      for (int i = 0; i < len; i++) {
         len = in.readByte();
         if (len > 0) {
            values[i] = new byte[len];
            in.readFully(values[i]);
         }
      }
   }

   @Override
   public String getSiteId() {
      return getTopologyValue(SITE_INDEX);
   }

   @Override
   public String getRackId() {
      return getTopologyValue(RACK_INDEX);
   }

   @Override
   public String getMachineId() {
      return getTopologyValue(MACHINE_INDEX);
   }

   @Override
   public boolean isSameSite(TopologyAwareAddress addr) {
      if (addr instanceof JGroupsAddress other) {
         return checkComponents(other, SITE_INDEX);
      }
      return checkComponent(SITE_INDEX, addr.getSiteId());
   }

   @Override
   public boolean isSameRack(TopologyAwareAddress addr) {
      if (addr instanceof JGroupsAddress other) {
         return checkComponents(other, SITE_INDEX, RACK_INDEX);
      }
      return checkComponent(SITE_INDEX, addr.getSiteId()) && checkComponent(RACK_INDEX, addr.getSiteId());
   }

   @Override
   public boolean isSameMachine(TopologyAwareAddress addr) {
      if (addr instanceof JGroupsAddress other) {
         return checkComponents(other, SITE_INDEX, RACK_INDEX, MACHINE_INDEX);
      }
      return checkComponent(SITE_INDEX, addr.getSiteId()) &&
            checkComponent(RACK_INDEX, addr.getSiteId()) &&
            checkComponent(MACHINE_INDEX, addr.getMachineId());
   }

   private boolean checkComponents(JGroupsAddress other, byte... indexes) {
      for (int i = 0; i < indexes.length; i++) {
         if (!Arrays.equals(indexes, other.values[i])) {
            return false;
         }
      }
      return true;
   }

   private boolean checkComponent(byte i, String expectedValue) {
      return values[i] != null && Arrays.equals(values[i], Util.stringToBytes(expectedValue));
   }

   private String getTopologyValue(byte i) {
      return values == null ? null : Util.bytesToString(values[i]);
   }

   public org.jgroups.Address getJGroupsAddress() {
      return this;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      JGroupsAddress that = (JGroupsAddress) o;

      return hashCode == that.hashCode && address.equals(that.address);
   }

   @Override
   public int hashCode() {
      return hashCode;
   }

   @Override
   public String toString() {
      String val = NameCache.get(this);
      return val != null ? val : String.valueOf(address);
   }

   @Override
   public int compareTo(Address o) {
      JGroupsAddress oa = (JGroupsAddress) o;
      return address.compareTo(oa.address);
   }
}
