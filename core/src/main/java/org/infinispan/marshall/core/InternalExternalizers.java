package org.infinispan.marshall.core;

import java.util.Set;

import org.infinispan.commons.marshall.AdvancedExternalizer;
import org.infinispan.commons.util.Immutables;
import org.infinispan.marshall.core.impl.ClassToExternalizerMap;
import org.infinispan.marshall.exts.EnumExternalizer;
import org.infinispan.marshall.exts.ThrowableExternalizer;

final class InternalExternalizers {

   private InternalExternalizers() {
   }

   static ClassToExternalizerMap load() {
      // TODO Add initial value and load factor
      ClassToExternalizerMap exts = new ClassToExternalizerMap(512, 0.6f);

      // Add the rest of stateless externalizers
      addInternalExternalizer(new Immutables.ImmutableMapWrapperExternalizer(), exts);
      addInternalExternalizer(new Immutables.ImmutableSetWrapperExternalizer(), exts);
      addInternalExternalizer(ThrowableExternalizer.INSTANCE, exts);
      addInternalExternalizer(EnumExternalizer.INSTANCE, exts);

      return exts;
   }

   private static void addInternalExternalizer(
         AdvancedExternalizer ext, ClassToExternalizerMap exts) {
      Set<Class<?>> subTypes = ext.getTypeClasses();
      for (Class<?> subType : subTypes)
         exts.put(subType, ext);
   }

}
