package org.infinispan.globalstate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.infinispan.commons.test.CommonsTestingUtil.tmpDirectory;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.infinispan.Cache;
import org.infinispan.commons.CacheException;
import org.infinispan.commons.util.Util;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.distribution.ch.ConsistentHash;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.transport.Address;
import org.infinispan.remoting.transport.Transport;
import org.infinispan.test.MultipleCacheManagersTest;
import org.infinispan.test.TestingUtil;
import org.infinispan.topology.LocalTopologyManager;

public abstract class AbstractGlobalStateRestartTest extends MultipleCacheManagersTest {

   public static final int DATA_SIZE = 100;

   public static final String CACHE_NAME = "testCache";

   protected abstract int getClusterSize();

   @Override
   protected boolean cleanupAfterMethod() {
      return true;
   }

   @Override
   protected boolean cleanupAfterTest() {
      return false;
   }

   @Override
   protected void createCacheManagers() throws Throwable {
      Util.recursiveFileRemove(tmpDirectory(this.getClass().getSimpleName()));
      createStatefulCacheManagers(true, -1, false);
   }

   protected void createStatefulCacheManagers(boolean clear, int extraneousNodePosition, boolean reverse) {
      int totalNodes = getClusterSize() + ((extraneousNodePosition < 0) ? 0 : 1);
      int node = reverse ? getClusterSize() - 1 : 0;
      int step = reverse ? -1 : 1;
      for (int i = 0; i < totalNodes; i++) {
         if (i == extraneousNodePosition) {
            // Create one more node if needed in the requested position
            createStatefulCacheManager(Character.toString('@'), true);
         } else {
            createStatefulCacheManager(Character.toString((char) ('A' + node)), clear);
            node += step;
         }
      }
   }

   void createStatefulCacheManager(String id, boolean clear) {
      String stateDirectory = tmpDirectory(this.getClass().getSimpleName(), id);
      if (clear)
         Util.recursiveFileRemove(stateDirectory);
      GlobalConfigurationBuilder global = GlobalConfigurationBuilder.defaultClusteredBuilder();
      global.globalState().enable().persistentLocation(stateDirectory);

      ConfigurationBuilder config = new ConfigurationBuilder();
      applyCacheManagerClusteringConfiguration(id, config);
      config.persistence().addSingleFileStore().location(stateDirectory).fetchPersistentState(true);
      config.clustering().stateTransfer().timeout(90, TimeUnit.SECONDS);
      EmbeddedCacheManager manager = addClusterEnabledCacheManager(global, null);
      manager.defineConfiguration(CACHE_NAME, config.build());
   }

   protected abstract void applyCacheManagerClusteringConfiguration(ConfigurationBuilder config);

   protected void applyCacheManagerClusteringConfiguration(String id, ConfigurationBuilder config) {
      applyCacheManagerClusteringConfiguration(config);
   }

   protected void shutdownAndRestart(int extraneousNodePosition, boolean reverse) throws Throwable {
      List<Address> addresses = createInitialCluster();

      ConsistentHash oldConsistentHash = advancedCache(0, CACHE_NAME).getDistributionManager().getWriteConsistentHash();

      // Shutdown the cache cluster-wide
      cache(0, CACHE_NAME).shutdown();

      TestingUtil.killCacheManagers(this.cacheManagers);

      // Verify that the cache state file exists
      for (int i = 0; i < getClusterSize(); i++) {
         String persistentLocation = manager(i).getCacheManagerConfiguration().globalState().persistentLocation();
         File[] listFiles = new File(persistentLocation).listFiles((dir, name) -> name.equals(CACHE_NAME + ".state"));
         assertEquals(Arrays.toString(listFiles), 1, listFiles.length);
      }
      this.cacheManagers.clear();

      // Recreate the cluster
      createStatefulCacheManagers(false, extraneousNodePosition, reverse);
      if(reverse) {
         Collections.reverse(addresses);
      }

      // Healthy cluster
      switch (extraneousNodePosition) {
         case -1: {
            assertHealthyCluster(addresses, oldConsistentHash);
            break;
         }
         case 0: {
            // Coordinator without state, all other nodes will break
            for(int i = 1; i < cacheManagers.size(); i++) {
               try {
                  cache(i, CACHE_NAME);
                  fail("Cache with state should not have joined coordinator without state");
               } catch (CacheException e) {
                  // Ignore
                  log.debugf("Got expected exception: %s", e);
               }
            }
            break;
         }
         default: {
            // Other node without state.
            // We create on all the members. The extraneous members uses a fork to not block.
            Future<Cache<Object, Object>> extraneousCreate = null;
            for (int i = 0; i < managers().length; i++) {
               if (i == extraneousNodePosition) {
                  extraneousCreate = fork(() -> cache(extraneousNodePosition, CACHE_NAME));
               } else {
                  cache(i, CACHE_NAME);
               }
            }
            assertThat(extraneousCreate).isNotNull();
            extraneousCreate.get(10, TimeUnit.SECONDS);

            checkClusterRestartedCorrectly(addresses);
            checkData();
         }
      }
   }

