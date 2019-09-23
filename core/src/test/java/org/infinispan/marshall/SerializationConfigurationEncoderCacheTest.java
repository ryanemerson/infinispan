package org.infinispan.marshall;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.Serializable;

import org.infinispan.Cache;
import org.infinispan.cache.impl.EncoderCache;
import org.infinispan.commons.marshall.JavaSerializationMarshaller;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.commons.marshall.WrappedByteArray;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.persistence.dummy.DummyInMemoryStore;
import org.infinispan.persistence.dummy.DummyInMemoryStoreConfigurationBuilder;
import org.infinispan.persistence.spi.MarshallableEntry;
import org.infinispan.test.SingleCacheManagerTest;
import org.infinispan.test.TestingUtil;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.testng.annotations.Test;

@Test(groups = "functional", testName = "marshall.SerializationConfigurationEncoderCacheTest")
public class SerializationConfigurationEncoderCacheTest extends SingleCacheManagerTest {

   @Override
   protected EmbeddedCacheManager createCacheManager() throws Exception {
      GlobalConfigurationBuilder globalBuilder = new GlobalConfigurationBuilder().nonClusteredDefault();
      globalBuilder.serialization().marshaller(new JavaSerializationMarshaller()).whiteList().addClasses(OnlySerializableObject.class);
      ConfigurationBuilder builder = new ConfigurationBuilder();
      builder.persistence().addStore(DummyInMemoryStoreConfigurationBuilder.class);
      return TestCacheManagerFactory.createCacheManager(globalBuilder, builder);
   }

   public void testCustomMarshallerEncodedCacheTest() throws Exception {
      Integer key = 1;
      Cache<Integer, OnlySerializableObject> cache = cache();
      assertTrue(cache instanceof EncoderCache);
      assertNull(cache.put(key, new OnlySerializableObject(10)));
      assertEquals(10, cache.get(key).value);

      DummyInMemoryStore dims = TestingUtil.getFirstWriter(cache);
      assertNull(dims.loadEntry(key));
      assertEquals(1, dims.keySet().size());
      assertTrue(dims.keySet().iterator().next() instanceof WrappedByteArray);

      Marshaller marshaller = new JavaSerializationMarshaller();
      byte[] keyBytes = marshaller.objectToByteBuffer(1);
      WrappedByteArray wrappedKey = new WrappedByteArray(keyBytes);
      MarshallableEntry entry = dims.loadEntry(wrappedKey);
      assertNotNull(entry);
      assertTrue(entry.getKey() instanceof WrappedByteArray);
      assertTrue(entry.getValue() instanceof WrappedByteArray);
   }

   static class OnlySerializableObject implements Serializable {
      int value;

      OnlySerializableObject() {}

      OnlySerializableObject(int value) {
         this.value = value;
      }
   }
}
