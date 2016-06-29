package org.infinispan.persistence.jdbc;

import org.infinispan.persistence.jdbc.configuration.ConnectionFactoryConfiguration;
import org.infinispan.persistence.jdbc.configuration.ConnectionFactoryConfigurationBuilder;
import org.infinispan.persistence.jdbc.configuration.JdbcStringBasedStoreConfigurationBuilder;
import org.infinispan.persistence.jdbc.connectionfactory.PooledConnectionFactory;
import org.infinispan.persistence.spi.PersistenceException;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.infinispan.test.fwk.UnitTestDatabaseManager;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.HashSet;
import java.util.Set;

/**
 * Tester class for {@link org.infinispan.persistence.jdbc.connectionfactory.PooledConnectionFactory}.
 *
 * @author Mircea.Markus@jboss.com
 * @author Tristan Tarrant
 */
@Test(groups = "functional", testName = "persistence.jdbc.PooledConnectionFactoryTest")
public class PooledConnectionFactoryTest {

   private PooledConnectionFactory factory;
   private JdbcStringBasedStoreConfigurationBuilder storeBuilder;
   private ConnectionFactoryConfigurationBuilder<?> factoryBuilder;

   @BeforeMethod
   public void beforeMethod() {
      factory = new PooledConnectionFactory();
   }

   @AfterMethod
   public void destroyFactory() {
      factory.stop();
      System.setProperty("c3p0.force", "false");
   }

   @Test
   public void testHikariValuesNoOverrides() throws Exception {
      testValuesNoOverrides();
   }

   @Test(groups = "unstable", description = "See ISPN-3522")
   public void testC3P0ValuesNoOverrides() throws Exception {
      System.setProperty("c3p0.force", "true");
      testValuesNoOverrides();
   }

   private void testValuesNoOverrides() throws Exception {
      storeBuilder = TestCacheManagerFactory
            .getDefaultCacheConfiguration(false)
            .persistence()
            .addStore(JdbcStringBasedStoreConfigurationBuilder.class);
      factoryBuilder = UnitTestDatabaseManager.configureUniqueConnectionFactory(storeBuilder);
      ConnectionFactoryConfiguration factoryConfiguration = factoryBuilder.create();
      factory.start(factoryConfiguration, Thread.currentThread().getContextClassLoader());

      int hadcodedMaxPoolSize = factory.getMaxPoolSize();
      Set<Connection> connections = new HashSet<>();
      for (int i = 0; i < hadcodedMaxPoolSize; i++) {
         connections.add(factory.getConnection());
      }
      assert connections.size() == hadcodedMaxPoolSize;
      assert factory.getNumBusyConnectionsAllUsers() == hadcodedMaxPoolSize;
      for (Connection conn : connections) {
         conn.close();
      }
      long start = System.currentTimeMillis();
      while (System.currentTimeMillis() - start < 2000) {
         if (factory.getNumBusyConnectionsAllUsers() == 0)
            break;
      }
      //this must happen eventually
      assert factory.getNumBusyConnectionsAllUsers() == 0;
   }

   @Test(expectedExceptions = PersistenceException.class)
   public void testC3PONoDriverClassFound() throws Exception {
      System.setProperty("c3p0.force", "true");
      testNoDriverClassFound();
   }

   @Test(expectedExceptions = PersistenceException.class)
   public void testHikariCPNoDriverClassFound() throws Exception {
      testNoDriverClassFound();
   }

   private void testNoDriverClassFound() throws Exception {
      storeBuilder = TestCacheManagerFactory
            .getDefaultCacheConfiguration(false)
            .persistence()
            .addStore(JdbcStringBasedStoreConfigurationBuilder.class);
      factoryBuilder = UnitTestDatabaseManager.configureBrokenConnectionFactory(storeBuilder);
      ConnectionFactoryConfiguration factoryConfiguration = factoryBuilder.create();
      factory.start(factoryConfiguration, Thread.currentThread().getContextClassLoader());
   }

   @Test
   public void testHikariCPLoaded() throws Exception {
      storeBuilder = TestCacheManagerFactory
            .getDefaultCacheConfiguration(false)
            .persistence()
            .addStore(JdbcStringBasedStoreConfigurationBuilder.class);
      factoryBuilder = UnitTestDatabaseManager.configureUniqueConnectionFactory(storeBuilder);
      ConnectionFactoryConfiguration factoryConfiguration = factoryBuilder.create();
      factory.start(factoryConfiguration, Thread.currentThread().getContextClassLoader());

      Field hikariPool = factory.getClass().getDeclaredField("hikari");
      hikariPool.setAccessible(true);
      assert hikariPool.get(factory) != null;
   }

   @Test
   public void testC3POLoaded() throws Exception {
      System.setProperty("c3p0.force", "true");
      storeBuilder = TestCacheManagerFactory
            .getDefaultCacheConfiguration(false)
            .persistence()
            .addStore(JdbcStringBasedStoreConfigurationBuilder.class);
      factoryBuilder = UnitTestDatabaseManager.configureUniqueConnectionFactory(storeBuilder);
      ConnectionFactoryConfiguration factoryConfiguration = factoryBuilder.create();
      factory.start(factoryConfiguration, Thread.currentThread().getContextClassLoader());

      Field c3p0Pool = factory.getClass().getDeclaredField("c3p0");
      c3p0Pool.setAccessible(true);
      assert c3p0Pool.get(factory) != null;
   }
}
