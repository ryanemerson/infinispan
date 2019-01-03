package org.infinispan.marshall.persistence.impl;

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

/**
 * @author Mircea Markus
 * @since 6.0
 */
@Deprecated
public class MarshalledEntryImpl<K,V> extends MarshallableEntryImpl<K,V> implements MarshalledEntry<K,V> {

   private InternalMetadata metadata;

   MarshalledEntryImpl(ByteBuffer key, ByteBuffer valueBytes, ByteBuffer metadataBytes, org.infinispan.commons.marshall.Marshaller marshaller) {
      super(key, valueBytes, metadataBytes, -1, -1, marshaller);
   }

   MarshalledEntryImpl(K key, ByteBuffer valueBytes, ByteBuffer metadataBytes, org.infinispan.commons.marshall.Marshaller marshaller) {
      super(key, valueBytes, metadataBytes, marshaller);
   }

   MarshalledEntryImpl(K key, V value, InternalMetadata metadata, org.infinispan.commons.marshall.Marshaller marshaller) {
      super(key, value, metadata, metadata.created(), metadata.lastUsed(), marshaller);
      this.metadata = metadata;
   }

   @Override
   public InternalMetadata getMetadata() {
      if (metadata == null) {
         if (metadataBytes == null) {
            return null;
         } else {
            Metadata meta = unmarshall(metadataBytes);
            if (meta instanceof InternalMetadataImpl)
               metadata = (InternalMetadata) meta;
            else {
               metadata = new InternalMetadataImpl(meta, created(), lastUsed());
            }
         }
      }
      return metadata;
   }

   @Override
   public ByteBuffer getMetadataBytes() {
      return metadataBytes();
   }

   @Override
   public boolean isExpired(long now) {
      return metadata != null && metadata.isExpired(now);
   }

   @Override
   public long expiryTime() {
      return metadata == null ? -1 : metadata.expiryTime();
   }

   @Override
   public long created() {
      return metadata == null ? -1 : metadata.created();
   }

   @Override
   public long lastUsed() {
      return metadata == null ? -1 : metadata.lastUsed();
   }

   public static class Externalizer extends AbstractExternalizer<MarshalledEntry> {

      private static final long serialVersionUID = -5291318076267612501L;

      private final Marshaller marshaller;

      public Externalizer(Marshaller marshaller) {
         this.marshaller = marshaller;
      }

      @Override
      public void writeObject(ObjectOutput output, MarshalledEntry me) throws IOException {
         output.writeObject(me.getKeyBytes());
         output.writeObject(me.getValueBytes());
         output.writeObject(me.getMetadataBytes());
      }

      @Override
      public MarshalledEntry readObject(ObjectInput input) throws IOException, ClassNotFoundException {
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
      public Set<Class<? extends MarshalledEntry>> getTypeClasses() {
         return Util.asSet(MarshalledEntryImpl.class, MarshalledEntry.Wrapper.class);
      }
   }
}
