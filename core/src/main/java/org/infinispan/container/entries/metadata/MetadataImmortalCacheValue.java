package org.infinispan.container.entries.metadata;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.container.entries.ImmortalCacheValue;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.functional.impl.MetaParamsInternalMetadata;
import org.infinispan.marshall.protostream.impl.MarshallableObject;
import org.infinispan.metadata.Metadata;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * A form of {@link ImmortalCacheValue} that is {@link MetadataAware}.
 *
 * @author Galder Zamarreño
 * @since 5.3
 */
@ProtoTypeId(ProtoStreamTypeIds.METADATA_IMMORTAL_VALUE)
public class MetadataImmortalCacheValue extends ImmortalCacheValue implements MetadataAware {

   @ProtoField(number = 3)
   MarshallableObject<Metadata> metadata;

   @ProtoFactory
   MetadataImmortalCacheValue(MarshallableObject<?> wrappedValue, MetaParamsInternalMetadata internalMetadata,
                              MarshallableObject<Metadata> metadata) {
      super(wrappedValue, internalMetadata);
      this.metadata = metadata;
   }

   public MetadataImmortalCacheValue(Object value, Metadata metadata) {
      this(value, null, metadata);
   }

   protected MetadataImmortalCacheValue(Object value, MetaParamsInternalMetadata internalMetadata, Metadata metadata) {
      super(value, internalMetadata);
      setMetadata(metadata);
   }

   @Override
   public InternalCacheEntry<?, ?> toInternalCacheEntry(Object key) {
      return new MetadataImmortalCacheEntry(MarshallableObject.create(key), value, internalMetadata, metadata);
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
   }
}
