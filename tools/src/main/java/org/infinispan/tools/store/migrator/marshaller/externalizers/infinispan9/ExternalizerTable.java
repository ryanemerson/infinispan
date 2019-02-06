package org.infinispan.tools.store.migrator.marshaller.externalizers.infinispan9;

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
import org.infinispan.marshall.exts.CollectionExternalizer;
import org.infinispan.marshall.exts.EnumSetExternalizer;
import org.infinispan.marshall.exts.MapExternalizer;
import org.infinispan.marshall.persistence.impl.MarshalledEntryImpl;
import org.infinispan.metadata.EmbeddedMetadata;
import org.infinispan.tools.store.migrator.marshaller.AbstractExternalizerTable;
import org.infinispan.tools.store.migrator.marshaller.externalizers.common.InternalMetadataImplExternalizer;
import org.infinispan.util.KeyValuePair;

/**
 * Legacy externalizers for Infinispan 9.
 *
 * @author Ryan Emerson
 * @since 10.0
 */
public class ExternalizerTable extends AbstractExternalizerTable {

   private static final int INTERNAL_METADATA = 61;

   public ExternalizerTable(Map<Integer, ? extends AdvancedExternalizer<?>> userExternalizerMap) {
      super(userExternalizerMap);
   }

   @Override
   public void initInternalExternalizers(Marshaller marshaller) {
      Util.asSet(
            new CollectionExternalizer(),
            new MapExternalizer(),
            new EnumSetExternalizer(),
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
}
