package org.infinispan.server.functional;

import static org.infinispan.functional.FunctionalTestUtils.await;
import static org.infinispan.util.concurrent.CompletionStages.join;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.infinispan.client.rest.RestCacheClient;
import org.infinispan.client.rest.RestCacheManagerClient;
import org.infinispan.client.rest.RestClient;
import org.infinispan.client.rest.RestClusterClient;
import org.infinispan.client.rest.RestCounterClient;
import org.infinispan.client.rest.RestEntity;
import org.infinispan.client.rest.RestResponse;
import org.infinispan.client.rest.RestTaskClient;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.dataconversion.internal.Json;
import org.infinispan.commons.test.CommonsTestingUtil;
import org.infinispan.commons.util.Util;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.counter.api.Storage;
import org.infinispan.counter.configuration.Element;
import org.infinispan.test.TestingUtil;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Ryan Emerson
 * @since 11.0
 */
public class BackupManagerIT extends AbstractMultiClusterIT {

   static final File WORKING_DIR = new File(CommonsTestingUtil.tmpDirectory(BackupManagerIT.class));
   static final int NUM_ENTRIES = 10;

   public BackupManagerIT() {
      super("configuration/ClusteredServerTest.xml");
   }

   @BeforeClass
   public static void setup() {
      WORKING_DIR.mkdirs();
   }

   @AfterClass
   public static void teardown() {
      Util.recursiveFileRemove(WORKING_DIR);
   }

   @Test
   public void testManagerBackup() throws Exception {
      String backupName = "testManagerBackup";
      performTest(
            client -> {
               RestCacheManagerClient cm = client.cacheManager("clustered");
               RestResponse response = await(cm.createBackup(backupName));
               assertEquals(202, response.getStatus());
               return downloadBackup(() -> cm.getBackup(backupName));
            },
            client -> await(client.cacheManager("clustered").deleteBackup(backupName)),
            (zip, client) -> client.cacheManager("clustered").restore(zip),
            this::assertWildcardContent
      );
   }

   @Test
   public void testManagerBackupParameters() throws Exception {
      String backupName = "testManagerBackup";
      performTest(
            client -> {
               Map<String, List<String>> params = new HashMap<>();
               params.put("caches", Collections.singletonList("*"));
               params.put("counters", Collections.singletonList("weak-volatile"));

               RestCacheManagerClient cm = client.cacheManager("clustered");
               RestResponse response = await(cm.createBackup(backupName, params));
               assertEquals(202, response.getStatus());
               return downloadBackup(() -> cm.getBackup(backupName));
            },
            client -> await(client.cacheManager("clustered").deleteBackup(backupName)),
            (zip, client) -> {
               Map<String, List<String>> params = new HashMap<>();
               params.put("caches", Collections.singletonList("cache1"));
               params.put("counters", Collections.singletonList("*"));
               return client.cacheManager("clustered").restore(zip, params);
            },
            client -> {
               // Assert that only caches and the specified "weak-volatile" counter have been backed up. Internal caches will still be present
               assertEquals("[\"___protobuf_metadata\",\"memcachedCache\",\"cache1\",\"___script_cache\"]", await(client.caches()).getBody());
               assertEquals("[\"weak-volatile\"]", await(client.counters()).getBody());
               assertEquals(404, await(client.schemas().get("schema.proto")).getStatus());
               assertEquals("[]", await(client.tasks().list(RestTaskClient.ResultType.USER)).getBody());
            }
      );
   }

   @Test
   public void testClusterBackup() throws Exception {
      String backupName = "testClusterBackup";
      performTest(
            client -> {
               RestClusterClient cluster = client.cluster();
               RestResponse response = await(cluster.createBackup(backupName));
               assertEquals(202, response.getStatus());
               return downloadBackup(() -> cluster.getBackup(backupName));
            },
            client -> await(client.cacheManager("clustered").deleteBackup(backupName)),
            (zip, client) -> client.cluster().restore(zip),
            this::assertWildcardContent
      );
   }

   private RestResponse downloadBackup(Supplier<CompletionStage<RestResponse>> download) {
      int count = 0;
      RestResponse response;
      while ((response = await(download.get())).getStatus() == 202 || count++ < 100) {
         TestingUtil.sleepThread(10);
      }
      assertEquals(200, response.getStatus());
      return response;
   }

   private void performTest(Function<RestClient, RestResponse> backupAndDownload,
                            Function<RestClient, RestResponse> delete,
                            BiFunction<File, RestClient, CompletionStage<RestResponse>> restore,
                            Consumer<RestClient> assertTargetContent) throws Exception {
      // Start the source cluster
      startSourceCluster();
      RestClient client = source.getClient();

      // Populate the source container
      populateContainer(client);

      // Perform the backup and download
      RestResponse getResponse = backupAndDownload.apply(client);
      String fileName = getResponse.getHeader("Content-Disposition").split("=")[1];

      // Delete the backup from the server
      RestResponse deleteResponse = delete.apply(client);
      assertEquals(204, deleteResponse.getStatus());

      // Ensure that all of the backup files have been deleted from the source cluster
      // We must wait for a short period time here to ensure that the returned entity has actually been removed from the filesystem
      Thread.sleep(50);
      assertNoServerBackupFilesExist(source);

      // Shutdown the source cluster
      source.stop("source");

      // Start the target cluster
      startTargetCluster();
      client = target.getClient();

      // Copy the returned zip bytes to the local working dir
      File backupZip = new File(WORKING_DIR, fileName);
      try (InputStream is = getResponse.getBodyAsStream()) {
         Files.copy(is, backupZip.toPath(), StandardCopyOption.REPLACE_EXISTING);
      }

      // Upload the backup to the target cluster
      deleteResponse = await(restore.apply(backupZip, client));
      assertEquals(deleteResponse.getBody(), 204, deleteResponse.getStatus());

      // Assert that all content has been restored as expected
      assertTargetContent.accept(client);

      // Ensure that the backup files have been deleted from the target cluster
      assertNoServerBackupFilesExist(target);
      stopTargetCluster();
   }

