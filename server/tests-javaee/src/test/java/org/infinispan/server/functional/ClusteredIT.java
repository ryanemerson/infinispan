package org.infinispan.server.functional;

import org.infinispan.server.test.core.ServerRunMode;
import org.infinispan.server.test.junit4.InfinispanServerRule;
import org.infinispan.server.test.junit4.InfinispanServerRuleBuilder;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses(JCacheOperations.class)
public class ClusteredIT {
    @ClassRule
    public static final InfinispanServerRule SERVERS =
            InfinispanServerRuleBuilder.config("configuration/ClusteredServerTest.xml")
                    .numServers(2)
                    .runMode(ServerRunMode.CONTAINER)
                    .build();
}
