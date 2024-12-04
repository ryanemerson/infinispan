package org.infinispan.marshall.protostream.impl.adapters.collections;

import java.util.Collections;
import java.util.Set;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;

// TODO @ProtoTypeID
@ProtoAdapter(
      value = Set.class,
      subClassNames = "java.util.Collections$EmptySet"
)
public class EmptySetAdapter {
   @ProtoFactory
   Set<?> create() {
      return Collections.emptySet();
   }
}
