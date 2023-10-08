package org.infinispan.tools.store.migrator.marshaller.common;

import java.io.IOException;
import java.io.ObjectInput;
import java.util.Collections;
import java.util.Set;

import org.infinispan.container.versioning.NumericVersion;
import org.infinispan.marshall.core.Ids;

public class NumericVersionExternalizer extends AbstractMigratorExternalizer<NumericVersion> {
   @Override
   public Set<Class<? extends NumericVersion>> getTypeClasses() {
      return Collections.singleton(NumericVersion.class);
   }

   @Override
   public NumericVersion readObject(ObjectInput input) throws IOException {
      return new NumericVersion(input.readLong());
   }

   @Override
   public Integer getId() {
      return Ids.NUMERIC_VERSION;
   }
}
