package org.infinispan.topology;

import static org.infinispan.commons.test.CommonsTestingUtil.tmpDirectory;
import static org.infinispan.test.fwk.TestCacheManagerFactory.createClusteredCacheManager;

import java.util.stream.IntStream;

import org.infinispan.commons.util.Util;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.factories.GlobalComponentRegistry;
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
      builder.globalState().enable().persistentLocation(stateDirectory).configurationStorage(ConfigurationStorage.OVERLAY);
      return builder;
   }

   private Configuration cacheConfig() {
      return getDefaultClusteredCacheConfig(CacheMode.DIST_SYNC).build();
   }

   // TODO add SCI with new command

   // TODO code
   // Add ManagerVersion to ReplicableCommand interface, default to 16.0.0
   // Update RpcManager and/or Transport to include mixed cluster check

   // TODO TEST
   // Update test so that a 3rd cluster member joins the cluster with a more recent cluster version
   // Attempt to send new command with new member
   // Expect an exception to be thrown
   // Set ManagerVersion to be latest version for all nodes
   // Ensure command can now be sent
   public void testUnknownCommand() {
      // TODO reuse instance from fake command?
      var newVersion = new ManagerVersion(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
      // Restart EmbeddedCacheManagers in reverse order to simulate a StatefulSet rolling update
//      for (int i = cacheManagers.size() - 1; i >= 0; i--) {
      for (int i = 0; i < cacheManagers.size(); i++) {
         var cm = (DefaultCacheManager) cacheManagers.get(i);
         // Shutdown server in the same manner as the Server
         cm.shutdownAllCaches();
         cm.stop();
         // TODO assert cluster size = 2 for remaining members
         cm = (DefaultCacheManager) createClusteredCacheManager(false, globalBuilder(i), null, new TransportFlags());
         cacheManagers.set(i, cm);
//         var ctm = (ClusterTopologyManagerImpl) GlobalComponentRegistry.of(cm).getClusterTopologyManager();
//         ctm.setVersion(newVersion);
         cm.defineConfiguration(TEST_CACHE, cacheConfig());
         cm.start();
         waitForClusterToForm(TEST_CACHE);

         // TODO assert cluster size = 3

//         if (ctm.isMixedCluster()) {
//            // TODO attempt to send new command and expect exception if in mixed mode
//         } else {
//            assert i == 0;
//         }
         // TODO attempt send new command succesfully
      }

      // Add CM representing new version with new command
//      var cm = addClusterEnabledCacheManager(RpcSCI.INSTANCE);
//      cm.defineConfiguration(TEST_CACHE, cacheConfig().build());
//      waitForClusterToForm(TEST_CACHE);

      // Execute new command
//      var rpcManager = cm.getCache(TEST_CACHE).getAdvancedCache().getRpcManager();
//      var cmd = new CustomCacheRpcCommand(ByteString.fromString(TEST_CACHE), "some-value");
//      Map<Address, Response> remoteResponses = rpcManager.blocking(
//            rpcManager.invokeCommandOnAll(cmd, MapResponseCollector.ignoreLeavers(), rpcManager.getSyncRpcOptions())
//      );
//      System.out.println(remoteResponses);
   }
}
