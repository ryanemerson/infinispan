package org.infinispan.container.entries.metadata;

import static java.lang.Math.min;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.container.entries.AbstractInternalCacheEntry;
import org.infinispan.container.entries.ExpiryHelper;
import org.infinispan.container.entries.InternalCacheValue;
import org.infinispan.container.entries.TransientMortalCacheEntry;
import org.infinispan.container.entries.versioned.Versioned;
import org.infinispan.functional.impl.MetaParamsInternalMetadata;
import org.infinispan.marshall.protostream.impl.MarshallableObject;
import org.infinispan.marshall.protostream.impl.MarshallableUserObject;
import org.infinispan.metadata.Metadata;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * A form of {@link TransientMortalCacheEntry} that is {@link Versioned}
 *
 * @author Manik Surtani
 * @since 5.1
 */
@ProtoTypeId(ProtoStreamTypeIds.METADATA_TRANSIENT_MORTAL_CACHE_ENTRY)
public class MetadataTransientMortalCacheEntry extends AbstractInternalCacheEntry implements MetadataAware {

   MarshallableObject<Metadata> metadata;
   long created;
   long lastUsed;

   public MetadataTransientMortalCacheEntry(Object key, Object value, Metadata metadata, long now) {
      this(key, value, metadata, now, now);
   }

   public MetadataTransientMortalCacheEntry(Object key, Object value, Metadata metadata, long lastUsed, long created) {
      this(key, value, null, metadata, lastUsed, created);
   }

   protected MetadataTransientMortalCacheEntry(Object key, Object value, MetaParamsInternalMetadata internalMetadata,
         Metadata metadata, long lastUsed, long created) {
      super(key, value, internalMetadata);
      this.setMetadata(metadata);
      this.lastUsed = lastUsed;
      this.created = created;
   }

   @ProtoFactory
   MetadataTransientMortalCacheEntry(MarshallableUserObject<?> wrappedKey, MarshallableUserObject<?> wrappedValue,
                                     MetaParamsInternalMetadata internalMetadata, MarshallableObject<Metadata> wrappedMetadata,
                                     long created, long lastUsed) {
      super(wrappedKey, wrappedValue, internalMetadata);
      this.metadata = wrappedMetadata;
      this.created = created;
      this.lastUsed = lastUsed;
   }

   @ProtoField(number = 4, name ="metadata")
   public MarshallableObject<Metadata> getWrappedMetadata() {
      return metadata;
   }

   @Override
   @ProtoField(number = 5, defaultValue = "-1")
   public long getCreated() {
      return created;
   }

   @Override
   @ProtoField(number = 6, defaultValue = "-1")
   public long getLastUsed() {
      return lastUsed;
   }

   @Override
   public long getLifespan() {
      return getMetadata().lifespan();
   }

   @Override
   public final boolean canExpire() {
      return true;
   }

   @Override
   public boolean isExpired(long now) {
      Metadata metadata = getMetadata();
      return ExpiryHelper.isExpiredTransientMortal(metadata.maxIdle(), lastUsed, metadata.lifespan(), created, now);
   }

   @Override
   public boolean canExpireMaxIdle() {
      return true;
   }

   @Override
   public final long getExpiryTime() {
      Metadata metadata = getMetadata();
      long lifespan = metadata.lifespan();
      long lset = lifespan > -1 ? created + lifespan : -1;
      long maxIdle = metadata.maxIdle();
      long muet = maxIdle > -1 ? lastUsed + maxIdle : -1;
      if (lset == -1) {
         return muet;
      }
      if (muet == -1) {
         return lset;
      }
      return min(lset, muet);
   }

   @Override
   public InternalCacheValue<?> toInternalCacheValue() {
      return new MetadataTransientMortalCacheValue(value, internalMetadata, metadata, created, lastUsed);
   }

   @Override
   public final void touch(long currentTimeMillis) {
      lastUsed = currentTimeMillis;
   }

   @Override
   public void reincarnate(long now) {
      created = now;
   }

   @Override
   public long getMaxIdle() {
      return getMetadata().maxIdle();
   }

   @Override
   public Metadata getMetadata() {
      return MarshallableObject.unwrap(metadata);
   }

   @Override
   public void setMetadata(Metadata metadata) {
      this.metadata = MarshallableObject.create(metadata);
   }

   @Override
   protected void appendFieldsToString(StringBuilder builder) {
      super.appendFieldsToString(builder);
      builder.append(", metadata=").append(metadata);
      builder.append(", created=").append(created);
      builder.append(", lastUsed=").append(lastUsed);
   }
}
