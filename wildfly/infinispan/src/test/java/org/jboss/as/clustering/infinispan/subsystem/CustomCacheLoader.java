package org.jboss.as.clustering.infinispan.subsystem;

import java.util.function.Predicate;

import org.infinispan.persistence.spi.AdvancedCacheLoader;
import org.infinispan.persistence.spi.InitializationContext;
import org.infinispan.persistence.spi.MarshallableEntry;
import org.reactivestreams.Publisher;

public class CustomCacheLoader implements AdvancedCacheLoader {
   @Override
   public int size() {
      return 0;
   }

   @Override
   public void init(InitializationContext initializationContext) {
   }

   @Override
   public MarshallableEntry loadEntry(Object o) {
      return null;
   }

   @Override
   public Publisher<MarshallableEntry> entryPublisher(Predicate filter, boolean fetchValue, boolean fetchMetadata) {
      return null;
   }

   @Override
   public boolean contains(Object o) {
      return false;
   }

   @Override
   public void start() {
   }

   @Override
   public void stop() {
   }
}
