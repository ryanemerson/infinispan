package org.infinispan.container.entries.metadata;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.container.entries.AbstractInternalCacheEntry;
import org.infinispan.container.entries.ExpiryHelper;
import org.infinispan.container.entries.InternalCacheValue;
import org.infinispan.functional.impl.MetaParamsInternalMetadata;
import org.infinispan.marshall.protostream.impl.MarshallableObject;
import org.infinispan.marshall.protostream.impl.MarshallableUserObject;
import org.infinispan.metadata.Metadata;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * A cache entry that is mortal and is {@link MetadataAware}
 *
 * @author Galder Zamarreño
 * @since 5.3
 */
@ProtoTypeId(ProtoStreamTypeIds.METADATA_MORTAL_ENTRY)
public class MetadataMortalCacheEntry extends AbstractInternalCacheEntry implements MetadataAware {

   protected MarshallableObject<Metadata> metadata;
   protected long created;

   public MetadataMortalCacheEntry(Object key, Object value, Metadata metadata, long created) {
      this(key, value, null, metadata, created);
   }

   protected MetadataMortalCacheEntry(Object key, Object value, MetaParamsInternalMetadata internalMetadata,
                                      Metadata metadata, long created) {
      super(key, value, internalMetadata);
      this.setMetadata(metadata);
      this.created = created;
   }

   @ProtoFactory
   MetadataMortalCacheEntry(MarshallableUserObject<?> wrappedKey, MarshallableUserObject<?> wrappedValue,
                            MetaParamsInternalMetadata internalMetadata, MarshallableObject<Metadata> wrappedMetadata,
                            long created) {
      super(wrappedKey, wrappedValue, internalMetadata);
      this.metadata = wrappedMetadata;
      this.created = created;
   }

   @ProtoField(number = 4, name ="metadata")
   public MarshallableObject<Metadata> getWrappedMetadata() {
      return metadata;
   }

   @Override
   @ProtoField(number = 5, defaultValue = "-1")
   public final long getCreated() {
      return created;
   }

   @Override
   public final boolean isExpired(long now) {
      return ExpiryHelper.isExpiredMortal(getMetadata().lifespan(), created, now);
   }

   @Override
   public final boolean canExpire() {
      return true;
   }

   @Override
   public final long getLastUsed() {
      return -1;
   }

   @Override
   public final long getLifespan() {
      return getMetadata().lifespan();
   }

   @Override
   public final long getMaxIdle() {
      return -1;
   }

   @Override
   public final long getExpiryTime() {
      long lifespan = getMetadata().lifespan();
      return lifespan > -1 ? created + lifespan : -1;
   }

   @Override
   public final void touch(long currentTimeMillis) {
      // no-op
   }

   @Override
   public void reincarnate(long now) {
      this.created = now;
   }

   @Override
   public InternalCacheValue<?> toInternalCacheValue() {
      return new MetadataMortalCacheValue(value, internalMetadata, metadata, created);
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
   }
}
