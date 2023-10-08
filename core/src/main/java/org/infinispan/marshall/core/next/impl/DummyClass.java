package org.infinispan.marshall.core.next.impl;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

// TODO remove
public class DummyClass {
   @ProtoField(number = 1, defaultValue = "0")
   int field;

   @ProtoFactory
   public DummyClass(int field) {
      this.field = field;
   }
}
