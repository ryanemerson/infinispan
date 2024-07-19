package org.infinispan.server.persistence;

import static org.infinispan.server.test.core.TestSystemPropertyNames.INFINISPAN_TEST_CONTAINER_DATABASE_PROPERTIES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.infinispan.server.test.core.category.Persistence;
import org.infinispan.server.test.core.persistence.ContainerDatabase;
import org.jgroups.Event;
import org.jgroups.JChannel;
import org.jgroups.protocols.FD_ALL3;
import org.jgroups.protocols.FD_SOCK2;
import org.jgroups.protocols.FRAG4;
import org.jgroups.protocols.JDBC_PING;
import org.jgroups.protocols.MERGE3;
import org.jgroups.protocols.MFC;
import org.jgroups.protocols.PingData;
import org.jgroups.protocols.RED;
import org.jgroups.protocols.TCP;
import org.jgroups.protocols.UFC;
import org.jgroups.protocols.UNICAST3;
import org.jgroups.protocols.VERIFY_SUSPECT2;
import org.jgroups.protocols.pbcast.GMS;
import org.jgroups.protocols.pbcast.NAKACK2;
import org.jgroups.protocols.pbcast.STABLE;
import org.jgroups.stack.Protocol;
import org.jgroups.stack.ProtocolStack;
import org.junit.experimental.categories.Category;
import org.junit.jupiter.api.Test;

@Category(Persistence.class)
public class JGroupsJdbcPingIT {

   @Test
   public void testDBConnectionLost() throws Exception {
      ContainerDatabase db = initDatabase("mysql");
      db.start();

      var clusterName = "test";
      var datasource = new DummyDataSource(db.jdbcUrl(), db.username(), db.password());
      var c1 = createChannel(7800, datasource);
      var c2 = createChannel(7801, datasource);
      c1.connect(clusterName);
      c2.connect(clusterName);

      assertEquals(2, c1.view().size());
      assertEquals(2, c2.view().size());

      db.stop();
      CountDownLatch reqLatch = new CountDownLatch(1);
      CountDownLatch successLatch = new CountDownLatch(2);
      c1.getProtocolStack().insertProtocol(new DiscoveryListener(reqLatch, successLatch), ProtocolStack.Position.ABOVE, JDBC_PING.class);
      assertTrue(reqLatch.await(10, TimeUnit.MINUTES));
      assertEquals(2, successLatch.getCount());
      db.restart();
      assertTrue(successLatch.await(10, TimeUnit.MINUTES));
   }

   private ContainerDatabase initDatabase(String databaseType) {
      return new ContainerDatabase(databaseType, properties(databaseType));
   }

   private Properties properties(String databaseType) {
      String property = System.getProperty(INFINISPAN_TEST_CONTAINER_DATABASE_PROPERTIES);
      try (InputStream inputStream = property != null ? Files.newInputStream(Paths.get(property).resolve(databaseType + ".properties")) : getClass().getResourceAsStream(String.format("/database/%s.properties", databaseType))) {
         Properties properties = new Properties();
         properties.load(inputStream);
         return properties;
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   private JChannel createChannel(int port, DataSource dataSource) throws Exception {
      return new JChannel(
            new TCP().setBindAddr(InetAddress.getLocalHost()).setBindPort(port),
            new RED(),
            new JDBC_PING().setDataSource(dataSource),
            new MERGE3().setMinInterval(1000).setMaxInterval(30000),
            new FD_SOCK2().setOffset(50000),
            new FD_ALL3(),
            new VERIFY_SUSPECT2().setTimeout(1000),
            new NAKACK2(),
            new UNICAST3(),
            new STABLE(),
            new GMS(),
            new UFC(),
            new MFC(),
            new FRAG4()
      );
   }

   static class DiscoveryListener extends Protocol {

      final CountDownLatch reqLatch;
      final CountDownLatch successLatch;

      public DiscoveryListener(CountDownLatch reqLatch, CountDownLatch successLatch) {
         this.reqLatch = reqLatch;
         this.successLatch = successLatch;
      }

      @Override
      public Object down(Event evt) {
         if (evt.getType() == Event.FIND_MBRS_ASYNC) {
            reqLatch.countDown();
            Consumer<PingData> callbackWrapper = p -> {
               // The reqLatch should have been satisfied before the DB was restarted and PingData responses received
               assert reqLatch.getCount() == 0;
               successLatch.countDown();
               Consumer<PingData> discovery_rsp_callback = evt.getArg();
               discovery_rsp_callback.accept(p);
            };
            return down_prot.down(new Event(Event.FIND_MBRS_ASYNC, callbackWrapper));
         }
         return down_prot.down(evt);
      }
   }

   record DummyDataSource(String connectionUrl, String username, String password) implements DataSource {


      @Override
      public Connection getConnection() throws SQLException {
         return getConnection(username, password);
      }

      @Override
      public Connection getConnection(String username, String password) throws SQLException {
         return DriverManager.getConnection(connectionUrl, username, password);
      }

      @Override
      public PrintWriter getLogWriter() throws SQLException {
         throw new IllegalStateException("This should not be called!");
      }

      @Override
      public void setLogWriter(PrintWriter out) throws SQLException {
         throw new IllegalStateException("This should not be called!");
      }

      @Override
      public void setLoginTimeout(int seconds) throws SQLException {
         throw new IllegalStateException("This should not be called!");
      }

      @Override
      public int getLoginTimeout() throws SQLException {
         throw new IllegalStateException("This should not be called!");
      }

      @Override
      public <T> T unwrap(Class<T> iface) throws SQLException {
         throw new IllegalStateException("This should not be called!");
      }

      @Override
      public boolean isWrapperFor(Class<?> iface) throws SQLException {
         throw new IllegalStateException("This should not be called!");
      }

      public Logger getParentLogger() {
         throw new IllegalStateException("This should not be called!");
      }
   }
}
