package org.infinispan.persistence.sifs;

import java.nio.ByteBuffer;

/**
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public class EntryHeader {
   static final int MAGIC = 0xBE11A61C;
   static final boolean useMagic = false;
   static final int HEADER_SIZE = 24 + 8 + 8 + (useMagic ? 4 : 0);

   private final int keyLength;
   private final int valueLength;
   private final int metadataLength;
   private final long seqId;
   private final long expiration;
   private final long created;
   private final long lastUsed;

   public EntryHeader(ByteBuffer buffer) {
      if (useMagic) {
         if (buffer.getInt() != MAGIC) throw new IllegalStateException();
      }
      this.keyLength = buffer.getShort();
      this.metadataLength = buffer.getShort();
      this.valueLength = buffer.getInt();
      this.seqId = buffer.getLong();
      this.expiration = buffer.getLong();
      this.created = buffer.getLong();
      this.lastUsed = buffer.getLong();
   }

   public int keyLength() {
      return keyLength;
   }

   public int metadataLength() {
      return metadataLength;
   }

   public int valueLength() {
      return valueLength;
   }

   public long seqId() {
      return seqId;
   }

   public long expiryTime() {
      return expiration;
   }

   public long getCreated() {
      return created;
   }

   public long getLastUsed() {
      return lastUsed;
   }

   @Override
   public String toString() {
      return String.format("[keyLength=%d, valueLength=%d, seqId=%d, expiration=%d]", keyLength, valueLength, seqId, expiration);
   }

   public int totalLength() {
      return keyLength + metadataLength + valueLength + HEADER_SIZE;
   }
}
