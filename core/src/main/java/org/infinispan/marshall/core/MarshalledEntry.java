package org.infinispan.marshall.core;

import org.infinispan.commons.io.ByteBuffer;
import org.infinispan.metadata.InternalMetadata;
import org.infinispan.metadata.Metadata;
import org.infinispan.metadata.impl.InternalMetadataImpl;
import org.infinispan.persistence.spi.MarshallableEntry;

/**
 * Defines an externally persisted entry. External stores that keep the data in serialised form should return an
 * MarshalledEntry that contains the data in binary form (ByteBuffer) and unmarshall it lazily when
 * getKey/Value/Metadata are invoked. This approach avoids unnecessary object (de)serialization e.g
 * when the entries are fetched from the external store for the sole purpose of being sent over the wire to
 * another requestor node.
 *
 * @author Mircea Markus
 * @since 6.0
 * @deprecated since 10.0, use {@link MarshallableEntry}
 */
@Deprecated
public interface MarshalledEntry<K,V> extends MarshallableEntry<K,V> {

   ByteBuffer getMetadataBytes();

   InternalMetadata getMetadata();

   /**
    * A simple wrapper to convert a {@link org.infinispan.persistence.spi.MarshallableEntry} to a {@link MarshalledEntry}
    * for backwards compatibility.
    */
   static <K,V> MarshalledEntry<K,V> wrap(MarshallableEntry<K,V> entry) {
      return entry instanceof MarshalledEntry ? (MarshalledEntry<K,V>) entry : new Wrapper<>(entry);
   }

   class Wrapper<K,V> implements MarshalledEntry<K,V> {

      MarshallableEntry<K,V> entry;

      Wrapper(MarshallableEntry<K, V> entry) {
         this.entry = entry;
      }

      @Override
      public ByteBuffer getKeyBytes() {
         return entry.getKeyBytes();
      }

      @Override
      public ByteBuffer getValueBytes() {
         return entry.getValueBytes();
      }

      @Override
      public ByteBuffer metadataBytes() {
         return entry.metadataBytes();
      }

      @Override
      public ByteBuffer getMetadataBytes() {
         return metadataBytes();
      }

      @Override
      public InternalMetadata getMetadata() {
         Metadata meta = metadata();
         return meta == null ? null : new InternalMetadataImpl(meta, created(), lastUsed());
      }

      @Override
      public K getKey() {
         return entry.getKey();
      }

      @Override
      public V getValue() {
         return entry.getValue();
      }

      @Override
      public Metadata metadata() {
         return entry.metadata();
      }

      @Override
      public long created() {
         return entry.created();
      }

      @Override
      public long lastUsed() {
         return entry.lastUsed();
      }

      @Override
      public boolean isExpired(long now) {
         return entry.isExpired(now);
      }

      @Override
      public long expiryTime() {
         return entry.expiryTime();
      }
   };
}
