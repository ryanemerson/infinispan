package org.infinispan.remoting.rpc;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.test.MultipleCacheManagersTest;
import org.testng.annotations.Test;

/**
 * A test to ensure that unknown commands are gracefully ignored. Unknown commands can exist when a new command is added
 * in a newer Infinispan version and the cluster consists of mixed versions during an upgrade.
 */
@Test(testName = "remoting.rpc.UnknownCommandTest", groups = "functional")
public class UnknownCommandTest extends MultipleCacheManagersTest {

   protected static final String TEST_CACHE = UnknownCommandTest.class.getSimpleName();

   @Override
   protected void createCacheManagers() throws Throwable {
      // Create cluster of 2 CM representing old version without command
      createClusteredCaches(2, TEST_CACHE, cacheConfig());
   }

   public void testUnknownCommand() {
      // Add CM representing new version with new command
      var cm = addClusterEnabledCacheManager(RpcSCI.INSTANCE);
      cm.defineConfiguration(TEST_CACHE, cacheConfig().build());
      waitForClusterToForm(TEST_CACHE);

      // Execute new command
//      var rpcManager = cm.getCache(TEST_CACHE).getAdvancedCache().getRpcManager();
//      var cmd = new CustomCacheRpcCommand(ByteString.fromString(TEST_CACHE), "some-value");
//      Map<Address, Response> remoteResponses = rpcManager.blocking(
//            rpcManager.invokeCommandOnAll(cmd, MapResponseCollector.ignoreLeavers(), rpcManager.getSyncRpcOptions())
//      );
//      System.out.println(remoteResponses);
   }

   private ConfigurationBuilder cacheConfig() {
      return getDefaultClusteredCacheConfig(CacheMode.DIST_SYNC);
   }
}
