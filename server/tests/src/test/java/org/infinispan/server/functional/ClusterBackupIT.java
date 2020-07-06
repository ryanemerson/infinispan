package org.infinispan.server.functional;

import static org.infinispan.functional.FunctionalTestUtils.await;
import static org.infinispan.util.concurrent.CompletionStages.join;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.infinispan.client.rest.RestCacheClient;
import org.infinispan.client.rest.RestClient;
import org.infinispan.client.rest.RestEntity;
import org.infinispan.client.rest.RestResponse;
import org.infinispan.client.rest.RestTaskClient;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.test.CommonsTestingUtil;
import org.infinispan.commons.util.Util;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.counter.api.Storage;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * @author Ryan Emerson
 * @since 11.0
 */
public class ClusterBackupIT extends AbstractMultiClusterIT {

   static final File WORKING_DIR = new File(CommonsTestingUtil.tmpDirectory(ClusterBackupIT.class));
   static final int NUM_ENTRIES = 10;

   public ClusterBackupIT() {
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
   public void testClusterBackup() throws Exception {
      // Start the source cluster
      startSourceCluster();
      RestClient client = source.getClient();

      // Populate the source container
      populateContainer(client);

      // Perform the backup
      RestResponse response = await(client.cluster().backup());
      assertEquals(200, response.getStatus());

      // Shutdown the source cluster
      source.stop("source");

      // Start the target cluster
      startTargetCluster();
      client = target.getClient();

      // Copy the returned zip bytes to the local working dir
      File backupZip = new File(WORKING_DIR, "backup.zip");
      try (InputStream is = response.getBodyAsStream()) {
         Files.copy(is, backupZip.toPath(), StandardCopyOption.REPLACE_EXISTING);
      }

      // Upload the backup to the target cluster
      response = await(client.cluster().restore(backupZip));
      assertEquals(response.getBody(), 201, response.getStatus());

      // Assert that all content has been restored as expected
      assertContainerContent(client);
   }

   private void populateContainer(RestClient client) throws Exception {
      String cacheName = "cache1";
      createCache(cacheName, new ConfigurationBuilder(), client);
      // Populate the container
      RestCacheClient cache = client.cache(cacheName);
      for (int i = 0; i < NUM_ENTRIES; i++) {
         join(cache.put(String.valueOf(i), String.valueOf(i)));
      }
      assertEquals(NUM_ENTRIES, getCacheSize(cacheName, client));

      createCounter("weak-volatile", "weak-counter", Storage.VOLATILE, client);
      createCounter("weak-persistent", "weak-counter", Storage.PERSISTENT, client);
      createCounter("strong-volatile", "strong-counter", Storage.VOLATILE, client);
      createCounter("strong-persistent", "strong-counter", Storage.PERSISTENT, client);

      addSchema(client);

      try (InputStream is = ClusterBackupIT.class.getResourceAsStream("/scripts/test.js")) {
         String script = CommonsTestingUtil.loadFileAsString(is);
         RestResponse rsp = await(client.tasks().uploadScript("test.js", RestEntity.create(MediaType.APPLICATION_JAVASCRIPT, script)));
         assertEquals(200, rsp.getStatus());
      }
   }

   private void assertContainerContent(RestClient client) throws Exception {
      String cacheName = "cache1";
      assertEquals(Integer.toString(NUM_ENTRIES), await(client.cache(cacheName).size()).getBody());

      // TODO finish validating counters
      RestResponse counterConfig = await(client.counter("weak-volatile").configuration());
      JsonNode jsonNode = MAPPER.readTree(counterConfig.getBody());

      RestResponse rsp = await(client.schemas().get("schema.proto"));
      assertEquals(200, rsp.getStatus());
      assertTrue(rsp.getBody().contains("message Person"));

      rsp = await(client.tasks().list(RestTaskClient.ResultType.USER));
      assertEquals(200, rsp.getStatus());
      ArrayNode json = (ArrayNode) MAPPER.readTree(rsp.getBody());
      assertEquals(1, json.size());
      assertEquals("test.js", json.get(0).get("name").asText());
   }

   private void createCounter(String name, String type, Storage storage, RestClient client) {
      String config = String.format("{\n" +
            "    \"%s\":{\n" +
            "        \"initial-value\":0,\n" +
            "        \"storage\":\"%s\"\n" +
            "    }\n" +
            "}", type, storage.toString());
      await(client.counter(name).create(RestEntity.create(MediaType.APPLICATION_JSON, config)));
   }
}
