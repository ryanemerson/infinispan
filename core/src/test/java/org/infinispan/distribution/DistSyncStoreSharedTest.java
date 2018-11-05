package org.infinispan.distribution;

import static org.infinispan.test.TestingUtil.extractGlobalMarshaller;
import static org.testng.AssertJUnit.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.infinispan.Cache;
import org.infinispan.commands.write.ClearCommand;
import org.infinispan.commands.write.PutKeyValueCommand;
import org.infinispan.commands.write.RemoveCommand;
import org.infinispan.commands.write.ReplaceCommand;
import org.infinispan.persistence.spi.MarshalledEntry;
import org.infinispan.marshall.persistence.impl.MarshalledEntryImpl;
import org.infinispan.persistence.spi.CacheLoader;
import org.infinispan.persistence.spi.CacheWriter;
import org.infinispan.persistence.spi.PersistenceException;
import org.infinispan.test.TestingUtil;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

/**
 * DistSyncStoreSharedTest.
 *
 * @author Galder Zamarreño
 * @since 4.0
 */
@Test(groups = "functional", testName = "distribution.DistSyncStoreSharedTest")
public class DistSyncStoreSharedTest<D extends DistSyncStoreSharedTest> extends BaseDistStoreTest<Object, String, D> {

   public DistSyncStoreSharedTest() {
      testRetVals = true;
      shared = true;
   }

   @AfterMethod
   public void clearStats() {
      for (Cache<?, ?> c: caches) {
         log.trace("Clearing stats for cache store on cache "+ c);
         clearStats(c);
      }
   }

   @Override
   public Object[] factory() {
      return new Object[] {
            new DistSyncStoreSharedTest().segmented(true),
            new DistSyncStoreSharedTest().segmented(false),
      };
   }

   public void testPutFromNonOwner() throws Exception {
      String key = "k4", value = "value4";
      for (Cache<Object, String> c : caches) assert c.isEmpty();
      Cache<Object, String> nonOwner = getFirstNonOwner(key);
      CacheLoader nonOwnerStore = TestingUtil.getFirstLoader(nonOwner);
      assert !nonOwnerStore.contains(key);
      Object retval = nonOwner.put(key, value);
      asyncWait(key, PutKeyValueCommand.class);

      Cache[] owners = getOwners(key);
      CacheLoader store = TestingUtil.getFirstLoader(owners[0]);
      assertIsInContainerImmortal(owners[0], key);
      assert store.contains(key);

      for (int i = 1; i < owners.length; i++) {
         store = TestingUtil.getFirstLoader(owners[i]);
         assertIsInContainerImmortal(owners[i], key);
         assert store.contains(key);
      }

      for (Cache<Object, String> c : caches) {
         store = TestingUtil.getFirstLoader(c);
         assert store.contains(key);
         assertNumberOfInvocations(store, "write", 1);
      }

      if (testRetVals) assert retval == null;
      assertOnAllCachesAndOwnership(key, value);
   }

   public void testPutFromOwner() throws Exception {
      String key = "k5", value = "value5";
      for (Cache<Object, String> c : caches) assert c.isEmpty();
      Cache[] owners = getOwners(key);
      Object retval = owners[0].put(key, value);
      asyncWait(key, PutKeyValueCommand.class);
      CacheLoader store = TestingUtil.getFirstLoader(owners[0]);
      assertIsInContainerImmortal(owners[0], key);
      assert store.contains(key);

      for (int i = 1; i < owners.length; i++) {
         store = TestingUtil.getFirstLoader(owners[i]);
         assertIsInContainerImmortal(owners[i], key);
         assert store.contains(key);
      }

      for (Cache<Object, String> c : caches) {
         store = TestingUtil.getFirstLoader(c);
         if (isOwner(c, key)) {
            assertIsInContainerImmortal(c, key);
         }
         assert store.contains(key);
         assertNumberOfInvocations(store, "write", 1);
      }

      if (testRetVals) assert retval == null;
      assertOnAllCachesAndOwnership(key, value);
   }

