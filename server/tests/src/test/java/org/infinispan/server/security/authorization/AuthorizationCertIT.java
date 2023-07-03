package org.infinispan.server.security.authorization;

import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.rest.configuration.RestClientConfigurationBuilder;
import org.infinispan.server.functional.ClusteredIT;
import org.infinispan.server.test.api.TestUser;
import org.infinispan.server.test.core.ServerRunMode;
import org.infinispan.server.test.core.category.Security;
import org.infinispan.server.test.junit5.InfinispanServerExtension;
import org.infinispan.server.test.junit5.InfinispanServerExtensionBuilder;
import org.junit.experimental.categories.Category;
import org.junit.jupiter.api.extension.RegisterExtension;


/**
 * @author Ryan Emerson
 * @since 13.0
 */
@Category(Security.class)
public class AuthorizationCertIT extends AbstractAuthorization {

   @RegisterExtension
   public static InfinispanServerExtension SERVERS =
         InfinispanServerExtensionBuilder.config("configuration/AuthorizationCertTest.xml")
               .runMode(ServerRunMode.CONTAINER)
               .mavenArtifacts(ClusteredIT.mavenArtifacts())
               .artifacts(ClusteredIT.artifacts())
               .build();

   public AuthorizationCertIT() {
      super(SERVERS);
   }

   @Override
   protected InfinispanServerExtension getServers() {
      return SERVERS;
   }

   @Override
   protected void addClientBuilders(TestUser user) {
      ConfigurationBuilder hotRodBuilder = new ConfigurationBuilder();
      SERVERS.getServerDriver().applyTrustStore(hotRodBuilder, "ca.pfx");
      if (user == TestUser.ANONYMOUS) {
         SERVERS.getServerDriver().applyKeyStore(hotRodBuilder, "server.pfx");
      } else {
         SERVERS.getServerDriver().applyKeyStore(hotRodBuilder, user.getUser() + ".pfx");
      }
      hotRodBuilder.security()
            .authentication()
            .saslMechanism("EXTERNAL")
            .serverName("infinispan")
            .realm("default")
            .ssl().sniHostName("infinispan.test");

      hotRodBuilders.put(user, hotRodBuilder);

      RestClientConfigurationBuilder restBuilder = new RestClientConfigurationBuilder();
      SERVERS.getServerDriver().applyTrustStore(restBuilder, "ca.pfx");
      if (user == TestUser.ANONYMOUS) {
         SERVERS.getServerDriver().applyKeyStore(restBuilder, "server.pfx");
      } else {
         SERVERS.getServerDriver().applyKeyStore(restBuilder, user.getUser() + ".pfx");
      }
      restBuilder.security().authentication().ssl()
            .sniHostName("infinispan")
            .hostnameVerifier((hostname, session) -> true).connectionTimeout(120_000).socketTimeout(120_000);
      restBuilders.put(user, restBuilder);
   }

   protected String expectedServerPrincipalName(TestUser user) {
      return String.format("CN=%s,OU=Infinispan,O=JBoss,L=Red Hat", user.getUser());
   }
}
