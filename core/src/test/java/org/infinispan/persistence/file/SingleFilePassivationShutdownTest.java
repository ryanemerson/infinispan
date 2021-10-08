//package org.infinispan.persistence.file;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.concurrent.TimeUnit;
//
//import javax.cache.CacheManager;
//
//import org.infinispan.Cache;
//import org.infinispan.commons.test.CommonsTestingUtil;
//import org.infinispan.commons.util.Util;
//import org.infinispan.configuration.cache.CacheMode;
//import org.infinispan.configuration.cache.ConfigurationBuilder;
//import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
//import org.infinispan.manager.DefaultCacheManager;
//import org.infinispan.manager.EmbeddedCacheManager;
//import org.infinispan.test.AbstractInfinispanTest;
//import org.infinispan.test.MultipleCacheManagersTest;
//import org.testng.annotations.AfterClass;
//import org.testng.annotations.BeforeClass;
//import org.testng.annotations.Test;
//
///**
// * @author Ryan Emerson
// * @since 13.0
// */
//@Test(groups = "unit", testName = "persistence.SingleFile.SingleFilePassivationShutdownTest")
//public class SingleFilePassivationShutdownTest extends MultipleCacheManagersTest {
//
//   private final String CACHE_NAME = SingleFilePassivationShutdownTest.class.getSimpleName();
//
//   private String tmpDirectory;
//
//   @BeforeClass
//   protected void setUpTempDir() throws IOException {
//      tmpDirectory = CommonsTestingUtil.tmpDirectory(this.getClass());
//      new File(tmpDirectory).mkdirs();
//   }
//
//   @AfterClass
//   protected void clearTempDir() {
//      Util.recursiveFileRemove(tmpDirectory);
//   }
//
//   @Override
//   protected void createCacheManagers() throws Throwable {
//      ConfigurationBuilder builder = getDefaultClusteredCacheConfig(CacheMode.DIST_SYNC)
//            .persistence()
//            .availabilityInterval(-1)
//            .passivation(true)
//            .addSingleFileStore()
//            .location(tmpDirectory)
//            // Internal caches don't need to be segmented
//            .segmented(false)
//            .preload(true)
//            .fetchPersistentState(true)
//            .expiration()
//            .lifespan(7, TimeUnit.DAYS);
//      createCluster(builder, 2);
//      waitForClusterToForm();
//   }
//
//   @Test
//   public void testPassivationOnShutdown() throws Exception {
//      cache
//         Cache<?,?> cache = cm.getCache(cacheName);
//         cache.stop();
//   }
//}
