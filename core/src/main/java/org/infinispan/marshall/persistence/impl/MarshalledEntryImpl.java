package org.infinispan.marshall.persistence.impl;

import static java.lang.Math.min;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;

import org.infinispan.commons.io.ByteBuffer;
import org.infinispan.commons.marshall.AbstractExternalizer;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.commons.util.Util;
import org.infinispan.marshall.core.Ids;
import org.infinispan.marshall.core.MarshalledEntry;
import org.infinispan.metadata.InternalMetadata;
import org.infinispan.metadata.Metadata;
import org.infinispan.metadata.impl.InternalMetadataImpl;
import org.infinispan.persistence.spi.PersistenceException;

/**
 * @author Mircea Markus
 * @since 6.0
 */
public class MarshalledEntryImpl<K,V> implements MarshalledEntry<K,V> {

   private ByteBuffer keyBytes;
   private ByteBuffer valueBytes;
   private ByteBuffer metadataBytes;
   private long created;
   private long lastUsed;
   private transient K key;
   private transient V value;
   private transient Metadata metadata;
   private final transient org.infinispan.commons.marshall.Marshaller marshaller;

   MarshalledEntryImpl(ByteBuffer key, ByteBuffer valueBytes, ByteBuffer metadataBytes, long created, long lastUsed, org.infinispan.commons.marshall.Marshaller marshaller) {
      this.keyBytes = key;
      this.valueBytes = valueBytes;
      this.metadataBytes = metadataBytes;
      this.created = created;
      this.lastUsed = lastUsed;
      this.marshaller = marshaller;
   }

   MarshalledEntryImpl(K key, ByteBuffer valueBytes, ByteBuffer metadataBytes, org.infinispan.commons.marshall.Marshaller marshaller) {
      this.key = key;
      this.valueBytes = valueBytes;
      this.metadataBytes = metadataBytes;
      this.marshaller = marshaller;
   }

   MarshalledEntryImpl(K key, V value, Metadata metadata, long created, long lastUsed, org.infinispan.commons.marshall.Marshaller marshaller) {
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
   public InternalMetadata getMetadata() {
      return new InternalMetadataImpl(metadata(), created, lastUsed);
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
   public ByteBuffer getMetadataBytes() {
      if (metadataBytes == null) {
         if  (metadata == null)
            return null;
         metadataBytes = marshall(metadata);
      }
      return metadataBytes;
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

   private ByteBuffer marshall(Object obj) {
      try {
         return marshaller.objectToBuffer(obj);
      } catch (Exception e) {
         throw new PersistenceException(e);
      }
   }

   @SuppressWarnings(value = "unchecked")
   private <T> T unmarshall(ByteBuffer buf) {
      try {
         return (T) marshaller.objectFromByteBuffer(buf.getBuf(), buf.getOffset(), buf.getLength());
      } catch (Exception e) {
         throw new PersistenceException(e);
      }
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof MarshalledEntryImpl)) return false;

      MarshalledEntryImpl that = (MarshalledEntryImpl) o;

      if (getKeyBytes() != null ? !getKeyBytes().equals(that.getKeyBytes()) : that.getKeyBytes() != null) return false;
      if (metadataBytes() != null ? !metadataBytes().equals(that.metadataBytes()) : that.metadataBytes() != null) return false;
      if (getValueBytes() != null ? !getValueBytes().equals(that.getValueBytes()) : that.getValueBytes() != null) return false;
      if (created != that.created) return false;
      if (lastUsed != that.lastUsed) return false;
      return true;
   }

   @Override
   public int hashCode() {
      int result = getKeyBytes() != null ? getKeyBytes().hashCode() : 0;
      result = 31 * result + (getValueBytes() != null ? getValueBytes().hashCode() : 0);
      result = 31 * result + (metadataBytes() != null ? metadataBytes().hashCode() : 0);
      result = 31 * result + (int) (created ^ (created >>> 32));
      result = 31 * result + (int) (lastUsed ^ (lastUsed >>> 32));
      return result;
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder().append("MarshalledEntryImpl{")
            .append("keyBytes=").append(keyBytes)
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

   public static class Externalizer extends AbstractExternalizer<MarshalledEntryImpl> {

      private static final long serialVersionUID = -5291318076267612501L;

      private final Marshaller marshaller;

      public Externalizer(Marshaller marshaller) {
         this.marshaller = marshaller;
      }

      @Override
      public void writeObject(ObjectOutput output, MarshalledEntryImpl me) throws IOException {
         output.writeObject(me.getKeyBytes());
         output.writeObject(me.getValueBytes());
         output.writeObject(me.metadataBytes());
      }

      @Override
      public MarshalledEntryImpl readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            ByteBuffer keyBytes = (ByteBuffer) input.readObject();
            ByteBuffer valueBytes = (ByteBuffer) input.readObject();
            ByteBuffer metadataBytes = (ByteBuffer) input.readObject();
            return new MarshalledEntryImpl(keyBytes, valueBytes, metadataBytes, marshaller);
      }

      @Override
      public Integer getId() {
         return Ids.MARSHALLED_ENTRY_ID;
      }

      @Override
      @SuppressWarnings("unchecked")
      public Set<Class<? extends MarshalledEntryImpl>> getTypeClasses() {
         return Util.asSet(MarshalledEntryImpl.class);
      }
   }
}