   private void populateContainer(RestClient client) throws Exception {
      String cacheName = "cache1";
      createCache(cacheName, new ConfigurationBuilder(), client);

      RestCacheClient cache = client.cache(cacheName);
      for (int i = 0; i < NUM_ENTRIES; i++) {
         join(cache.put(String.valueOf(i), String.valueOf(i)));
      }
      assertEquals(NUM_ENTRIES, getCacheSize(cacheName, client));

      createCounter("weak-volatile", Element.WEAK_COUNTER, Storage.VOLATILE, client, 0);
      createCounter("weak-persistent", Element.WEAK_COUNTER, Storage.PERSISTENT, client, -100);
      createCounter("strong-volatile", Element.STRONG_COUNTER, Storage.VOLATILE, client, 50);
      createCounter("strong-persistent", Element.STRONG_COUNTER, Storage.PERSISTENT, client, 0);

      addSchema(client);

      try (InputStream is = BackupManagerIT.class.getResourceAsStream("/scripts/test.js")) {
         String script = CommonsTestingUtil.loadFileAsString(is);
         RestResponse rsp = await(client.tasks().uploadScript("test.js", RestEntity.create(MediaType.APPLICATION_JAVASCRIPT, script)));
         assertEquals(200, rsp.getStatus());
      }
   }

   private void assertWildcardContent(RestClient client) {
      String cacheName = "cache1";
      assertEquals(Integer.toString(NUM_ENTRIES), await(client.cache(cacheName).size()).getBody());

      assertCounter(client, "weak-volatile", Element.WEAK_COUNTER, Storage.VOLATILE, 0);
      assertCounter(client, "weak-persistent", Element.WEAK_COUNTER, Storage.PERSISTENT, -100);
      assertCounter(client, "strong-volatile", Element.STRONG_COUNTER, Storage.VOLATILE, 50);
      assertCounter(client, "strong-persistent", Element.STRONG_COUNTER, Storage.PERSISTENT, 0);

      RestResponse rsp = await(client.schemas().get("schema.proto"));
      assertEquals(200, rsp.getStatus());
      assertTrue(rsp.getBody().contains("message Person"));

      rsp = await(client.tasks().list(RestTaskClient.ResultType.USER));
      assertEquals(200, rsp.getStatus());

      Json json = Json.read(rsp.getBody());
      assertTrue(json.isArray());
      List<Json> tasks = json.asJsonList();
      assertEquals(1, tasks.size());
      assertEquals("test.js", tasks.get(0).at("name").asString());
   }

   private void createCounter(String name, Element type, Storage storage, RestClient client, long delta) {
      String config = String.format("{\n" +
            "    \"%s\":{\n" +
            "        \"initial-value\":0,\n" +
            "        \"storage\":\"%s\"\n" +
            "    }\n" +
            "}", type, storage.toString());
      RestCounterClient counterClient = client.counter(name);
      RestResponse rsp = await(counterClient.create(RestEntity.create(MediaType.APPLICATION_JSON, config)));
      assertEquals(200, rsp.getStatus());

      if (delta != 0) {
         rsp = await(counterClient.add(delta));
         assertEquals(name, name.contains("strong") ? 200 : 204, rsp.getStatus());
         assertNotNull(rsp.getBody());
      }
   }

   private void assertCounter(RestClient client, String name, Element type, Storage storage, long expectedValue) {
      RestResponse rsp = await(client.counter(name).configuration());
      assertEquals(200, rsp.getStatus());
      String content = rsp.getBody();
      Json config = Json.read(content).at(type.toString());
      assertEquals(name, config.at("name").asString());
      assertEquals(storage.toString(), config.at("storage").asString());
      assertEquals(0, config.at("initial-value").asInteger());

      rsp = await(client.counter(name).get());
      assertEquals(200, rsp.getStatus());
      assertEquals(expectedValue, Long.parseLong(rsp.getBody()));
   }

   private void assertNoServerBackupFilesExist(Cluster cluster) {
      for (int i = 0; i < 2; i++) {
         Path root = cluster.driver.getRootDir().toPath();
         File workingDir = root.resolve(Integer.toString(i)).resolve("data").resolve("backup-manager").toFile();
         assertTrue(workingDir.isDirectory());
         String[] files = workingDir.list();
         assertNotNull(files);
         assertEquals(Arrays.toString(files), 0, files.length);
      }
   }
}
