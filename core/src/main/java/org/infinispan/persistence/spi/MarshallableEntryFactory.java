package org.infinispan.persistence.spi;

import org.infinispan.commons.io.ByteBuffer;
import org.infinispan.metadata.Metadata;

/**
 * Factory for {@link MarshallableEntry}.
 *
 * @author Ryan Emerson
 * @since 10.0
 */
public interface MarshallableEntryFactory<K,V> {

   MarshallableEntry<K,V> create(ByteBuffer key, ByteBuffer valueBytes, ByteBuffer metadataBytes);

   MarshallableEntry<K,V> create(ByteBuffer key, ByteBuffer valueBytes, ByteBuffer metadataBytes, long created, long lastUsed);

   MarshallableEntry<K,V> create(Object key, ByteBuffer valueBytes, ByteBuffer metadataBytes);

   MarshallableEntry<K,V> create(Object key);

   MarshallableEntry<K,V> create(Object key, Object value);

   MarshallableEntry<K,V> create(Object key, Object value, Metadata metadata);

   MarshallableEntry<K,V> create(Object key, Object value, Metadata metadata, long created, long lastUsed);

   /**
    * @return a cached empty {@link MarshallableEntry} instance.
    */
   MarshallableEntry<K,V> getEmpty();
}
