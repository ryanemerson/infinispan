package org.infinispan.container.entries.metadata;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.container.entries.ImmortalCacheEntry;
import org.infinispan.container.entries.InternalCacheValue;
import org.infinispan.functional.impl.MetaParamsInternalMetadata;
import org.infinispan.marshall.protostream.impl.MarshallableObject;
import org.infinispan.metadata.Metadata;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * A form of {@link org.infinispan.container.entries.ImmortalCacheEntry} that is {@link
 * org.infinispan.container.entries.metadata.MetadataAware}
 *
 * @author Galder Zamarreño
 * @since 5.3
 */
@ProtoTypeId(ProtoStreamTypeIds.METADATA_IMMORTAL_ENTRY)
public class MetadataImmortalCacheEntry extends ImmortalCacheEntry implements MetadataAware {

   @ProtoField(number = 4)
   protected MarshallableObject<Metadata> metadata;

   @ProtoFactory
   MetadataImmortalCacheEntry(MarshallableObject<?> wrappedKey, MarshallableObject<?> wrappedValue,
                              MetaParamsInternalMetadata internalMetadata, MarshallableObject<Metadata> metadata) {
      super(wrappedKey, wrappedValue, internalMetadata);
      this.metadata = metadata;
   }

   public MetadataImmortalCacheEntry(Object key, Object value, Metadata metadata) {
      super(key, value, null);
      setMetadata(metadata);
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
   public InternalCacheValue<?> toInternalCacheValue() {
      return new MetadataImmortalCacheValue(value, internalMetadata, metadata);
   }

   @Override
   protected void appendFieldsToString(StringBuilder builder) {
      super.appendFieldsToString(builder);
      builder.append(", metadata=").append(metadata);
   }
}
