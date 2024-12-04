package org.infinispan.marshall.protostream.impl.adapters.collections;

import java.util.Collections;
import java.util.List;

import org.infinispan.marshall.protostream.impl.MarshallableObject;
import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

// TODO @ProtoTypeID
@ProtoAdapter(
      value = List.class,
      subClassNames = "java.util.Collections$SingletonList"
)
public class SingletonListAdapter {
   @ProtoFactory
   List<?> create(MarshallableObject<?> element) {
      return Collections.singletonList(element);
   }

   @ProtoField(1)
   MarshallableObject<?> getElement(List<?> list) {
      return MarshallableObject.create(list.get(0));
   }
}
