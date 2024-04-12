package org.infinispan.commons.marshall.protostream.adapters;

import java.util.concurrent.atomic.AtomicIntegerArray;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.protostream.containers.IndexedElementContainerAdapter;

@ProtoTypeId(ProtoStreamTypeIds.ATOMIC_INTEGER_ARRAY)
@ProtoAdapter(AtomicIntegerArray.class)
public class AtomicIntegerArrayAdapter implements IndexedElementContainerAdapter<AtomicIntegerArray, Integer> {

   @ProtoFactory
   public AtomicIntegerArray create(int size) {
      return new AtomicIntegerArray(size);
   }

   @Override
   public Integer getElement(AtomicIntegerArray container, int index) {
      return container.get(index);
   }

   @Override
   public void setElement(AtomicIntegerArray container, int index, Integer element) {
      container.set(index, element);
   }

   @Override
   public int getNumElements(AtomicIntegerArray container) {
      return container.length();
   }
}
