package org.infinispan.marshall.persistence.impl;

import org.infinispan.commons.io.ByteBuffer;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.marshall.core.MarshalledEntry;
import org.infinispan.marshall.core.MarshalledEntryFactory;
import org.infinispan.metadata.InternalMetadata;
import org.infinispan.metadata.Metadata;
import org.infinispan.metadata.impl.InternalMetadataImpl;
import org.infinispan.persistence.spi.MarshallableEntry;
import org.infinispan.persistence.spi.MarshallableEntryFactory;

/**
 * @author Ryan Emerson
 * @since 10.0
 */
public class MarshalledEntryFactoryImpl implements MarshalledEntryFactory, MarshallableEntryFactory {

   private static final MarshallableEntry EMPTY = new MarshalledEntryImpl(null, null, (ByteBuffer) null, null);

   @Inject private Marshaller marshaller;

   public MarshalledEntryFactoryImpl() {
   }

   public MarshalledEntryFactoryImpl(Marshaller marshaller) {
      this.marshaller = marshaller;
   }

   @Override
   public MarshallableEntry create(ByteBuffer key, ByteBuffer valueBytes, ByteBuffer metadataBytes) {
      return newMarshalledEntry(key, valueBytes, metadataBytes);
   }

   @Override
   public MarshallableEntry create(ByteBuffer key, ByteBuffer valueBytes, ByteBuffer metadataBytes, long created, long lastUsed) {
      return new MarshalledEntryImpl<>(key, valueBytes, metadataBytes, created, lastUsed, marshaller);
   }

   @Override
   public MarshallableEntry create(Object key, ByteBuffer valueBytes, ByteBuffer metadataBytes) {
      return newMarshalledEntry(key, valueBytes, metadataBytes);
   }

   @Override
   public MarshallableEntry create(Object key) {
      return create(key, null, (Metadata) null);
   }

   @Override
   public MarshallableEntry create(Object key, Object value) {
      return create(key, value, null);
   }

   @Override
   public MarshallableEntry create(Object key, Object value, Metadata metadata) {
      return create(key, value, metadata, -1, -1);
   }

   @Override
   public MarshallableEntry create(Object key, Object value, Metadata metadata, long created, long lastUsed) {
      return new MarshalledEntryImpl<>(key, value, metadata, created, lastUsed, marshaller);
   }

   @Override
   public MarshallableEntry getEmpty() {
      return EMPTY;
   }

   @Override
   public MarshalledEntry newMarshalledEntry(ByteBuffer key, ByteBuffer valueBytes, ByteBuffer metadataBytes) {
      return new MarshalledEntryImpl<>(key, valueBytes, metadataBytes, marshaller);
   }

   @Override
   public MarshalledEntry newMarshalledEntry(Object key, ByteBuffer valueBytes, ByteBuffer metadataBytes) {
      return new MarshalledEntryImpl<>(key, valueBytes, metadataBytes, marshaller);
   }

   @Override
   public MarshalledEntry newMarshalledEntry(Object key, Object value, InternalMetadata im) {
      if (im == null)
         return new MarshalledEntryImpl<>(key, value, (Metadata) null, -1, -1, marshaller);

      long created = im.created();
      long lastUsed = im.lastUsed();
      if (im instanceof InternalMetadataImpl) {
         InternalMetadataImpl impl = (InternalMetadataImpl) im;
         return new MarshalledEntryImpl<>(key, value, impl.actual(), created, lastUsed, marshaller);
      }
      return new MarshalledEntryImpl<>(key, value, null, created, lastUsed, marshaller);
   }
}