   protected void assertHealthyCluster(List<Address> addresses, ConsistentHash oldConsistentHash) throws Throwable {
      // Healthy cluster
      waitForClusterToForm(CACHE_NAME);

      checkClusterRestartedCorrectly(addresses);
      checkData();

      ConsistentHash newConsistentHash = advancedCache(0, CACHE_NAME).getDistributionManager().getWriteConsistentHash();
      assertEquivalent(oldConsistentHash, newConsistentHash);
   }

   protected void restartWithoutGracefulShutdown() {
      // Verify that the state file was removed
      for (int i = 0; i < getClusterSize(); i++) {
         String persistentLocation = manager(i).getCacheManagerConfiguration().globalState().persistentLocation();
         File[] listFiles = new File(persistentLocation).listFiles((dir, name) -> name.equals(CACHE_NAME + ".state"));
         assertEquals(Arrays.toString(listFiles), 0, listFiles.length);
      }

      // Stop the cluster without graceful shutdown
      for (int i = getClusterSize() - 1; i >= 0; i--) {
         killMember(i, CACHE_NAME, false);
      }

      // Start a coordinator without state and then make the nodes with state join
      createStatefulCacheManagers(false, 0, false);

      for (int i = 0; i <= getClusterSize(); i++) {
         cache(i, CACHE_NAME);
      }
   }

   void assertEquivalent(ConsistentHash oldConsistentHash, ConsistentHash newConsistentHash) {
      assertTrue(isEquivalent(oldConsistentHash, newConsistentHash));
   }

   void checkClusterRestartedCorrectly(List<Address> addresses) throws Exception {
      Iterator<Address> addressIterator = addresses.iterator();
      Set<Address> uuids = new HashSet<>();
      for (EmbeddedCacheManager cm : cacheManagers)
         assertTrue(uuids.add(localAddress(cm)));


      for (int i = 0; i < cacheManagers.size() && addressIterator.hasNext(); i++) {
         // Ensure that nodes have the old UUID based address
         Address entry = addressIterator.next();
         assertTrue("Expected Address: " + entry + " not found in: " + uuids, uuids.contains(entry));
         // Ensure that rebalancing is enabled for the cache
         LocalTopologyManager ltm = TestingUtil.extractGlobalComponent(manager(i), LocalTopologyManager.class);
         assertTrue(ltm.isCacheRebalancingEnabled(CACHE_NAME));
      }
   }

   void checkData() {
      // Ensure that the cache contains the right data
      assertEquals(DATA_SIZE, cache(0, CACHE_NAME).size());
      for (int i = 0; i < DATA_SIZE; i++) {
         assertEquals(cache(0, CACHE_NAME).get(String.valueOf(i)), String.valueOf(i));
      }
   }

   List<Address> createInitialCluster() {
      waitForClusterToForm(CACHE_NAME);
      fillData();
      checkData();

      // Collect using an ArrayList explicitly so that we can reverse the collection later
      return cacheManagers.stream()
            .map(this::localAddress)
            .collect(Collectors.toCollection(ArrayList::new));
   }

   private Address localAddress(EmbeddedCacheManager cm) {
      return TestingUtil.extractGlobalComponent(cm, Transport.class).getAddress();
   }

   private void fillData() {
      // Fill some data
      for (int i = 0; i < DATA_SIZE; i++) {
         cache(0, CACHE_NAME).put(String.valueOf(i), String.valueOf(i));
      }
   }

   private boolean isEquivalent(ConsistentHash oldConsistentHash, ConsistentHash newConsistentHash) {
      if (oldConsistentHash.getNumSegments() != newConsistentHash.getNumSegments()) return false;
      for (int i = 0; i < oldConsistentHash.getMembers().size(); i++) {
         Address oldAddress = oldConsistentHash.getMembers().get(i);
         Address newAddress = newConsistentHash.getMembers().get(i);
         Set<Integer> oldSegmentsForOwner = oldConsistentHash.getSegmentsForOwner(oldAddress);
         Set<Integer> newSegmentsForOwner = newConsistentHash.getSegmentsForOwner(newAddress);
         if (!oldSegmentsForOwner.equals(newSegmentsForOwner)) return false;
      }
      return true;
   }
}
