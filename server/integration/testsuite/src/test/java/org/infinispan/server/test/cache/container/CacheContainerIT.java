package org.infinispan.server.test.cache.container;

import static org.infinispan.server.test.util.ITestUtils.SERVER1_MGMT_PORT;
import static org.junit.Assert.assertTrue;

import java.util.Scanner;
import javax.management.ObjectName;

import org.infinispan.arquillian.core.InfinispanResource;
import org.infinispan.arquillian.core.RemoteInfinispanServer;
import org.infinispan.arquillian.core.RunningServer;
import org.infinispan.arquillian.core.WithRunningServer;
import org.infinispan.arquillian.utils.MBeanServerConnectionProvider;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.commons.junit.Cleanup;
import org.infinispan.server.infinispan.spi.InfinispanSubsystem;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * - test start, default-cache, eviction-executor, listener-executor and replication-queue-executor attributes of cache-container element
 * - test the cache-container attribute of hotrod-connector, so that we can have different hotrod endpoints for different containers
 *
 * @author <a href="mailto:mgencur@redhat.com">Martin Gencur</a>
 * @author <a href="mailto:jmarkos@redhat.com">Jakub Markos</a>
 */
@RunWith(Arquillian.class)
@WithRunningServer({@RunningServer(name = "cachecontainer")})
public class CacheContainerIT {

    @InfinispanResource("cachecontainer")
    RemoteInfinispanServer server1;

    final String dumpServicesBean = "jboss.msc:type=container,name=jboss-as";
    final String dumpServicesOp = "dumpServicesToString";

   static RemoteCacheManager rcm1; // connects to 'default' cache container
   static RemoteCacheManager rcm2; // connects to 'special-cache-container' cache container

   @ClassRule
   public static Cleanup classCleanup = new Cleanup();

    @Before
    public void setUp() {
        if (rcm1 == null) {
            Configuration conf = new ConfigurationBuilder().addServer().host(server1.getHotrodEndpoint().getInetAddress().getHostName()).port(server1
                    .getHotrodEndpoint().getPort()).build();
            Configuration conf2 = new ConfigurationBuilder().addServer()
                    .host(server1.getHotrodEndpoint("hotrodconnector2").getInetAddress().getHostName())
                    .port(server1.getHotrodEndpoint("hotrodconnector2").getPort()).build();
            rcm1 = new RemoteCacheManager(conf);
            rcm2 = new RemoteCacheManager(conf2);
            classCleanup.add(rcm1, rcm2);
        }
    }

    @Test
    public void testEndpointConfiguration() throws Exception {
        RemoteCache<String, String> cache1 = rcm1.getCache("default");
        RemoteCache<String, String> cache2 = rcm2.getCache("other-cache");
        cache1.put("key", "value");
        cache2.put("key2", "value2");
        assertTrue(1 == server1.getCacheManager("default").getCache("default").getNumberOfEntries());
        assertTrue(1 == server1.getCacheManager("special-cache-container").getCache("other-cache").getNumberOfEntries());
        cache1.remove("key");
        cache2.remove("key2");
    }

    @Test
    public void testDefaultCacheAttribute() throws Exception {
        RemoteCache<String, String> cache = rcm2.getCache();
        cache.put("key", "value");
        assertTrue(1 == server1.getCacheManager("special-cache-container").getCache("special-cache").getNumberOfEntries());
        assertTrue(0 == server1.getCacheManager("special-cache-container").getCache("other-cache").getNumberOfEntries());
        cache.remove("key");
    }

    /*
     * test that eviction-executor, listener-executor and replication-queue-executor attributes have been picked up by
     * infinispan-subsystem and jboss.infinispan.default service should depend on these executors
     * also test the start attribute:
     * 'default' cache container has default start mode (=LAZY)  (mode of service - ACTIVE)
     * 'special-cache-container' has EAGER start mode  (mode of service - ON_DEMAND)
     */
    @Ignore
    @Test
    public void testExecutorAttributesAndStartMode() throws Exception {
       String hostName = server1.getHotrodEndpoint().getInetAddress().getHostName();
       MBeanServerConnectionProvider provider = new MBeanServerConnectionProvider(hostName, SERVER1_MGMT_PORT);
       String services = provider.getConnection()
                                 .invoke(new ObjectName(dumpServicesBean), dumpServicesOp, null, null)
                                 .toString();
        boolean b1 = false, b2 = false, b3 = false;
        final String executorPrefix = "jboss.thread.executor.";
        Scanner s = new Scanner(services).useDelimiter("\n");
        while (true) {
            try {
                String line = s.nextLine();
                if (line.contains("Service \"jboss." + InfinispanSubsystem.SUBSYSTEM_NAME + ".default.config\"") && line.contains("dependencies:")) {
                    String dependencies = line.substring(line.indexOf("dependencies:"));
                    if (dependencies.contains(executorPrefix + "test-infinispan-listener") &&
                          dependencies.contains(executorPrefix + "test-infinispan-eviction")) {
                        b1 = true;
                    }
                }
                if (line.contains("Service \"jboss." + InfinispanSubsystem.SUBSYSTEM_NAME + ".special-cache-container\"") &&
                        line.contains("mode ACTIVE state UP")) {
                    b2 = true;
                }
                if (line.contains("Service \"jboss." + InfinispanSubsystem.SUBSYSTEM_NAME + ".other-cache\"") &&
                        line.contains("mode ON_DEMAND state UP")) {
                    b3 = true;
                }

            } catch (Exception e) {
                break;
            }
        }
        assertTrue(b1 && b2 && b3);
    }
}
