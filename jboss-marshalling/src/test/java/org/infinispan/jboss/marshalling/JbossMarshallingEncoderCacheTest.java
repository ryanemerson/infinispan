package org.infinispan.jboss.marshalling;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;

import org.infinispan.Cache;
import org.infinispan.cache.impl.EncoderCache;
import org.infinispan.commons.marshall.AbstractExternalizer;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.commons.marshall.WrappedByteArray;
import org.infinispan.commons.util.Util;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.jboss.marshalling.core.JBossUserMarshaller;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.persistence.dummy.DummyInMemoryStore;
import org.infinispan.persistence.dummy.DummyInMemoryStoreConfigurationBuilder;
import org.infinispan.persistence.spi.MarshallableEntry;
import org.infinispan.test.SingleCacheManagerTest;
import org.infinispan.test.TestingUtil;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.testng.annotations.Test;

@Test(groups = "functional", testName = "marshall.JbossMarshallingEncoderCacheTest")
public class JbossMarshallingEncoderCacheTest extends SingleCacheManagerTest {

   @Override
   protected EmbeddedCacheManager createCacheManager() throws Exception {
      GlobalConfigurationBuilder globalBuilder = new GlobalConfigurationBuilder().nonClusteredDefault();
      globalBuilder.serialization().addAdvancedExternalizer(Externalizer.USER_EXT_ID_MIN, new Externalizer());
      ConfigurationBuilder builder = new ConfigurationBuilder();
      builder.persistence().addStore(DummyInMemoryStoreConfigurationBuilder.class);
      return TestCacheManagerFactory.createCacheManager(globalBuilder, builder);
   }

   public void testJbossMarshallingEncodedCacheTest() throws Exception {
      Integer key = 1;
      Cache<Integer, ExternalizerObject> cache = cache();
      assertTrue(cache instanceof EncoderCache);
      assertNull(cache.put(key, new ExternalizerObject(10)));
      assertEquals(10, cache.get(key).value);

      DummyInMemoryStore dims = TestingUtil.getFirstWriter(cache);
      assertNull(dims.loadEntry(key));
      assertEquals(1, dims.keySet().size());
      assertTrue(dims.keySet().iterator().next() instanceof WrappedByteArray);

      Marshaller marshaller = new JBossUserMarshaller(cache.getAdvancedCache().getCacheManager().getGlobalComponentRegistry());
      byte[] keyBytes = marshaller.objectToByteBuffer(1);
      WrappedByteArray wrappedKey = new WrappedByteArray(keyBytes);
      MarshallableEntry entry = dims.loadEntry(wrappedKey);
      assertNotNull(entry);
      assertTrue(entry.getKey() instanceof WrappedByteArray);
      assertTrue(entry.getValue() instanceof WrappedByteArray);
   }

   static class ExternalizerObject {
      int value;

      ExternalizerObject(int value) {
         this.value = value;
      }
   }

   static class Externalizer extends AbstractExternalizer<ExternalizerObject> {
      @Override
      public Set<Class<? extends ExternalizerObject>> getTypeClasses() {
         return Util.asSet(ExternalizerObject.class);
      }

      @Override
      public void writeObject(ObjectOutput output, ExternalizerObject object) throws IOException {
         output.writeInt(object.value);
      }

      @Override
      public ExternalizerObject readObject(ObjectInput input) throws IOException, ClassNotFoundException {
         return new ExternalizerObject(input.readInt());
      }
   }
}
