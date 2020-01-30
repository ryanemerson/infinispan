package org.infinispan.marshall.core;

import java.util.Set;

import org.infinispan.commons.marshall.AdvancedExternalizer;
import org.infinispan.commons.util.Immutables;
import org.infinispan.distribution.group.impl.CacheEntryGroupPredicate;
import org.infinispan.marshall.core.impl.ClassToExternalizerMap;
import org.infinispan.marshall.exts.EnumExternalizer;
import org.infinispan.marshall.exts.ThrowableExternalizer;
import org.infinispan.metadata.impl.InternalMetadataImpl;
import org.infinispan.reactive.publisher.impl.commands.batch.PublisherResponseExternalizer;
import org.infinispan.xsite.response.AutoStateTransferResponse;

final class InternalExternalizers {

   private InternalExternalizers() {
   }

   static ClassToExternalizerMap load() {
      // TODO Add initial value and load factor
      ClassToExternalizerMap exts = new ClassToExternalizerMap(512, 0.6f);

      // Add the rest of stateless externalizers
      addInternalExternalizer(new Immutables.ImmutableMapWrapperExternalizer(), exts);
      addInternalExternalizer(new Immutables.ImmutableSetWrapperExternalizer(), exts);
      addInternalExternalizer(new PublisherResponseExternalizer(), exts);
      addInternalExternalizer(ThrowableExternalizer.INSTANCE, exts);
      addInternalExternalizer(EnumExternalizer.INSTANCE, exts);
      addInternalExternalizer(new InternalMetadataImpl.Externalizer(), exts);
      addInternalExternalizer(AutoStateTransferResponse.EXTERNALIZER, exts);
      addInternalExternalizer(CacheEntryGroupPredicate.EXTERNALIZER, exts);

      return exts;
   }

   private static void addInternalExternalizer(
         AdvancedExternalizer ext, ClassToExternalizerMap exts) {
      Set<Class<?>> subTypes = ext.getTypeClasses();
      for (Class<?> subType : subTypes)
         exts.put(subType, ext);
   }

}
