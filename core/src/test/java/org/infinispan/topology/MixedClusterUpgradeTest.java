package org.infinispan.topology;

import static org.infinispan.commons.test.CommonsTestingUtil.tmpDirectory;
import static org.infinispan.test.fwk.TestCacheManagerFactory.createClusteredCacheManager;

import java.util.stream.IntStream;

import org.infinispan.commons.util.Util;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.globalstate.ConfigurationStorage;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.test.MultipleCacheManagersTest;
import org.infinispan.test.fwk.TransportFlags;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

/**
 * A test to ensure that unknown commands are gracefully ignored when a cluster consists of mixed versions that do not
 * all support the command.
 */
@Test(testName = "remoting.rpc.MixedClusterUpgradeTest", groups = "functional")
public class MixedClusterUpgradeTest extends MultipleCacheManagersTest {

   protected static final String TEST_NAME = MixedClusterUpgradeTest.class.getSimpleName();
   protected static final String TEST_CACHE = TEST_NAME;

   @AfterClass(alwaysRun = true)
   @Override
   protected void destroy() {
      super.destroy();
      Util.recursiveFileRemove(tmpDirectory(TEST_NAME));
   }

   @Override
   protected void createCacheManagers() throws Throwable {
      Util.recursiveFileRemove(tmpDirectory(TEST_NAME));
      IntStream.range(0, 3).forEach(i ->
            addClusterEnabledCacheManager(globalBuilder(i), null)
                  .defineConfiguration(TEST_CACHE, cacheConfig())
      );
      waitForClusterToForm(TEST_CACHE);
   }

   private GlobalConfigurationBuilder globalBuilder(int index) {
      String stateDirectory = tmpDirectory(TEST_NAME, Integer.toString(index));
      GlobalConfigurationBuilder builder = GlobalConfigurationBuilder.defaultClusteredBuilder();
      builder.transport().nodeName(TEST_NAME + "-" + index);
      builder.globalState().enable().persistentLocation(stateDirectory).configurationStorage(ConfigurationStorage.OVERLAY);
      return builder;
   }

   private Configuration cacheConfig() {
      return getDefaultClusteredCacheConfig(CacheMode.DIST_SYNC).build();
   }

   public void testUnknownCommand() {
      int i = 0;
      var cm = (DefaultCacheManager) cacheManagers.get(i);
      // Shutdown server in the same manner as the Server
      cm.shutdownAllCaches();
      cm.stop();

      cm = (DefaultCacheManager) createClusteredCacheManager(false, globalBuilder(i), null, new TransportFlags());
      cacheManagers.set(i, cm);
      cm.defineConfiguration(TEST_CACHE, cacheConfig());

      // Restart server with persistent state
      cm.start();

      // waitForNoRebalance fails as rebalanceInProgress=true
      // "Ignoring late consistent hash update for cache MixedClusterUpgradeTest, current topology is CacheTopology{id=12, phase=NO_REBALANCE, rebalanceId=1, currentCH=DefaultConsistentHash{ns=256, owners = (3)[MixedClusterUpgradeTest-0: 83+84, MixedClusterUpgradeTest-1: 86+85, MixedClusterUpgradeTest-2: 87+87]}, pendingCH=null, unionCH=null, actualMembers=[MixedClusterUpgradeTest-0, MixedClusterUpgradeTest-1, MixedClusterUpgradeTest-2], persistentUUIDs=[8aebd82c-d8f3-4a38-9034-9864a2eb693c, 5cc6cfe3-7bf1-476d-ba47-a733bc5e4073, b8383516-9c9a-4425-b771-4d3a79270c65]} received CacheTopology{id=12, phase=NO_REBALANCE, rebalanceId=1, currentCH=DefaultConsistentHash{ns=256, owners = (3)[MixedClusterUpgradeTest-0: 83+84, MixedClusterUpgradeTest-1: 86+85, MixedClusterUpgradeTest-2: 87+87]}, pendingCH=null, unionCH=null, actualMembers=[MixedClusterUpgradeTest-0, MixedClusterUpgradeTest-1, MixedClusterUpgradeTest-2], persistentUUIDs=[8aebd82c-d8f3-4a38-9034-9864a2eb693c, 5cc6cfe3-7bf1-476d-ba47-a733bc5e4073, b8383516-9c9a-4425-b771-4d3a79270c65]}"
      waitForClusterToForm(TEST_CACHE);
   }
}
