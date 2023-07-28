//package org.infinispan.lock;
//
//import static java.util.Arrays.asList;
//import static org.infinispan.commons.test.Exceptions.expectCompletionException;
//import static org.infinispan.functional.FunctionalTestUtils.await;
//import static org.testng.AssertJUnit.assertFalse;
//import static org.testng.AssertJUnit.assertNotNull;
//import static org.testng.AssertJUnit.assertTrue;
//
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.TimeUnit;
//
//import org.infinispan.lock.api.ClusteredLock;
//import org.infinispan.lock.api.ClusteredLockManager;
//import org.infinispan.lock.exception.ClusteredLockException;
//import org.infinispan.manager.EmbeddedCacheManager;
//import org.infinispan.partitionhandling.AvailabilityException;
//import org.junit.jupiter.api.Test;
//
//@Test(groups = "functional", testName = "clusteredLock.ConsistentReliabilitySplitBrainTest")
//public class ConsistentReliabilitySplitBrainTest extends BaseClusteredLockSplitBrainTest {
//
//   @Override
//   protected String getLockName() {
//      return "ConsistentReliabilitySplitBrainTest";
//   }
//
//   @Test
//   public void testLockCreationWhenPartitionHappening() {
//      ClusteredLockManager clusteredLockManager = EmbeddedClusteredLockManagerFactory.from(getCacheManagers().get(0));
//      await(clusteredLockManager.remove(getLockName()));
//
//      splitCluster(new int[]{0, 1, 2}, new int[]{3, 4, 5});
//
//      for (EmbeddedCacheManager cm : getCacheManagers()) {
//         ClusteredLockManager clm = EmbeddedClusteredLockManagerFactory.from(cm);
//         eventually(() -> availabilityExceptionRaised(clm));
//      }
//   }
//
//   @Test
//   public void testLockUseAfterPartitionWithoutMajority() {
//
//      ClusteredLockManager clm0 = EmbeddedClusteredLockManagerFactory.from(getCacheManagers().get(0));
//      ClusteredLockManager clm1 = EmbeddedClusteredLockManagerFactory.from(getCacheManagers().get(1));
//      ClusteredLockManager clm2 = EmbeddedClusteredLockManagerFactory.from(getCacheManagers().get(2));
//      ClusteredLockManager clm3 = EmbeddedClusteredLockManagerFactory.from(getCacheManagers().get(3));
//      ClusteredLockManager clm4 = EmbeddedClusteredLockManagerFactory.from(getCacheManagers().get(4));
//      ClusteredLockManager clm5 = EmbeddedClusteredLockManagerFactory.from(getCacheManagers().get(5));
//
//      clm0.defineLock(getLockName());
//      assertTrue(clm0.isDefined(getLockName()));
//
//      ClusteredLock lock0 = clm0.get(getLockName());
//      ClusteredLock lock1 = clm1.get(getLockName());
//      ClusteredLock lock2 = clm2.get(getLockName());
//      ClusteredLock lock3 = clm3.get(getLockName());
//      ClusteredLock lock4 = clm4.get(getLockName());
//      ClusteredLock lock5 = clm5.get(getLockName());
//
//      splitCluster(new int[]{0, 1, 2}, new int[]{3, 4, 5});
//
//      // Wait for degraded topologies to work around ISPN-9008
//      partition(0).assertDegradedMode();
//      partition(1).assertDegradedMode();
//
//      asList(lock0, lock1, lock2, lock3, lock4, lock5).forEach(lock -> {
//         assertNotNull(lock);
//         expectCompletionException(ClusteredLockException.class, AvailabilityException.class, lock.tryLock());
//      });
//   }
//
//   @Test
//   public void testLockUseAfterPartitionWithMajority() {
//
//      ClusteredLockManager clm0 = EmbeddedClusteredLockManagerFactory.from(getCacheManagers().get(0));
//      ClusteredLockManager clm1 = EmbeddedClusteredLockManagerFactory.from(getCacheManagers().get(1));
//      ClusteredLockManager clm2 = EmbeddedClusteredLockManagerFactory.from(getCacheManagers().get(2));
//      ClusteredLockManager clm3 = EmbeddedClusteredLockManagerFactory.from(getCacheManagers().get(3));
//      ClusteredLockManager clm4 = EmbeddedClusteredLockManagerFactory.from(getCacheManagers().get(4));
//      ClusteredLockManager clm5 = EmbeddedClusteredLockManagerFactory.from(getCacheManagers().get(5));
//
//      assertTrue(clm0.defineLock(getLockName()));
//      assertFalse(clm1.defineLock(getLockName()));
//      assertFalse(clm2.defineLock(getLockName()));
//      assertFalse(clm3.defineLock(getLockName()));
//      assertFalse(clm4.defineLock(getLockName()));
//      assertFalse(clm5.defineLock(getLockName()));
//
//      ClusteredLock lock0 = clm0.get(getLockName());
//      ClusteredLock lock1 = clm1.get(getLockName());
//      ClusteredLock lock2 = clm2.get(getLockName());
//      ClusteredLock lock3 = clm3.get(getLockName());
//      ClusteredLock lock4 = clm4.get(getLockName());
//      ClusteredLock lock5 = clm5.get(getLockName());
//
//      splitCluster(new int[]{0, 1, 2, 3}, new int[]{4, 5});
//
//      asList(lock0, lock1, lock2, lock3).forEach(this::assertTryLock);
//
//      assertFailureFromMinorityPartition(lock4);
//      assertFailureFromMinorityPartition(lock5);
//   }
//
//   @Test
//   public void testAutoReleaseIfLockIsAcquiredFromAMinorityPartition() {
//
//      ClusteredLockManager clm0 = EmbeddedClusteredLockManagerFactory.from(getCacheManagers().get(0));
//      ClusteredLockManager clm1 = EmbeddedClusteredLockManagerFactory.from(getCacheManagers().get(1));
//      ClusteredLockManager clm2 = EmbeddedClusteredLockManagerFactory.from(getCacheManagers().get(2));
//
//      assertTrue(clm0.defineLock(getLockName()));
//
//      ClusteredLock lock0 = clm0.get(getLockName());
//      ClusteredLock lock1 = clm1.get(getLockName());
//      ClusteredLock lock2 = clm2.get(getLockName());
//
//      await(lock0.tryLock());
//      assertTrue(await(lock0.isLockedByMe()));
//
//      splitCluster(new int[]{0}, new int[]{1, 2, 3, 4, 5});
//
//      CompletableFuture<Boolean> tryLock1Result = lock1.tryLock(1, TimeUnit.SECONDS);
//      CompletableFuture<Boolean> tryLock2Result = lock2.tryLock(1, TimeUnit.SECONDS);
//
//      assertTrue("Just one of the locks has to work", await(tryLock1Result) ^ await(tryLock2Result));
//
//      assertFailureFromMinorityPartition(lock0);
//   }
//
//   @Test
//   public void testTryLocksBeforeSplitBrain() {
//
//      ClusteredLockManager clm0 = EmbeddedClusteredLockManagerFactory.from(getCacheManagers().get(0));
//      ClusteredLockManager clm1 = EmbeddedClusteredLockManagerFactory.from(getCacheManagers().get(1));
//      ClusteredLockManager clm2 = EmbeddedClusteredLockManagerFactory.from(getCacheManagers().get(2));
//
//      assertTrue(clm0.defineLock(getLockName()));
//
//      ClusteredLock lock0 = clm0.get(getLockName());
//      ClusteredLock lock1 = clm1.get(getLockName());
//      ClusteredLock lock2 = clm2.get(getLockName());
//
//      CompletableFuture<Boolean> tryLock1 = lock1.tryLock();
//      CompletableFuture<Boolean> tryLock2 = lock2.tryLock();
//
//      splitCluster(new int[]{0}, new int[]{1, 2, 3, 4, 5});
//
//      assertTrue("Just one of the locks has to work", await(tryLock1) ^ await(tryLock2));
//
//      assertFailureFromMinorityPartition(lock0);
//   }
//
//   private void assertTryLock(ClusteredLock lock) {
//      Boolean locked = await(lock.tryLock(29, TimeUnit.SECONDS));
//      if (locked) {
//         await(lock.unlock());
//      }
//      assertTrue("Lock acquisition should be true " + lock, locked);
//   }
//
//   private void assertFailureFromMinorityPartition(ClusteredLock lock) {
//      expectCompletionException(ClusteredLockException.class, AvailabilityException.class, lock.tryLock());
//   }
//}
