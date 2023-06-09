package org.infinispan.server.security.authorization;

import org.infinispan.server.functional.ClusteredIT;
import org.infinispan.server.test.core.ServerRunMode;
import org.infinispan.server.test.junit5.InfinispanServerExtension;
import org.infinispan.server.test.junit5.InfinispanServerExtensionBuilder;
import org.infinispan.server.test.junit5.InfinispanSuite;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 11.0
 **/
@Suite
@SelectClasses({AuthorizationPropertiesIT.HotRod.class, AuthorizationPropertiesIT.Resp.class, AuthorizationPropertiesIT.Rest.class})
public class AuthorizationPropertiesIT extends InfinispanSuite {

   @RegisterExtension
   public static InfinispanServerExtension SERVERS =
         InfinispanServerExtensionBuilder.config("configuration/AuthorizationPropertiesTest.xml")
               .runMode(ServerRunMode.CONTAINER)
               .mavenArtifacts(ClusteredIT.mavenArtifacts())
               .artifacts(ClusteredIT.artifacts())
               .build();

   static class HotRod extends HotRodAuthorizationTest {
      @RegisterExtension
      static InfinispanServerExtension SERVERS = AuthorizationPropertiesIT.SERVERS;

      public HotRod() {
         super(SERVERS);
      }
   }

   static class Rest extends RESTAuthorizationTest {
      @RegisterExtension
      static InfinispanServerExtension SERVERS = AuthorizationPropertiesIT.SERVERS;

      public Rest() {
         super(SERVERS);
      }
   }

   static class Resp extends RESPAuthorizationTest {
      @RegisterExtension
      static InfinispanServerExtension SERVERS = AuthorizationPropertiesIT.SERVERS;
      public Resp() {
         super(SERVERS);
      }
   }
}
