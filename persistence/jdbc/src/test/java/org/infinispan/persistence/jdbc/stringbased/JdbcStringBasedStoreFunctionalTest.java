package org.infinispan.persistence.jdbc.stringbased;

import org.infinispan.configuration.cache.PersistenceConfigurationBuilder;
import org.infinispan.persistence.BaseStoreFunctionalTest;
import org.infinispan.persistence.jdbc.configuration.JdbcStringBasedStoreConfigurationBuilder;
import org.infinispan.test.fwk.UnitTestDatabaseManager;
import org.testng.annotations.Test;

@Test(groups = {"functional", "smoke"}, testName = "persistence.jdbc.stringbased.JdbcStringBasedStoreFunctionalTest")
public class JdbcStringBasedStoreFunctionalTest extends BaseStoreFunctionalTest {

   @Override
   protected PersistenceConfigurationBuilder createCacheStoreConfig(PersistenceConfigurationBuilder persistence, boolean preload, boolean preloadOnly) {
      JdbcStringBasedStoreConfigurationBuilder store = persistence
         .addStore(JdbcStringBasedStoreConfigurationBuilder.class)
         .preload(preload)
         .preloadOnly(preloadOnly);
      UnitTestDatabaseManager.buildTableManipulation(store.table());
      UnitTestDatabaseManager.configureTestConnectionFactory(store);
      return persistence;
   }
}
