package org.infinispan.container.entries.metadata;

import static java.lang.Math.min;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.container.entries.ExpiryHelper;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.container.entries.TransientMortalCacheValue;
import org.infinispan.container.entries.versioned.Versioned;
import org.infinispan.functional.impl.MetaParamsInternalMetadata;
import org.infinispan.marshall.protostream.impl.MarshallableObject;
import org.infinispan.marshall.protostream.impl.MarshallableUserObject;
import org.infinispan.metadata.Metadata;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * A form of {@link TransientMortalCacheValue} that is {@link Versioned}
 *
 * @author Manik Surtani
 * @since 5.1
 */
@ProtoTypeId(ProtoStreamTypeIds.METADATA_TRANSIENT_MORTAL_CACHE_VALUE)
public class MetadataTransientMortalCacheValue extends MetadataMortalCacheValue implements MetadataAware {

   long lastUsed;

   public MetadataTransientMortalCacheValue(Object value, Metadata metadata, long created, long lastUsed) {
      this(value, null, metadata, created, lastUsed);
   }

   protected MetadataTransientMortalCacheValue(Object value, MetaParamsInternalMetadata internalMetadata,
         Metadata metadata, long created, long lastUsed) {
      super(value, internalMetadata, metadata, created);
      this.lastUsed = lastUsed;
   }

   @ProtoFactory
   MetadataTransientMortalCacheValue(MarshallableUserObject<?> wrappedValue, MetaParamsInternalMetadata internalMetadata,
                                     MarshallableObject<Metadata> wrappedMetadata, long created, long lastUsed) {
      super(wrappedValue, internalMetadata, wrappedMetadata, created);
      this.lastUsed = lastUsed;
   }

   @Override
   @ProtoField(number = 5, defaultValue = "-1")
   public long getLastUsed() {
      return lastUsed;
   }

   @Override
   public InternalCacheEntry<?, ?> toInternalCacheEntry(Object key) {
      return new MetadataTransientMortalCacheEntry((MarshallableUserObject<?>) key, value, internalMetadata, metadata,
            lastUsed, created);
   }

   @Override
   public long getMaxIdle() {
      return getMetadata().maxIdle();
   }

   @Override
   public boolean isExpired(long now) {
      Metadata metadata = getMetadata();
      return ExpiryHelper.isExpiredTransientMortal(metadata.maxIdle(), lastUsed, metadata.lifespan(), created, now);
   }

   @Override
   public boolean isMaxIdleExpirable() {
      return true;
   }

   @Override
   public long getExpiryTime() {
      Metadata metadata = getMetadata();
      long lifespan = metadata.lifespan();
      long lset = lifespan > -1 ? created + lifespan : -1;
      long maxIdle = metadata.maxIdle();
      long muet = maxIdle > -1 ? lastUsed + maxIdle : -1;
      if (lset == -1) return muet;
      if (muet == -1) return lset;
      return min(lset, muet);
   }

   @Override
   protected void appendFieldsToString(StringBuilder builder) {
      super.appendFieldsToString(builder);
      builder.append(", lastUsed=").append(lastUsed);
   }
}
