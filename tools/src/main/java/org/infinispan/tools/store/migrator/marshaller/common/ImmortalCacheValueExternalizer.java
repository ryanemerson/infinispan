package org.infinispan.tools.store.migrator.marshaller.common;

import java.io.IOException;
import java.io.ObjectInput;
import java.util.Set;

import org.infinispan.commons.util.Util;
import org.infinispan.container.entries.ImmortalCacheValue;
import org.infinispan.marshall.core.Ids;

/**
 * Externalizer for {@link ImmortalCacheValue}.
 *
 * @author Pedro Ruivo
 * @since 11.0
 */
public class ImmortalCacheValueExternalizer extends AbstractMigratorExternalizer<ImmortalCacheValue> {

   @Override
   public ImmortalCacheValue readObject(ObjectInput input) throws IOException, ClassNotFoundException {
      Object v = input.readObject();
      return new ImmortalCacheValue(v, null);
   }

   @Override
   public Integer getId() {
      return Ids.IMMORTAL_VALUE;
   }

   @Override
   public Set<Class<? extends ImmortalCacheValue>> getTypeClasses() {
      return Util.asSet(ImmortalCacheValue.class);
   }
}
