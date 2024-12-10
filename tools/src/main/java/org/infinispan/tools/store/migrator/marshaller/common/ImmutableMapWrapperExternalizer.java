package org.infinispan.tools.store.migrator.marshaller.common;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.infinispan.commons.marshall.MarshallUtil;
import org.infinispan.commons.util.Immutables;
import org.infinispan.commons.util.Util;

public class ImmutableMapWrapperExternalizer extends AbstractExternalizer<Map> {
   @Override
   public void writeObject(ObjectOutput output, Map map) throws IOException {
      MarshallUtil.marshallMap(map, output);
   }

   @Override
   public Map readObject(ObjectInput input) throws IOException, ClassNotFoundException {
      return Immutables.immutableMapWrap(MarshallUtil.unmarshallMap(input, HashMap::new));
   }

   @Override
   public Integer getId() {
      return Ids.IMMUTABLE_MAP;
   }

   @Override
   public Set<Class<? extends Map>> getTypeClasses() {
      return Util.asSet(Immutables.ImmutableMapWrapper.class);
   }
}
