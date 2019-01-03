package org.infinispan.marshall.persistence.impl;

import static java.lang.Math.min;

import org.infinispan.commons.io.ByteBuffer;
import org.infinispan.metadata.Metadata;
import org.infinispan.persistence.spi.MarshallableEntry;
import org.infinispan.persistence.spi.PersistenceException;

/**
 * @author Ryan Emerson
 * @since 10.0
 */
public class MarshallableEntryImpl<K, V> implements MarshallableEntry<K, V> {

   private ByteBuffer keyBytes;
   private ByteBuffer valueBytes;
   ByteBuffer metadataBytes;
   private long created;
   private long lastUsed;
   private transient K key;
   private transient V value;
   private transient Metadata metadata;
   private final transient org.infinispan.commons.marshall.Marshaller marshaller;

   MarshallableEntryImpl(ByteBuffer key, ByteBuffer valueBytes, ByteBuffer metadataBytes, long created, long lastUsed, org.infinispan.commons.marshall.Marshaller marshaller) {
      this.keyBytes = key;
      this.valueBytes = valueBytes;
      this.metadataBytes = metadataBytes;
      this.created = created;
      this.lastUsed = lastUsed;
      this.marshaller = marshaller;
   }

   MarshallableEntryImpl(K key, ByteBuffer valueBytes, ByteBuffer metadataBytes, org.infinispan.commons.marshall.Marshaller marshaller) {
      this.key = key;
      this.valueBytes = valueBytes;
      this.metadataBytes = metadataBytes;
      this.marshaller = marshaller;
   }

   MarshallableEntryImpl(K key, V value, Metadata metadata, long created, long lastUsed, org.infinispan.commons.marshall.Marshaller marshaller) {
      this.key = key;
      this.value = value;
      this.metadata = metadata;
      this.created = created;
      this.lastUsed = lastUsed;
      this.marshaller = marshaller;
   }

   @Override
   public K getKey() {
      if (key == null) {
         if (keyBytes == null) {
            return null;
         }
         key = unmarshall(keyBytes);
      }
      return key;
   }

   @Override
   public V getValue() {
      if (value == null) {
         if (valueBytes == null) {
            return null;
         }
         value = unmarshall(valueBytes);
      }
      return value;
   }

   @Override
   public Metadata metadata() {
      if (metadata == null) {
         if (metadataBytes == null)
            return null;
         else
            metadata = unmarshall(metadataBytes);
      }
      return metadata;
   }

   @Override
   public ByteBuffer getKeyBytes() {
      if (keyBytes == null) {
         if (key == null) {
            return null;
         }
         keyBytes = marshall(key);
      }
      return keyBytes;
   }

   @Override
   public ByteBuffer getValueBytes() {
      if (valueBytes == null) {
         if (value == null) {
            return null;
         }
         valueBytes = marshall(value);
      }
      return valueBytes;
   }

   @Override
   public ByteBuffer metadataBytes() {
      if (metadataBytes == null) {
         if  (metadata == null)
            return null;
         metadataBytes = marshall(metadata);
      }
      return metadataBytes;
   }

   @Override
   public long created() {
      return created;
   }

   @Override
   public long lastUsed() {
      return lastUsed;
   }

   @Override
   public boolean isExpired(long now) {
      long expiry = expiryTime();
      return expiry > 0 && expiry <= now;
   }

   @Override
   public long expiryTime() {
      if (metadata == null) return -1;
      long lifespan = metadata.lifespan();
      long lset = lifespan > -1 ? created + lifespan : -1;
      long maxIdle = metadata.maxIdle();
      long muet = maxIdle > -1 ? lastUsed + maxIdle : -1;
      if (lset == -1) return muet;
      if (muet == -1) return lset;
      return min(lset, muet);
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof MarshalledEntryImpl)) return false;

      MarshalledEntryImpl that = (MarshalledEntryImpl) o;

      if (getKeyBytes() != null ? !getKeyBytes().equals(that.getKeyBytes()) : that.getKeyBytes() != null) return false;
      if (metadataBytes() != null ? !metadataBytes().equals(that.metadataBytes()) : that.metadataBytes() != null) return false;
      if (getValueBytes() != null ? !getValueBytes().equals(that.getValueBytes()) : that.getValueBytes() != null) return false;
      if (expiryTime() != that.expiryTime()) return false;
      return true;
   }

   @Override
   public int hashCode() {
      long expiryTime = expiryTime();
      int result = getKeyBytes() != null ? getKeyBytes().hashCode() : 0;
      result = 31 * result + (getValueBytes() != null ? getValueBytes().hashCode() : 0);
      result = 31 * result + (metadataBytes() != null ? metadataBytes().hashCode() : 0);
      result = 31 * result + (int) (expiryTime ^ (expiryTime >>> 32));
      return result;
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder().append(this.getClass().getSimpleName())
            .append("{keyBytes=").append(keyBytes)
            .append(", valueBytes=").append(valueBytes)
            .append(", metadataBytes=").append(metadataBytes)
            .append(", key=").append(key);
      if (key == null && keyBytes != null && marshaller != null) {
         sb.append('/').append(this.<Object>unmarshall(keyBytes));
      }
      sb.append(", value=").append(value);
      if (value == null && valueBytes != null && marshaller != null) {
         sb.append('/').append(this.<Object>unmarshall(valueBytes));
      }
      sb.append(", metadata=").append(metadata);
      if (metadata == null && metadataBytes != null && marshaller != null) {
         sb.append('/').append(this.<Object>unmarshall(metadataBytes));
      }
      sb.append(", marshaller=").append(marshaller).append('}');
      return sb.toString();
   }

   ByteBuffer marshall(Object obj) {
      try {
         return marshaller.objectToBuffer(obj);
      } catch (Exception e) {
         throw new PersistenceException(e);
      }
   }

   @SuppressWarnings(value = "unchecked")
   <T> T unmarshall(ByteBuffer buf) {
      try {
         return (T) marshaller.objectFromByteBuffer(buf.getBuf(), buf.getOffset(), buf.getLength());
      } catch (Exception e) {
         throw new PersistenceException(e);
      }
   }
}
