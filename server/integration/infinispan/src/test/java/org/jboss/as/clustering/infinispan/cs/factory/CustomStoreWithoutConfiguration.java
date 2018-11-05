package org.jboss.as.clustering.infinispan.cs.factory;

import org.infinispan.persistence.spi.MarshalledEntry;
import org.infinispan.persistence.spi.AdvancedCacheLoader;
import org.infinispan.persistence.spi.InitializationContext;

public class CustomStoreWithoutConfiguration implements AdvancedCacheLoader<Object,Object> {
   @Override
   public int size() {
      return 0;
   }

   @Override
   public void init(InitializationContext ctx) {
   }

   @Override
   public MarshalledEntry<Object, Object> load(Object key) {
      return null;
   }

   @Override
   public boolean contains(Object key) {
      return false;
   }

   @Override
   public void start() {
   }

   @Override
   public void stop() {
   }
}
