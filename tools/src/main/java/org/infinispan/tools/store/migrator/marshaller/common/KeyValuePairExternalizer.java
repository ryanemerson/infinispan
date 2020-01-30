package org.infinispan.tools.store.migrator.marshaller.common;

import java.io.IOException;
import java.io.ObjectInput;
import java.util.Collections;
import java.util.Set;

import org.infinispan.marshall.core.Ids;
import org.infinispan.util.KeyValuePair;

public class KeyValuePairExternalizer extends AbstractMigratorExternalizer<KeyValuePair> {

   @Override
   public KeyValuePair readObject(ObjectInput input) throws IOException, ClassNotFoundException {
      return new KeyValuePair(input.readObject(), input.readObject());
   }

   @Override
   public Integer getId() {
      return Ids.KEY_VALUE_PAIR_ID;
   }

   @Override
   public Set<Class<? extends KeyValuePair>> getTypeClasses() {
      return Collections.singleton(KeyValuePair.class);
   }
}
