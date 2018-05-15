package org.infinispan.persistence;


import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.persistence.async.AdvancedAsyncCacheWriter;
import org.infinispan.persistence.dummy.DummyInMemoryStore;
import org.infinispan.persistence.dummy.DummyInMemoryStoreConfigurationBuilder;
import org.infinispan.persistence.manager.PersistenceManager;
import org.infinispan.persistence.spi.StoreUnavailableException;
import org.infinispan.test.AbstractInfinispanTest;
import org.infinispan.test.Exceptions;
import org.infinispan.test.TestingUtil;
import org.infinispan.test.fwk.CleanupAfterMethod;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.testng.annotations.Test;

@CleanupAfterMethod
@Test(testName = "persistence.WriteBehindFaultToleranceTest", groups = "functional")
public class WriteBehindFaultToleranceTest extends AbstractInfinispanTest {

   private Cache<Object, Object> createManagerAndGetCache(boolean failSilently, int queueSize) {
      GlobalConfiguration globalConfiguration = new GlobalConfigurationBuilder().globalJmxStatistics().allowDuplicateDomains(true).build();
      ConfigurationBuilder cb = TestCacheManagerFactory.getDefaultCacheConfiguration(false);
      Configuration config = cb.persistence().availabilityInterval(10)
            .addStore(DummyInMemoryStoreConfigurationBuilder.class)
            .async().enable().modificationQueueSize(queueSize).failSilently(failSilently)
            .build();
      return new DefaultCacheManager(globalConfiguration, config).getCache();
   }

   @Test
   public void testBlockingOnStoreAvailabilityChange() {
      Cache<Object, Object> cache = createManagerAndGetCache(false, 1);
      AdvancedAsyncCacheWriter asyncWriter = TestingUtil.getFirstWriter(cache);
      DummyInMemoryStore store = (DummyInMemoryStore) TestingUtil.extractField(AdvancedAsyncCacheWriter.class, asyncWriter, "actual");
      store.setAvailable(true);
      cache.put(1, 1);
      eventuallyEquals(1, store::size);

      PersistenceManager pm = TestingUtil.extractComponent(cache, PersistenceManager.class);
      store.setAvailable(false);
      assertFalse(store.isAvailable());
      // PM & AsyncWriter should still be available as the async modification queue is not full
      eventually(asyncWriter::isAvailable);
      eventually(pm::isAvailable);

      // Ensure that the availability check has recognised that the delegate store is unavailable
      eventually(() -> !(boolean)TestingUtil.extractField(asyncWriter, "delegateAvailable"));

      // Add entries >= modification queue size so that store is no longer available
      cache.putAll(intMap(0, 10));
      assertEquals(1, store.size());

      // PM and writer should not be available as the async modification queue is now oversubscribed and the delegate is still unavailable
      eventually(() -> !asyncWriter.isAvailable());
      eventually(() -> !pm.isAvailable());
      Exceptions.expectException(StoreUnavailableException.class, () -> cache.putAll(intMap(10, 20)));
      assertEquals(1, store.size());

      // Make the delegate available and ensure that the initially queued modifications exist in the store
      store.setAvailable(true);
      assertTrue(store.isAvailable());
      eventually(asyncWriter::isAvailable);
      eventually(pm::isAvailable);
      eventuallyEquals(10, store::size);
      // Ensure that only the initial map entries are stored and that the second putAll operation truly failed
      assertFalse(store.contains(10));
   }

   private Map<Integer, Integer> intMap(int start, int end) {
      return IntStream.range(start, end).boxed().collect(Collectors.toMap(Function.identity(), Function.identity()));
   }

   @Test
   public void testWritesFailSilentlyWhenConfigured() {
      Cache<Object, Object> cache = createManagerAndGetCache(true, 1);
      AdvancedAsyncCacheWriter asyncWriter = TestingUtil.getFirstWriter(cache);
      DummyInMemoryStore store = (DummyInMemoryStore) TestingUtil.extractField(AdvancedAsyncCacheWriter.class, asyncWriter, "actual");
      store.setAvailable(true);
      eventually(store::isAvailable);
      cache.put(1, 1);
      eventuallyEquals(1, store::size);

      store.setAvailable(false);
      assertFalse(store.isAvailable());
      cache.put(1, 2); // Should fail on the store, but complete in-memory
      TestingUtil.sleepThread(1000); // Sleep to ensure async write is attempted
      store.setAvailable(true);
      eventually(store::isAvailable);
      assertEquals(1, store.load(1).getValue());
      assertEquals(2, cache.get(1));
   }
}
