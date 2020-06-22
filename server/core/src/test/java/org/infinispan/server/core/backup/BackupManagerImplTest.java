package org.infinispan.server.core.backup;

import static org.infinispan.commons.dataconversion.MediaType.APPLICATION_OBJECT_TYPE;
import static org.infinispan.commons.dataconversion.MediaType.APPLICATION_PROTOSTREAM_TYPE;
import static org.infinispan.commons.dataconversion.MediaType.APPLICATION_UNKNOWN_TYPE;
import static org.infinispan.functional.FunctionalTestUtils.MAX_WAIT_SECS;
import static org.infinispan.functional.FunctionalTestUtils.await;
import static org.infinispan.server.core.backup.BackupUtil.CACHES_DIR;
import static org.infinispan.server.core.backup.BackupUtil.CONTAINERS_PROPERTIES_FILE;
import static org.infinispan.server.core.backup.BackupUtil.CONTAINER_DIR;
import static org.infinispan.server.core.backup.BackupUtil.GLOBAL_CONFIG_FILE;
import static org.infinispan.server.core.backup.BackupUtil.MANIFEST_PROPERTIES_FILE;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.infinispan.Cache;
import org.infinispan.commons.CacheException;
import org.infinispan.commons.test.CommonsTestingUtil;
import org.infinispan.commons.util.Util;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.EncodingConfigurationBuilder;
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
@Test(groups = "functional", testName = "server.core.BackupManagerImplTest")
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

   public void testExceptionsPropagated() throws Exception {
      Map<String, DefaultCacheManager> cacheManagers = createManagerMap("container1");
      try (DefaultCacheManager cm = cacheManagers.values().iterator().next()) {
         Path nonExistingDir = new File(CommonsTestingUtil.tmpDirectory(BackupManagerImplTest.class.getSimpleName() + "blah")).toPath();

         BlockingManager blockingManager = cm.getGlobalComponentRegistry().getComponent(BlockingManager.class);
         new BackupManagerImpl(blockingManager, cacheManagers, nonExistingDir)
               .create()
               .toCompletableFuture()
               .get(MAX_WAIT_SECS, TimeUnit.SECONDS);
         fail();
      } catch (ExecutionException e) {
         assertTrue(e.getCause() instanceof CacheException);
      }
   }

   public void testCreateBackupAndRestore() throws Exception {
      int numEntries = 100;
      String container1 = "container1";
      String container2 = "container2";
      Map<String, DefaultCacheManager> readerManagers = createManagerMap(container1, container2);
      Map<String, DefaultCacheManager> writerManagers = createManagerMap(container1, container2);
      try {
         DefaultCacheManager sourceManager1 = writerManagers.get(container1);
         DefaultCacheManager sourceManager2 = writerManagers.get(container2);

         sourceManager1.defineConfiguration("object-cache", config(APPLICATION_OBJECT_TYPE));
         sourceManager1.defineConfiguration("protostream-cache", config(APPLICATION_PROTOSTREAM_TYPE));
         sourceManager1.defineConfiguration("empty-cache", config());
         sourceManager2.defineConfiguration("container2-cache", config());

         IntStream.range(0, numEntries).forEach(i -> sourceManager1.getCache("object-cache").put(i, i));
         IntStream.range(0, numEntries).forEach(i -> sourceManager1.getCache("protostream-cache").put(i, i));

         BlockingManager blockingManager = writerManagers.values().iterator().next().getGlobalComponentRegistry().getComponent(BlockingManager.class);
         BackupWriter writer = new BackupWriter(blockingManager, writerManagers, workingDir.toPath());
         Path backupZip = await(writer.create());
         assertNotNull(backupZip);

         Path extractedRoot = workingDir.toPath().resolve("extracted");
         File extractedDir = extractedRoot.toFile();
         extractedDir.mkdir();
         extractBackup(backupZip.toFile(), extractedDir);

         assertFileExists(extractedRoot, MANIFEST_PROPERTIES_FILE);

         Path containerPath = path(extractedRoot, CONTAINER_DIR, container1);
         assertFileExists(containerPath, CONTAINERS_PROPERTIES_FILE);
         assertFileExists(containerPath, GLOBAL_CONFIG_FILE);
         assertFileExists(containerPath, CACHES_DIR, "object-cache", "object-cache.xml");
         assertFileExists(containerPath, CACHES_DIR, "object-cache", "object-cache.dat");
         assertFileExists(containerPath, CACHES_DIR, "protostream-cache", "protostream-cache.xml");
         assertFileExists(containerPath, CACHES_DIR, "protostream-cache", "protostream-cache.dat");
         assertFileExists(containerPath, CACHES_DIR, "empty-cache", "empty-cache.xml");
         assertFileDoesNotExist(containerPath, CACHES_DIR, "empty-cache", "empty-cache.dat");

         containerPath = path(extractedRoot, CONTAINER_DIR, container2);
         assertFileExists(containerPath, GLOBAL_CONFIG_FILE);
         assertFileExists(containerPath, CONTAINERS_PROPERTIES_FILE);
         assertFileExists(containerPath, CACHES_DIR, "container2-cache", "container2-cache.xml");
         assertFileDoesNotExist(containerPath, CACHES_DIR, "object-cache");
         assertFileDoesNotExist(containerPath, CACHES_DIR, "protostream-cache");


         byte[] zipBytes = Files.readAllBytes(backupZip);
         BackupReader reader = new BackupReader(blockingManager, readerManagers, workingDir.toPath());
         CompletionStage<Void> restoreStage = reader.restore(zipBytes);
         await(restoreStage);

         DefaultCacheManager targetManager1 = readerManagers.get(container1);
         DefaultCacheManager targetManager2 = readerManagers.get(container2);

         Cache<Integer, Integer> objectCache = targetManager1.getCache("object-cache");
         assertFalse(objectCache.isEmpty());
         assertEquals(100, objectCache.size());
         assertEquals(Integer.valueOf(50), objectCache.get(50));

         Cache<Integer, Integer> protoCache = targetManager1.getCache("protostream-cache");
         assertFalse(protoCache.isEmpty());
         assertEquals(100, protoCache.size());
         assertEquals(Integer.valueOf(1), protoCache.get(1));

         Cache<Object, Object> emptyCache = targetManager1.getCache("empty-cache");
         assertTrue(emptyCache.isEmpty());

         Cache<Object, Object> container2Cache = targetManager2.getCache("container2-cache");
         assertTrue(container2Cache.isEmpty());
      } finally {
         writerManagers.values().forEach(EmbeddedCacheManager::stop);
         readerManagers.values().forEach(EmbeddedCacheManager::stop);
      }
   }

   private Map<String, DefaultCacheManager> createManagerMap(String... containers) {
      return Arrays.stream(containers)
            .collect(Collectors.toMap(Function.identity(), name -> new DefaultCacheManager()));
   }

   private Configuration config() {
      return config(APPLICATION_UNKNOWN_TYPE);
   }

   private Configuration config(String type) {
      return config(type, type);
   }

   private Configuration config(String keyType, String valueType) {
      ConfigurationBuilder builder = new ConfigurationBuilder();
      EncodingConfigurationBuilder encoding = builder.encoding();
      encoding.key().mediaType(keyType);
      encoding.value().mediaType(valueType);
      return builder.build();
   }

   private Path path(Path root, String... paths) {
      return Paths.get(root.toString(), paths);
   }

   private void assertFileExists(Path root, String... paths) {
      Path path = path(root, paths);
      assertTrue(path.toFile().exists());
   }

   private void assertFileDoesNotExist(Path root, String... paths) {
      Path path = path(root, paths);
      assertFalse(path.toFile().exists());
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
