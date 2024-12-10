package org.infinispan.tools.store.migrator.marshaller.infinispan8;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.infinispan.tools.store.migrator.marshaller.common.AbstractExternalizer;
import org.infinispan.commons.marshall.MarshallUtil;
import org.infinispan.commons.util.Util;

import net.jcip.annotations.Immutable;

/**
 * List externalizer dealing with ArrayList and LinkedList implementations.
 *
 * @author Galder Zamarreño
 * @since 4.0
 */
@Immutable
class ListExternalizer extends AbstractExternalizer<List> {

   private static final int ARRAY_LIST = 0;
   private static final int LINKED_LIST = 1;

   private final Map<Class<?>, Integer> numbers = new HashMap<>(2);

   public ListExternalizer() {
      numbers.put(ArrayList.class, ARRAY_LIST);
      numbers.put(getPrivateArrayListClass(), ARRAY_LIST);
      numbers.put(LinkedList.class, LINKED_LIST);
   }

   @Override
   public void writeObject(ObjectOutput output, List list) throws IOException {
      int number = numbers.getOrDefault(list.getClass(), -1);
      output.writeByte(number);
      MarshallUtil.marshallCollection(list, output);
   }

   @Override
   public List readObject(ObjectInput input) throws IOException, ClassNotFoundException {
      int magicNumber = input.readUnsignedByte();
      switch (magicNumber) {
         case ARRAY_LIST:
            return MarshallUtil.unmarshallCollection(input, ArrayList::new);
         case LINKED_LIST:
            return MarshallUtil.unmarshallCollection(input, s -> new LinkedList<>());
         default:
            throw new IllegalStateException("Unknown List type: " + magicNumber);
      }
   }

   @Override
   public Integer getId() {
      return ExternalizerTable.ARRAY_LIST;
   }

   @Override
   public Set<Class<? extends List>> getTypeClasses() {
      return Util.asSet(ArrayList.class, LinkedList.class,
            getPrivateArrayListClass());
   }

   private Class<List> getPrivateArrayListClass() {
      return Util.<List>loadClass("java.util.Arrays$ArrayList", List.class.getClassLoader());
   }
}