   public void testPutAll() throws Exception {
      log.trace("Here it begins");
      String k1 = "1", v1 = "one", k2 = "2", v2 = "two", k3 = "3", v3 = "three", k4 = "4", v4 = "four";
      String[] keys = new String[]{k1, k2, k3, k4};
      Map<String, String> data = new HashMap<String, String>();
      data.put(k1, v1);
      data.put(k2, v2);
      data.put(k3, v3);
      data.put(k4, v4);

      c1.putAll(data);
      for (String key : keys) {
         for (Cache<Object, String> c : caches) {
            CacheLoader store = TestingUtil.getFirstLoader(c);
            if (isFirstOwner(c, key)) {
               assertIsInContainerImmortal(c, key);
            }
            log.debug("Testing " + c);
            assertNumberOfInvocations(store, "write", 4);
            assert store.contains(key);
         }
      }
   }

   public void testRemoveFromNonOwner() throws Exception {
      String key = "k1", value = "value";
      initAndTest();

      for (Cache<Object, String> c : caches) {
         CacheLoader store = TestingUtil.getFirstLoader(c);
         if (isFirstOwner(c, key)) {
            assertIsInContainerImmortal(c, key);
            assert store.load(key).getValue().equals(value);
         }
      }

      Object retval = getFirstNonOwner(key).remove(key);
      asyncWait("k1", RemoveCommand.class);
      if (testRetVals) assert value.equals(retval);
      for (Cache<Object, String> c : caches) {
         CacheLoader store = TestingUtil.getFirstLoader(c);
         MarshalledEntry me = store.load(key);
         if (me == null) {
            assertNumberOfInvocations(store, "delete", 1);
            assertNumberOfInvocations(store, "write", 1);
         } else {
            assertNumberOfInvocations(store, "write", 2);
         }
      }
   }

   public void testReplaceFromNonOwner() throws Exception {
      String key = "k1", value = "value", value2 = "v2";
      initAndTest();

      for (Cache<Object, String> c : caches) {
         CacheLoader store = TestingUtil.getFirstLoader(c);
         if (isFirstOwner(c, key)) {
            assertIsInContainerImmortal(c, key);
            assert store.load(key).getValue().equals(value);
         }
      }

      Object retval = getFirstNonOwner(key).replace(key, value2);
      asyncWait(key, ReplaceCommand.class);
      if (testRetVals) assert value.equals(retval);
      for (Cache<Object, String> c : caches) {
         CacheLoader store = TestingUtil.getFirstLoader(c);
         if (isFirstOwner(c, key)) {
            assertIsInContainerImmortal(c, key);
         }
         assert store.load(key).getValue().equals(value2);
         assertNumberOfInvocations(store, "write", 2);
      }
   }

   public void testClear() throws Exception {
      for (Cache<Object, String> c : caches) assert c.isEmpty();
      for (int i = 0; i < 5; i++) {
         getOwners("k" + i)[0].put("k" + i, "value" + i);
         asyncWait("k" + i, PutKeyValueCommand.class);
      }
      // this will fill up L1 as well
      for (int i = 0; i < 5; i++) assertOnAllCachesAndOwnership("k" + i, "value" + i);
      for (Cache<Object, String> c : caches) assert !c.isEmpty();
      c1.clear();
      asyncWait(null, ClearCommand.class);
      for (Cache<Object, String> c : caches) assert c.isEmpty();

      /* We only check c1 because on a shared situation, no matter where the clear is called,
       * it should clear the whole store regardless. Bear in mind that in the test, even though
       * the cache store is shared, each cache has each own cache store, that allows for checking
       * who execute puts, removes...etc. */
      CacheLoader store = TestingUtil.getFirstLoader(c1);
      assertNumberOfInvocations(store, "clear", calculateTotalSegmentsForAllNodes());
      for (int i = 0; i < 5; i++) {
         String key = "k" + i;
         assert !store.contains(key);
      }
   }

   /**
    * @return how many segments there are across all nodes or 1 if the config is not segmented (all shared)
    */
   protected int calculateTotalSegmentsForAllNodes() {
      return segmented ? c1.getCacheConfiguration().clustering().hash().numSegments() : 1;
   }

   public void testGetOnlyQueriesCacheOnOwners() throws PersistenceException {
      // Make a key that own'ers is c1 and c2
      final MagicKey k = getMagicKey();
      final String v1 = "real-data";
      final String v2 = "stale-data";

      // Simulate c3 was by itself and someone wrote a value that is now stale
      CacheWriter store = (CacheWriter) TestingUtil.getFirstLoader(c3);
      store.write(new MarshalledEntryImpl(k, v2, null, extractGlobalMarshaller(c3.getCacheManager())));

      c1.put(k, v1);

      assertEquals(v1, c3.get(k));
   }
}
