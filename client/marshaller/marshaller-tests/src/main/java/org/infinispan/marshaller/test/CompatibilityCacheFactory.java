package org.infinispan.marshaller.test;

import static org.infinispan.client.hotrod.test.HotRodClientTestingUtil.killRemoteCacheManager;
import static org.infinispan.client.hotrod.test.HotRodClientTestingUtil.killServers;
import static org.infinispan.client.hotrod.test.HotRodClientTestingUtil.startHotRodServer;
import static org.infinispan.server.memcached.test.MemcachedTestingUtil.killMemcachedClient;
import static org.infinispan.server.memcached.test.MemcachedTestingUtil.killMemcachedServer;
import static org.infinispan.server.memcached.test.MemcachedTestingUtil.startMemcachedTextServer;
import static org.infinispan.test.TestingUtil.killCacheManagers;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collections;

import org.apache.commons.httpclient.HttpClient;
import org.infinispan.Cache;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.commons.api.BasicCacheContainer;
import org.infinispan.commons.api.Lifecycle;
import org.infinispan.commons.equivalence.Equivalence;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.rest.NettyRestServer;
import org.infinispan.rest.configuration.RestServerConfigurationBuilder;
import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.server.memcached.MemcachedServer;
import org.infinispan.test.fwk.TestCacheManagerFactory;

import net.spy.memcached.ConnectionFactory;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.DefaultConnectionFactory;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.transcoders.Transcoder;

/**
 * Compatibility cache factory taking care of construction and destruction of
 * caches, servers and clients for each of the endpoints being tested.
 *
 * @author Galder Zamarreño
 * @author Ryan Emerson
 */
final public class CompatibilityCacheFactory<K, V> {

   private EmbeddedCacheManager cacheManager;
   private HotRodServer hotrod;
   private RemoteCacheManager hotrodClient;
   private Lifecycle rest;
   private MemcachedServer memcached;

   private Cache<K, V> embeddedCache;
   private RemoteCache<K, V> hotrodCache;
   private HttpClient restClient;
   private MemcachedClient memcachedClient;
   private Transcoder transcoder;

   private final String cacheName;
   private final Marshaller marshaller;
   private final CacheMode cacheMode;
   private int restPort;
   private final int defaultNumOwners = 2;
   private int numOwners = defaultNumOwners;
   private boolean l1Enable = false;
   private Equivalence keyEquivalence = null;
   private Equivalence valueEquivalence = null;

   public CompatibilityCacheFactory(String cacheName, Marshaller marshaller, Transcoder transcoder, CacheMode cacheMode) {
      this.cacheName = cacheName;
      this.marshaller = marshaller;
      this.transcoder = transcoder;
      this.cacheMode = cacheMode;
   }

   public CompatibilityCacheFactory<K, V> setup() throws Exception {
      createEmbeddedCache();
      createHotRodCache();
      createRestMemcachedCaches();
      return this;
   }

   private void createRestMemcachedCaches() throws Exception {
      restPort = hotrod.getPort() + 20;
      final int memcachedPort = hotrod.getPort() + 40;
      createRestCache(restPort);
      createMemcachedCache(memcachedPort);
   }

   private void createEmbeddedCache() {
      org.infinispan.configuration.cache.ConfigurationBuilder builder =
            new org.infinispan.configuration.cache.ConfigurationBuilder();
      builder.clustering().cacheMode(cacheMode)
            .compatibility().enable().marshaller(marshaller);

      if (cacheMode.isDistributed() && numOwners != defaultNumOwners) {
         builder.clustering().hash().numOwners(numOwners);
      }

      if (cacheMode.isDistributed() && l1Enable) {
         builder.clustering().l1().enable();
      }

      if (keyEquivalence != null) {
         builder.dataContainer().keyEquivalence(keyEquivalence);
      }

      if (valueEquivalence != null) {
         builder.dataContainer().valueEquivalence(valueEquivalence);
      }

      cacheManager = cacheMode.isClustered()
            ? TestCacheManagerFactory.createClusteredCacheManager(builder)
            : TestCacheManagerFactory.createCacheManager(builder);

      embeddedCache = cacheName.isEmpty()
            ? cacheManager.getCache()
            : cacheManager.getCache(cacheName);
   }

   private void createHotRodCache() {
      createHotRodCache(startHotRodServer(cacheManager));
   }

   private void createHotRodCache(HotRodServer server) {
      hotrod = server;
      hotrodClient = new RemoteCacheManager(new ConfigurationBuilder()
            .addServers("localhost:" + hotrod.getPort())
            .marshaller(marshaller)
            .build());
      hotrodCache = cacheName.isEmpty()
            ? hotrodClient.getCache()
            : hotrodClient.getCache(cacheName);
   }

   private void createRestCache(int port) throws Exception {
      RestServerConfigurationBuilder builder = new RestServerConfigurationBuilder();
      builder.port(port);
      rest = NettyRestServer.createServer(builder.build(), cacheManager);
      rest.start();
      restClient = new HttpClient();
   }

   private void createMemcachedCache(int port) throws IOException {
      memcached = startMemcachedTextServer(cacheManager, port, cacheName);
      memcachedClient = createMemcachedClient(60000, memcached.getPort());
   }

   private MemcachedClient createMemcachedClient(long timeout, int port) throws IOException {
      ConnectionFactory cf = new DefaultConnectionFactory() {
         @Override
         public long getOperationTimeout() {
            return timeout;
         }
      };

      if (transcoder != null) {
         cf = new ConnectionFactoryBuilder(cf).setTranscoder(transcoder).build();
      }
      return new MemcachedClient(cf, Collections.singletonList(new InetSocketAddress("127.0.0.1", port)));
   }

   static void killCacheFactories(CompatibilityCacheFactory... cacheFactories) {
      if (cacheFactories != null) {
         for (CompatibilityCacheFactory cacheFactory : cacheFactories) {
            if (cacheFactory != null)
               cacheFactory.teardown();
         }
      }
   }

   private void teardown() {
      killRemoteCacheManager(hotrodClient);
      killServers(hotrod);
      killRestServer(rest);
      killMemcachedClient(memcachedClient);
      killMemcachedServer(memcached);
      killCacheManagers(cacheManager);
   }

   private void killRestServer(Lifecycle rest) {
      if (rest != null) {
         try {
            rest.stop();
         } catch (Exception e) {
            // Ignore
         }
      }
   }

   Cache<K, V> getEmbeddedCache() {
      return embeddedCache;
   }

   RemoteCache<K, V> getHotRodCache() {
      return hotrodCache;
   }

   HttpClient getRestClient() {
      return restClient;
   }

   MemcachedClient getMemcachedClient() {
      return memcachedClient;
   }

   public Marshaller getMarshaller() {
      return marshaller;
   }

   String getRestUrl() {
      String restCacheName = cacheName.isEmpty() ? BasicCacheContainer.DEFAULT_CACHE_NAME : cacheName;
      return String.format("http://localhost:%s/rest/%s", restPort, restCacheName);
   }
}
