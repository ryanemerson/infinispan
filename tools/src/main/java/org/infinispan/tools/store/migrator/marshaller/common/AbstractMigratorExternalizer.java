package org.infinispan.tools.store.migrator.marshaller.common;

import java.io.IOException;
import java.io.ObjectOutput;

import org.infinispan.commons.marshall.AdvancedExternalizer;

// TODO add constructor to take ID and classes
// TODO convert all store migrator externalizers to have no-op writeObject
// TODO remove unnecessary javadoc for externalizers I create
public abstract class AbstractMigratorExternalizer<T> implements AdvancedExternalizer<T> {
   @Override
   public void writeObject(ObjectOutput output, T object) throws IOException {
      throw new IllegalStateException(String.format("writeObject called on Externalizer %s", this.getClass().getSimpleName()));
   }
}
