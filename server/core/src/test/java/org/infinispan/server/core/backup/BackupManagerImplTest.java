package org.infinispan.server.core.backup;

import static org.infinispan.functional.FunctionalTestUtils.MAX_WAIT_SECS;
import static org.infinispan.functional.FunctionalTestUtils.await;
import static org.infinispan.server.core.backup.BackupUtil.CACHES_DIR;
import static org.infinispan.server.core.backup.BackupUtil.CONTAINERS_PROPERTIES_FILE;
import static org.infinispan.server.core.backup.BackupUtil.CONTAINER_DIR;
import static org.infinispan.server.core.backup.BackupUtil.GLOBAL_CONFIG_FILE;
import static org.infinispan.server.core.backup.BackupUtil.MANIFEST_PROPERTIES_FILE;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.infinispan.Cache;
import org.infinispan.commons.CacheException;
import org.infinispan.commons.test.CommonsTestingUtil;
import org.infinispan.commons.util.Util;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.test.AbstractInfinispanTest;
import org.infinispan.util.concurrent.BlockingManager;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author Ryan Emerson
 * @since 11.0
 */
@Test(groups = "functional", testName = "server.core.BackupManagerTest")
public class BackupManagerImplTest extends AbstractInfinispanTest {

   private static File workingDir;

   @BeforeClass
   static void setup() {
      workingDir = new File(CommonsTestingUtil.tmpDirectory(BackupManagerImplTest.class));
      Util.recursiveFileRemove(workingDir);
      workingDir.mkdirs();
   }

   @AfterClass
   static void teardown() {
      Util.recursiveFileRemove(workingDir);
   }

   public void testExceptionsPropogated() throws Exception {
      Map<String, DefaultCacheManager> cacheManagers = createManagerMap(false);
      try {
         Path nonExistingDir = new File(CommonsTestingUtil.tmpDirectory(BackupManagerImplTest.class.getSimpleName() + "blah")).toPath();

         BlockingManager blockingManager = cacheManagers.get("container1").getGlobalComponentRegistry().getComponent(BlockingManager.class);
         new BackupManagerImpl(blockingManager, cacheManagers, null, nonExistingDir)
               .create()
               .toCompletableFuture()
               .get(MAX_WAIT_SECS, TimeUnit.SECONDS);
         fail();
      } catch (ExecutionException e) {
         assertTrue(e.getCause() instanceof CacheException);
      } finally {
         cacheManagers.values().forEach(EmbeddedCacheManager::stop);
      }
   }

   public void testConcurrentBackupException() {
      // TODO
   }

   public void testCreateBackup() throws Exception {
      Map<String, DefaultCacheManager> writerManagers = createManagerMap(true);
      Map<String, DefaultCacheManager> readerManagers = createManagerMap(false);
      try {
         BlockingManager blockingManager = writerManagers.get("container1").getGlobalComponentRegistry().getComponent(BlockingManager.class);
         BackupWriter writer = new BackupWriter(blockingManager, writerManagers, null, workingDir.toPath());
         Path backupZip = await(writer.create());
         assertNotNull(backupZip);

         Path extractedRoot = workingDir.toPath().resolve("extracted");
         File extractedDir = extractedRoot.toFile();
         extractedDir.mkdir();
         extractBackup(backupZip.toFile(), extractedDir);

         assertFileExists(extractedRoot, MANIFEST_PROPERTIES_FILE);

         Path containerPath = path(extractedRoot, CONTAINER_DIR, "container1");
         assertFileExists(containerPath, CONTAINERS_PROPERTIES_FILE);
         assertFileExists(containerPath, GLOBAL_CONFIG_FILE);
         assertFileExists(containerPath, CACHES_DIR, "cache1", "cache1.xml");
         assertFileExists(containerPath, CACHES_DIR, "cache1", "cache1.dat");
         assertFileExists(containerPath, CACHES_DIR, "cache2", "cache2.xml");

         containerPath = path(extractedRoot, CONTAINER_DIR, "container2");
         assertFileExists(containerPath, GLOBAL_CONFIG_FILE);
         assertFileExists(containerPath, CONTAINERS_PROPERTIES_FILE);
         assertFileExists(containerPath, CACHES_DIR, "cache1", "cache1.xml");
         assertFileExists(containerPath, CACHES_DIR, "cache2", "cache2.xml");

         // TODO replace with call to BackupManager class
         BackupReader reader = new BackupReader(blockingManager, readerManagers, workingDir.toPath());
         byte[] zipBytes = Files.readAllBytes(backupZip);
         CompletionStage<Void> restoreStage = reader.restore(zipBytes);
         await(restoreStage);
         EmbeddedCacheManager container1 = readerManagers.get("container1");
         assertNotNull(container1);
         Cache<byte[], byte[]> cache1 = container1.getCache("cache1");
         assertNotNull(cache1);
         assertEquals("value".getBytes(), cache1.get("key".getBytes()));
      } finally {
         writerManagers.values().forEach(EmbeddedCacheManager::stop);
         readerManagers.values().forEach(EmbeddedCacheManager::stop);
      }
   }

   private Map<String, DefaultCacheManager> createManagerMap(boolean createContent) {
      ConfigurationBuilderHolder ch = new ConfigurationBuilderHolder();
      Map<String, ConfigurationBuilder> caches = ch.getNamedConfigurationBuilders();

      if (createContent) {
         caches.put("cache1", new ConfigurationBuilder());
         caches.put("cache2", new ConfigurationBuilder());
      }

      Map<String, DefaultCacheManager> managerMap = Stream.of("container1", "container2")
            .collect(Collectors.toMap(Function.identity(), name -> new DefaultCacheManager(ch, true)));

      if (createContent) {
         DefaultCacheManager cm = managerMap.get("container1");
         Cache<byte[], byte[]> cache = cm.getCache("cache1");
         cache.put("key".getBytes(), "value".getBytes());

         // TODO test proto files in a Integration test ... it's not possible to test in server/core as no dependency on remote-query-server module
//         cm.getCache(PROTO_CACHE_NAME).put("test.proto", "message test {}");
//         cm.getCache(PROTO_CACHE_NAME).put("test2.proto", "message test2 {\n\toptional string name = 1;\n}");

         // TODO add integration test for counters
      }

      // TODO add values to caches with different MediaTypes
//      TODO fails because of Integer to byte[] cast failure in BackupManager ... is this because x-java-object?
//      IntStream.range(0, 1000).forEach(i -> cache.put(i, i));

      return managerMap;
   }

   private Path path(Path root, String... paths) {
      return Paths.get(root.toString(), paths);
   }

   private void assertFileExists(Path root, String... paths) {
      Path path = path(root, paths);
      assertFileExists(path);
   }

   private void assertFileExists(Path path) {
      assertTrue(path.toFile().exists());
   }

   private void extractBackup(File backup, File destDir) throws IOException {
      try (ZipFile zip = new ZipFile(backup)) {
         Enumeration<? extends ZipEntry> zipEntries = zip.entries();
         while (zipEntries.hasMoreElements()) {
            ZipEntry entry = zipEntries.nextElement();
            File file = new File(destDir, entry.getName());
            if (entry.isDirectory()) {
               file.mkdirs();
            } else {
               file.getParentFile().mkdirs();
               Files.copy(zip.getInputStream(entry), file.toPath());
            }
         }
      }
   }
}
