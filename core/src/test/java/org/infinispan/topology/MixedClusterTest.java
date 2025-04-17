package org.infinispan.topology;

import static org.infinispan.commons.test.CommonsTestingUtil.tmpDirectory;
import static org.infinispan.test.fwk.TestCacheManagerFactory.createClusteredCacheManager;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Map;
import java.util.stream.IntStream;

import org.infinispan.commons.util.Util;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.globalstate.ConfigurationStorage;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.responses.Response;
import org.infinispan.remoting.rpc.CustomCacheRpcCommand;
import org.infinispan.remoting.rpc.RpcSCI;
import org.infinispan.remoting.transport.Address;
import org.infinispan.remoting.transport.impl.MapResponseCollector;
import org.infinispan.test.MultipleCacheManagersTest;
import org.infinispan.test.TestingUtil;
import org.infinispan.test.fwk.TransportFlags;
import org.infinispan.upgrade.ManagerVersion;
import org.infinispan.util.ByteString;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

/**
 * A test to ensure that unknown commands are gracefully ignored when a cluster consists of mixed versions that do not
 * all support the command.
 */
@Test(testName = "remoting.rpc.MixedClusterUpgradeTest", groups = "functional")
public class MixedClusterTest extends MultipleCacheManagersTest {

   protected static final String TEST_NAME = MixedClusterTest.class.getSimpleName();
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
      IntStream.range(0, 2).forEach(i ->
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

   // TODO how to implement?
   // 1. Add version to CacheTopology -> Topology sharing logic already implemented, but it is at the cache not global level
   // 2. Bespoke mechanism

   // TODO TEST
   // Update test so that a 3rd cluster member joins the cluster with a more recent cluster version
   // Attempt to send new command with new member
   // Expect an exception to be thrown
   // Set ManagerVersion to be latest version for all nodes
   // Ensure command can now be sent
   public void testUnknownCommand() {
      var newVersion = new ManagerVersion(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);

      var globalBuilder = globalBuilder(2);
      globalBuilder.serialization().addContextInitializer(RpcSCI.INSTANCE);

      var cm = createClusteredCacheManager(false, globalBuilder, null, new TransportFlags());
      var ctm = new VersionedClusterTopologyManager(newVersion);
      TestingUtil.replaceComponent(cm, ClusterTopologyManager.class, ctm, true);
      cm.defineConfiguration(TEST_CACHE, cacheConfig());
      cacheManagers.add(cm);
      cm.start();
      waitForClusterToForm(TEST_CACHE);

      assertTrue(ctm.isMixedCluster());
      // TODO need a way to update ClusterTopologyManager both ways
      assertTrue(ctm(manager(0)).isMixedCluster());
      assertTrue(ctm(manager(1)).isMixedCluster());
      assertEquals(ManagerVersion.INSTANCE, ctm.getOldestMember());

      // Execute new command
      var rpcManager = cm.getCache(TEST_CACHE).getAdvancedCache().getRpcManager();
      var cmd = new CustomCacheRpcCommand(ByteString.fromString(TEST_CACHE), "some-value");
      cmd.setSupportedSince(newVersion);
      Map<Address, Response> remoteResponses = rpcManager.blocking(
            rpcManager.invokeCommandOnAll(cmd, MapResponseCollector.ignoreLeavers(), rpcManager.getSyncRpcOptions())
      );
      System.out.println(remoteResponses);
   }

   private ClusterTopologyManagerImpl ctm(EmbeddedCacheManager cm) {
      return TestingUtil.extractGlobalComponent(cm, ClusterTopologyManagerImpl.class);
   }

   static class VersionedClusterTopologyManager extends ClusterTopologyManagerImpl {

      final ManagerVersion version;

      public VersionedClusterTopologyManager(ManagerVersion version) {
         this.version = version;
      }

      @Override
      public ManagerVersion getVersion() {
         return version;
      }
   }
}
