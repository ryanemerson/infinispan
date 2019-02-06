package org.infinispan.tools.store.migrator.marshaller.externalizers.infinispan8;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.infinispan.commons.io.ByteBufferImpl;
import org.infinispan.commons.marshall.AdvancedExternalizer;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.commons.util.Immutables;
import org.infinispan.commons.util.Util;
import org.infinispan.container.entries.ImmortalCacheEntry;
import org.infinispan.container.entries.ImmortalCacheValue;
import org.infinispan.container.entries.MortalCacheEntry;
import org.infinispan.container.entries.MortalCacheValue;
import org.infinispan.container.entries.TransientCacheEntry;
import org.infinispan.container.entries.TransientCacheValue;
import org.infinispan.container.entries.TransientMortalCacheEntry;
import org.infinispan.container.entries.TransientMortalCacheValue;
import org.infinispan.container.entries.metadata.MetadataImmortalCacheEntry;
import org.infinispan.container.entries.metadata.MetadataImmortalCacheValue;
import org.infinispan.container.entries.metadata.MetadataMortalCacheEntry;
import org.infinispan.container.entries.metadata.MetadataMortalCacheValue;
import org.infinispan.container.entries.metadata.MetadataTransientCacheEntry;
import org.infinispan.container.entries.metadata.MetadataTransientCacheValue;
import org.infinispan.container.entries.metadata.MetadataTransientMortalCacheEntry;
import org.infinispan.container.entries.metadata.MetadataTransientMortalCacheValue;
import org.infinispan.container.versioning.NumericVersion;
import org.infinispan.container.versioning.SimpleClusteredVersion;
import org.infinispan.marshall.exts.EnumSetExternalizer;
import org.infinispan.marshall.exts.MapExternalizer;
import org.infinispan.marshall.persistence.impl.MarshalledEntryImpl;
import org.infinispan.metadata.EmbeddedMetadata;
import org.infinispan.tools.store.migrator.marshaller.AbstractExternalizerTable;
import org.infinispan.tools.store.migrator.marshaller.externalizers.common.InternalMetadataImplExternalizer;
import org.infinispan.util.KeyValuePair;

/**
 * Legacy Ids used in Infinispan 8 to map {@link org.infinispan.commons.marshall.Externalizer} implementations.
 * <p>
 * Indexes for object types. These are currently limited to being unsigned ints, so valid values are considered those in
 * the range of 0 to 254. Please note that the use of 255 is forbidden since this is reserved for foreign, or user
 * defined, externalizers.
 */
public class ExternalizerTable extends AbstractExternalizerTable {

   private static final Map<Integer, Integer> LEGACY_MAP;
   static {
      HashMap<Integer, Integer> map = new HashMap<>();
      map.put(2, 1); // MAPS
      map.put(10, 7); // IMMORTAL_ENTRY
      map.put(11, 8); // MORTAL_ENTRY
      map.put(12, 9); // TRANSIENT_ENTRY
      map.put(13, 10); // TRANSIENT_MORTAL_ENTRY
      map.put(14, 11); // IMMORTAL_VALUE
      map.put(15, 12); // MORTAL_VALUE
      map.put(16, 13); // TRANSIENT_VALUE
      map.put(17, 14); // TRANSIENT_VALUE
      map.put(19, 105); // IMMUTABLE_MAP
      map.put(76, 38); // METADATA_IMMORTAL_ENTRY
      map.put(77, 39); // METADATA_MORTAL_ENTRY
      map.put(78, 40); // METADATA_TRANSIENT_ENTRY
      map.put(79, 41); // METADATA_TRANSIENT_MORTAL_ENTRY
      map.put(80, 42); // METADATA_IMMORTAL_ENTRY
      map.put(81, 43); // METADATA_MORTAL_ENTRY
      map.put(82, 44); // METADATA_TRANSIENT_VALUE
      map.put(83, 45); // METADATA_TRANSIENT_MORTAL_VALUE
      map.put(96, 55); // SIMPLE_CLUSTERED_VERSION
      map.put(98, 57); // EMBEDDED_METADATA
      map.put(99, 58); // NUMERIC_VERSION
      map.put(103, 60); // KEY_VALUE_PAIR
      map.put(105, 62); // MARSHALLED_ENTRY
      map.put(106, 106); // BYTE_BUFFER
      map.put(121, 63); // ENUM_SET
      LEGACY_MAP = Collections.unmodifiableMap(map);
   }

   static int ARRAY_LIST = 0;
   static int JDK_SETS = 3;
   static int SINGLETON_LIST = 4;
   static int IMMUTABLE_LIST = 18;
   static int INTERNAL_METADATA = 104;
   static int LIST_ARRAY = 122;

   public ExternalizerTable(Map<Integer, ? extends AdvancedExternalizer<?>> userExternalizerMap) {
      super(userExternalizerMap);
   }

   @Override
   public void initInternalExternalizers(Marshaller marshaller) {
      Util.asSet(
            new ListExternalizer(),
            new MapExternalizer(),
            new SetExternalizer(),
            new EnumSetExternalizer(),
            new ArrayExternalizers.ListArray(),
            new SingletonListExternalizer(),

            new ImmutableListCopyExternalizer(),
            new Immutables.ImmutableMapWrapperExternalizer(),
            new ByteBufferImpl.Externalizer(),

            new NumericVersion.Externalizer(),
            new ByteBufferImpl.Externalizer(),
            new KeyValuePair.Externalizer(),
            new InternalMetadataImplExternalizer(INTERNAL_METADATA),
            new MarshalledEntryImpl.Externalizer(marshaller),

            new ImmortalCacheEntry.Externalizer(),
            new MortalCacheEntry.Externalizer(),
            new TransientCacheEntry.Externalizer(),
            new TransientMortalCacheEntry.Externalizer(),
            new ImmortalCacheValue.Externalizer(),
            new MortalCacheValue.Externalizer(),
            new TransientCacheValue.Externalizer(),
            new TransientMortalCacheValue.Externalizer(),

            new SimpleClusteredVersion.Externalizer(),
            new MetadataImmortalCacheEntry.Externalizer(),
            new MetadataMortalCacheEntry.Externalizer(),
            new MetadataTransientCacheEntry.Externalizer(),
            new MetadataTransientMortalCacheEntry.Externalizer(),
            new MetadataImmortalCacheValue.Externalizer(),
            new MetadataMortalCacheValue.Externalizer(),
            new MetadataTransientCacheValue.Externalizer(),
            new MetadataTransientMortalCacheValue.Externalizer(),

            new EmbeddedMetadata.Externalizer()
      ).forEach(this::addInternalExternalizer);
   }

   @Override
   protected int transformId(int id) {
      Integer legacyId = LEGACY_MAP.get(id);
      return legacyId == null ? id : legacyId;
   }
}
