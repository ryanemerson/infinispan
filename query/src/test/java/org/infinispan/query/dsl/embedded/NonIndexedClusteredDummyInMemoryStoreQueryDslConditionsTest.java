package org.infinispan.query.dsl.embedded;

import org.infinispan.commons.marshall.JavaSerializationMarshaller;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.persistence.dummy.DummyInMemoryStoreConfigurationBuilder;
import org.testng.annotations.Test;

/**
 * @author anistor@redhat.com
 * @since 7.0
 */
@Test(groups = "functional", testName = "query.dsl.embedded.NonIndexedClusteredDummyInMemoryStoreQueryDslConditionsTest")
public class NonIndexedClusteredDummyInMemoryStoreQueryDslConditionsTest extends NonIndexedQueryDslConditionsTest {

   @Override
   protected void createCacheManagers() throws Throwable {
      ConfigurationBuilder cfg = getDefaultClusteredCacheConfig(CacheMode.REPL_SYNC, true);
      cfg.clustering()
            .stateTransfer().fetchInMemoryState(true)
            .persistence()
            .addStore(DummyInMemoryStoreConfigurationBuilder.class)
               .purgeOnStartup(true);

      // ensure the data container contains minimal data so the store will need to be accessed to get the rest
      cfg.locking().concurrencyLevel(1).memory().size(1L);

//      GlobalConfigurationBuilder globalBuilder = new GlobalConfigurationBuilder().clusteredDefault();
//      globalBuilder.serialization().marshaller(new JavaSerializationMarshaller()).whiteList().addRegexp("org.*").addRegexp("java.*");
//      createClusteredCaches(2, globalBuilder, cfg, false);
      createClusteredCaches(2, DslSCI.INSTANCE, cfg);
   }
}
