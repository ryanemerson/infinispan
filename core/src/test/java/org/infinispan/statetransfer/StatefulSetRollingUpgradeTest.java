package org.infinispan.statetransfer;

import static org.infinispan.commons.test.CommonsTestingUtil.tmpDirectory;
import static org.infinispan.test.TestingUtil.extractComponent;
import static org.infinispan.test.fwk.TestCacheManagerFactory.createClusteredCacheManager;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.infinispan.commons.util.Util;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.partitionhandling.AvailabilityMode;
import org.infinispan.partitionhandling.PartitionHandling;
import org.infinispan.partitionhandling.impl.PartitionHandlingManager;
import org.infinispan.test.MultipleCacheManagersTest;
import org.infinispan.test.TestingUtil;
import org.infinispan.test.fwk.CleanupAfterMethod;
import org.infinispan.test.fwk.TransportFlags;
import org.testng.annotations.Test;

/**
 * Start cluster (A,B) redeploy after upgrade. Rolling upgrades always occur in the order B,A and A does not restart
 * until B has completed successfully.
 *
 * @author Ryan Emerson
 * @since 11.0
 */
@CleanupAfterMethod
@Test(groups = "functional", testName = "statetransfer.StateTransferRollingUpgradeTest")
public class StatefulSetRollingUpgradeTest extends MultipleCacheManagersTest {

   private static final String CACHE_NAME = "testCache";
   private static final int NUM_NODES = 2;
   private static final int NUM_ROLLING_UPGRADES = 4;

   @Override
   protected void createCacheManagers() throws Throwable {
      cacheManagers = IntStream.range(0, NUM_NODES).boxed().map(ignore -> (EmbeddedCacheManager) null).collect(Collectors.toList());
      Util.recursiveFileRemove(tmpDirectory(this.getClass().getSimpleName()));

      for (int id = 0; id < NUM_NODES; id++)
         createStatefulCacheManager(id);

      waitForClusterToForm(CACHE_NAME);
   }

   public void testStateTransferRestart() throws Throwable {
      for (int i = 0; i < NUM_ROLLING_UPGRADES; i++) {
         // Stop the cache manager in the same manner as via the Shutdownhook
         TestingUtil.extractGlobalComponentRegistry(manager(1)).stop();

         // Wait for the cluster to be degraded
         PartitionHandlingManager phm = extractComponent(cache(0, CACHE_NAME), PartitionHandlingManager.class);
         eventuallyEquals(AvailabilityMode.DEGRADED_MODE, phm::getAvailabilityMode);

         createStatefulCacheManager(1);
         waitForClusterToForm(CACHE_NAME);

         TestingUtil.extractGlobalComponentRegistry(manager(0)).stop();
         createStatefulCacheManager(0);
         waitForClusterToForm(CACHE_NAME);
      }
   }

   private void createStatefulCacheManager(int id) {
      String stateDirectory = tmpDirectory(this.getClass().getSimpleName() + File.separator + id);
      GlobalConfigurationBuilder global = GlobalConfigurationBuilder.defaultClusteredBuilder();
      global.globalState().enable().persistentLocation(stateDirectory);

      ConfigurationBuilder config = getDefaultClusteredCacheConfig(CacheMode.DIST_SYNC);
      config.clustering()
            .partitionHandling().whenSplit(PartitionHandling.DENY_READ_WRITES)
            .stateTransfer().timeout(10, TimeUnit.SECONDS);
      EmbeddedCacheManager manager = createClusteredCacheManager(true, global, null, new TransportFlags());
      cacheManagers.set(id, manager);
      manager.defineConfiguration(CACHE_NAME, config.build());
   }
}
