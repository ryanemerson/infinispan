package org.infinispan.server.test.eviction;

import static org.infinispan.test.TestingUtil.loadFileAsString;
import static org.infinispan.util.concurrent.CompletableFutures.await;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.infinispan.arquillian.core.InfinispanResource;
import org.infinispan.arquillian.core.RemoteInfinispanServer;
import org.infinispan.arquillian.core.RunningServer;
import org.infinispan.arquillian.core.WithRunningServer;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.server.test.util.ClassRemoteCacheManager;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Basic integration tests for eviction on server
 *
 * @author vjuranek
 * @since 9.2
 */
@RunWith(Arquillian.class)
@WithRunningServer({@RunningServer(name = "eviction")})
public class EvictionIT {

   @InfinispanResource("eviction")
   RemoteInfinispanServer server1;

   private RemoteCacheManager remoteCacheManager;

   @ClassRule
   public static ClassRemoteCacheManager classRCM = new ClassRemoteCacheManager();

   @Before
   public void setUp() throws Exception {
      remoteCacheManager = classRCM.cacheRemoteCacheManager(server1);
   }

   @Test
   public void testPutAllEviction() {
      RemoteCache<String, String> rc = remoteCacheManager.getCache("object");
      rc.clear();
      Map<String, String> entries = new HashMap<>();
      entries.put("keyA", "A");
      entries.put("keyB", "B");
      entries.put("keyC", "C");
      entries.put("keyD", "D");

      rc.putAll(entries);
      assertEquals(3, rc.size());
   }

   @Test
   public void testPutAllAsyncEviction() throws Exception {
      RemoteCache<String, String> rc = remoteCacheManager.getCache("binary");
      rc.clear();

      Map<String, String> entries = new HashMap<>();
      entries.put("keyA", "A");
      entries.put("keyB", "B");
      entries.put("keyC", "C");
      entries.put("keyD", "D");
      await(rc.putAllAsync(entries));

      assertEquals(3, rc.size());
   }

   @Test
   public void testMultipleClients() {
      RemoteCache<String, String> rc1 = remoteCacheManager.getCache("off-heap");
      RemoteCache<String, String> rc2 = remoteCacheManager.getCache("off-heap");
      rc1.clear();

      for (int i = 0; i < 1000; i++) {
         rc1.put(String.format("key1_%d", i), String.format("value1_%d", i));
         rc2.put(String.format("key2_%d", i), String.format("value2_%d", i));
      }

      assertEquals(3, rc1.size());
      assertEquals(3, rc2.size());
   }

   @Test
   public void testEvictionInScript() throws IOException {
      addScripts("test.js");
      RemoteCache<String, String> rc = remoteCacheManager.getCache("object");
      rc.clear();
      rc.put("keyA", "A");
      rc.put("keyB", "B");
      rc.put("keyC", "C");

      Map<String, Object> parameters = new HashMap<>();
      parameters.put("key", "keyD");
      parameters.put("value", "D");
      int result = rc.execute("test.js", parameters);

      assertEquals(3, result);
      assertEquals("D", rc.get("keyD"));
   }

   private void addScripts(String script) throws IOException {
      RemoteCache<String, String> scriptCache = remoteCacheManager.getCache("___script_cache");
      try (InputStream in = this.getClass().getClassLoader().getResourceAsStream(script)) {
         scriptCache.put(script, loadFileAsString(in));
      }
   }
}
