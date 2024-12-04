package org.infinispan.marshall.protostream.impl.adapters.collections;

import java.util.Collections;
import java.util.List;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;

// TODO @ProtoTypeID
@ProtoAdapter(
      value = List.class,
      subClassNames = "java.util.Collections$EmptyList"
)
public class EmptyListAdapter {
   @ProtoFactory
   List<?> create() {
      return Collections.emptyList();
   }
}
