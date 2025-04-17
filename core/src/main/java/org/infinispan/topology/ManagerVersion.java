package org.infinispan.topology;

import java.util.Objects;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.commons.util.Version;
import org.infinispan.protostream.annotations.Proto;
import org.infinispan.protostream.annotations.ProtoTypeId;

@Proto
@ProtoTypeId(ProtoStreamTypeIds.MANAGER_VERSION)
public record ManagerVersion(int major, int minor, int patch) implements Comparable<ManagerVersion> {

   public static final ManagerVersion SIXTEEN = new ManagerVersion(16, 0, 0);
   public static final ManagerVersion INSTANCE;

   static {
      int major = Integer.parseInt(Version.getMajor());
      int minor = Integer.parseInt(Version.getMinor());
      int patch = Integer.parseInt(Version.getMinor());
      INSTANCE = new ManagerVersion(major, minor, patch);
   }

   public boolean lessThan(ManagerVersion other) {
      return compareTo(other) < 0;
   }

   @Override
   public int compareTo(ManagerVersion o) {
      if (major != o.major) return major - o.major;
      if (minor != o.minor) return minor - o.minor;
      return patch - o.patch;
   }

   @Override
   public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) return false;
      ManagerVersion that = (ManagerVersion) o;
      return major == that.major && minor == that.minor && patch == that.patch;
   }

   @Override
   public int hashCode() {
      return Objects.hash(major, minor, patch);
   }

   @Override
   public String toString() {
      return String.format("%d.%d.%d", major, minor, patch);
   }
}
