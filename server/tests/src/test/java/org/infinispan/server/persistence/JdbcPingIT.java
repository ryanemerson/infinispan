package org.infinispan.server.persistence;

import static org.infinispan.server.persistence.PersistenceIT.getJavaArchive;
import static org.infinispan.server.persistence.PersistenceIT.getJdbcDrivers;
import static org.infinispan.server.test.core.Common.sync;
import static org.infinispan.server.test.core.TestSystemPropertyNames.INFINISPAN_TEST_SERVER_CONTAINER_VOLUME_REQUIRED;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import org.infinispan.client.rest.RestClient;
import org.infinispan.client.rest.RestResponse;
import org.infinispan.commons.dataconversion.internal.Json;
import org.infinispan.commons.test.Eventually;
import org.infinispan.server.test.core.Common;
import org.infinispan.server.test.core.ServerRunMode;
import org.infinispan.server.test.core.category.Persistence;
import org.infinispan.server.test.core.persistence.ContainerDatabase;
import org.infinispan.server.test.core.persistence.DatabaseServerListener;
import org.infinispan.server.test.junit5.InfinispanServerExtension;
import org.infinispan.server.test.junit5.InfinispanServerExtensionBuilder;
import org.junit.experimental.categories.Category;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

@Category(Persistence.class)
public class JdbcPingIT {

   static final DatabaseServerListener DATABASE_LISTENER = new DatabaseServerListener("mysql");

   @RegisterExtension
   public static InfinispanServerExtension SERVERS =
         InfinispanServerExtensionBuilder.config(System.getProperty(JdbcPingIT.class.getName(), "configuration/JdbcPingTest.xml"))
               .numServers(2)
//               .numServers(1)
               .runMode(ServerRunMode.CONTAINER)
               .mavenArtifacts(getJdbcDrivers())
               .artifacts(getJavaArchive())
               .addListener(DATABASE_LISTENER)
               .property(INFINISPAN_TEST_SERVER_CONTAINER_VOLUME_REQUIRED, "true")
               .build();

   public static class DatabaseProvider implements ArgumentsProvider {
      @Override
      public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
         return Arrays.stream(DATABASE_LISTENER.getDatabaseTypes())
               .map(DATABASE_LISTENER::getDatabase)
               .filter(ContainerDatabase.class::isInstance)
               .map(Arguments::of);
      }
   }

   @Test
   public void testJDBCPing() {
      RestClient client = SERVERS.rest().get();
      try (RestResponse info = sync(client.container().info())) {
         assertEquals(200, info.status());
         Json json = Json.read(info.body());
         assertEquals(2, json.at("cluster_members").asJsonList().size());
      }
   }

   @ParameterizedTest
   @ArgumentsSource(DatabaseProvider.class)
   public void testDBConnectionLost(ContainerDatabase db) {
      Eventually.eventually(assertClusterMembersSize(1, 2));
//      Eventually.eventually(assertClusterMembersSize(0, 1));
      db.stop();
      System.out.println("stopped");
      // TODO wait for JDBC_PING failures
      db.restart();
      System.out.println("resumed");
   }

   @Test
   public void testMemberLeave() {
      // TODO how to make this quicker?
      var timeout = TimeUnit.MINUTES.toMillis(20);
      Eventually.eventually(assertClusterMembersSize(1, 2), timeout);
      SERVERS.getServerDriver().kill(0);
      Eventually.eventually(assertClusterMembersSize(1, 1), timeout);
      SERVERS.getServerDriver().restart(0);
      Eventually.eventually(assertClusterMembersSize(1, 2), timeout);
   }

   @Test
   public void testZombieProcess() {
      // Create cluster of 2
      // Gracefully kill 0
      // Kill -9 1
      // Start new server
      // Process hangs
      var timeout = TimeUnit.MINUTES.toMillis(20);
      SERVERS.getServerDriver().stop(0);
      Eventually.eventually(assertClusterMembersSize(1, 1), timeout);
      SERVERS.getServerDriver().kill(1);
      SERVERS.getServerDriver().restart(0);
      Eventually.eventually(assertClusterMembersSize(1, 1), timeout);
   }

   @Test
   public void testKeycloakIssue() {
      // https://github.com/keycloak/keycloak/issues/10776
   }

   private Eventually.Condition assertClusterMembersSize(int server, int expectedMembers) {
      RestClient client = SERVERS.rest().get(server);
      try (RestResponse info = sync(client.container().info())) {
         assertEquals(200, info.status());
         Json json = Json.read(info.body());
//         System.out.println(json.toPrettyString());
         return () -> json.at("cluster_members").asJsonList().size() == expectedMembers;
      }
   }
}
