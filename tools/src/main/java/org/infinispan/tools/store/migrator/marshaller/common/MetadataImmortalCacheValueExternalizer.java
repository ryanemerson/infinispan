package org.infinispan.tools.store.migrator.marshaller.common;

import java.io.IOException;
import java.io.ObjectInput;
import java.util.Set;

import org.infinispan.commons.util.Util;
import org.infinispan.container.entries.metadata.MetadataImmortalCacheValue;
import org.infinispan.marshall.core.Ids;
import org.infinispan.metadata.Metadata;

/**
 * Externalizer for {@link MetadataImmortalCacheValue}.
 *
 * @author Pedro Ruivo
 * @since 11.0
 */
public class MetadataImmortalCacheValueExternalizer extends AbstractMigratorExternalizer<MetadataImmortalCacheValue> {

   @Override
   public MetadataImmortalCacheValue readObject(ObjectInput input) throws IOException, ClassNotFoundException {
      Object v = input.readObject();
      Metadata metadata = (Metadata) input.readObject();
      return new MetadataImmortalCacheValue(v, null, metadata);
   }

   @Override
   public Integer getId() {
      return Ids.METADATA_IMMORTAL_VALUE;
   }

   @Override
   public Set<Class<? extends MetadataImmortalCacheValue>> getTypeClasses() {
      return Util.asSet(MetadataImmortalCacheValue.class);
   }
}
