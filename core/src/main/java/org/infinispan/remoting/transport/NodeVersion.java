package org.infinispan.remoting.transport;

import java.util.Objects;

import org.infinispan.commons.util.Version;

public record NodeVersion(byte major, byte minor, byte patch) implements Comparable<NodeVersion> {

   public static final NodeVersion INSTANCE;

   static {
      byte major = Byte.parseByte(Version.getMajor());
      byte minor = Byte.parseByte(Version.getMinor());
      byte patch = Byte.parseByte(Version.getMinor());
      INSTANCE = new NodeVersion(major, minor, patch);
   }

   public boolean lessThan(NodeVersion other) {
      return compareTo(other) < 0;
   }

   @Override
   public int compareTo(NodeVersion o) {
      if (major != o.major) return major - o.major;
      if (minor != o.minor) return minor - o.minor;
      return patch - o.patch;
   }

   @Override
   public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) return false;
      NodeVersion that = (NodeVersion) o;
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
