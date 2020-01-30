package org.infinispan.tools.store.migrator.marshaller.common;

import java.io.IOException;
import java.io.ObjectInput;
import java.util.Set;

import org.infinispan.commons.util.Util;
import org.infinispan.container.entries.ImmortalCacheEntry;
import org.infinispan.marshall.core.Ids;

/**
 * Externalizer for {@link ImmortalCacheEntry}.
 *
 * @author Pedro Ruivo
 * @since 11.0
 */
public class ImmortalCacheEntryExternalizer extends AbstractMigratorExternalizer<ImmortalCacheEntry> {

   @Override
   public ImmortalCacheEntry readObject(ObjectInput input) throws IOException, ClassNotFoundException {
      Object k = input.readObject();
      Object v = input.readObject();
      return new ImmortalCacheEntry(k, v);
   }

   @Override
   public Integer getId() {
      return Ids.IMMORTAL_ENTRY;
   }

   @Override
   public Set<Class<? extends ImmortalCacheEntry>> getTypeClasses() {
      return Util.asSet(ImmortalCacheEntry.class);
   }
}
