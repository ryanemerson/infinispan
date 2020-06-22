package org.infinispan.server.functional;

import static org.infinispan.functional.FunctionalTestUtils.await;
import static org.infinispan.util.concurrent.CompletionStages.join;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.infinispan.client.rest.RestCacheClient;
import org.infinispan.client.rest.RestClient;
import org.infinispan.client.rest.RestResponse;
import org.infinispan.commons.test.CommonsTestingUtil;
import org.infinispan.commons.util.Util;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Ryan Emerson
 * @since 11.0
 */
public class ClusterBackupIT extends AbstractMultiClusterIT {

   static final File WORKING_DIR = new File(CommonsTestingUtil.tmpDirectory(ClusterBackupIT.class));

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
      RestClient restClientSource = source.getClient();
      RestClient restClientTarget = target.getClient();

      String cacheName = "cache1";
      createCache(cacheName, new ConfigurationBuilder(), restClientSource);
      populateSourceCluster(cacheName, restClientSource);

      RestResponse response = await(restClientSource.cluster().backup());
      assertEquals(200, response.getStatus());
      File backupZip = new File(WORKING_DIR, "backup.zip");
      try (InputStream is = response.getBodyAsStream()) {
         Files.copy(is, backupZip.toPath(), StandardCopyOption.REPLACE_EXISTING);
      }
      response = await(restClientTarget.cluster().restore(backupZip));
      assertEquals(response.getBody(), 201, response.getStatus());
   }

   private void populateSourceCluster(String cacheName, RestClient client) {
      RestCacheClient cache = client.cache(cacheName);
      int entries = 100;
      for (int i = 0; i < entries; i++) {
         join(cache.put(String.valueOf(i), String.valueOf(i)));
      }
      assertEquals(entries, getCacheSize(cacheName, client));
   }
}
