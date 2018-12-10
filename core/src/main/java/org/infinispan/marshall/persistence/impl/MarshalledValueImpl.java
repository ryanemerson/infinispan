package org.infinispan.marshall.persistence.impl;

import java.util.Objects;

import org.infinispan.commons.io.ByteBuffer;
import org.infinispan.persistence.spi.MarshalledValue;

/**
 * A marshallable object that can be used by our internal store implementations to store values, metadata and timestamps.
 *
 * @author Ryan Emerson
 * @since 10.0
 */
public class MarshalledValueImpl implements MarshalledValue {

   static final MarshalledValue EMPTY = new MarshalledValueImpl();

   ByteBuffer valueBytes;
   ByteBuffer metadataBytes;
   long created;
   long lastUsed;

   MarshalledValueImpl(ByteBuffer valueBytes, ByteBuffer metadataBytes, long created, long lastUsed) {
      this.valueBytes = valueBytes;
      this.metadataBytes = metadataBytes;
      this.created = created;
      this.lastUsed = lastUsed;
   }

   MarshalledValueImpl() {}

   @Override
   public ByteBuffer getValueBytes() {
      return valueBytes;
   }

   public void setValueBytes(ByteBuffer valueBytes) {
      this.valueBytes = valueBytes;
   }

   @Override
   public ByteBuffer getMetadataBytes() {
      return metadataBytes;
   }

   public void setMetadataBytes(ByteBuffer metadataBytes) {
      this.metadataBytes = metadataBytes;
   }

   @Override
   public long getCreated() {
      return created;
   }

   public void setCreated(long created) {
      this.created = created;
   }

   @Override
   public long getLastUsed() {
      return lastUsed;
   }

   public void setLastUsed(long lastUsed) {
      this.lastUsed = lastUsed;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      MarshalledValueImpl that = (MarshalledValueImpl) o;
      return created == that.created &&
            lastUsed == that.lastUsed &&
            Objects.equals(valueBytes, that.valueBytes) &&
            Objects.equals(metadataBytes, that.metadataBytes);
   }

   @Override
   public int hashCode() {
      return Objects.hash(valueBytes, metadataBytes, created, lastUsed);
   }

   @Override
   public String toString() {
      return "MarshalledValueImpl{" +
            "valueBytes=" + valueBytes +
            ", metadataBytes=" + metadataBytes +
            ", created=" + created +
            ", lastUsed=" + lastUsed +
            '}';
   }
}
