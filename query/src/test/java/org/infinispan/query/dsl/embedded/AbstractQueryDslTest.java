package org.infinispan.query.dsl.embedded;

import static org.infinispan.configuration.cache.IndexStorage.LOCAL_HEAP;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.infinispan.Cache;
import org.infinispan.commons.api.BasicCache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.query.Search;
import org.infinispan.query.dsl.QueryFactory;
import org.infinispan.query.dsl.embedded.testdomain.ModelFactory;
import org.infinispan.query.dsl.embedded.testdomain.hsearch.ModelFactoryHS;
import org.infinispan.test.MultipleCacheManagersTest;
import org.infinispan.test.fwk.CleanupAfterTest;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.infinispan.transaction.TransactionMode;

/**
 * Base for the DSL query tests.
 *
 * @author rvansa@redhat.com
 * @author anistor@redhat.com
 * @since 6.0
 */
@CleanupAfterTest
public abstract class AbstractQueryDslTest extends MultipleCacheManagersTest {

   protected final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

   protected AbstractQueryDslTest() {
      DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
   }

   protected Date makeDate(String dateStr) throws ParseException {
      return DATE_FORMAT.parse(dateStr);
   }

   /**
    * To be overridden by subclasses.
    */
   protected BasicCache<Object, Object> getCacheForWrite() {
      return getCacheForQuery();
   }

   /**
    * To be overridden by subclasses.
    */
   protected BasicCache<Object, Object> getCacheForQuery() {
      return cache(0);
   }

   /**
    * To be overridden by subclasses that need a different query factory.
    */
   protected QueryFactory getQueryFactory() {
      return Search.getQueryFactory((Cache) getCacheForQuery());
   }

   /**
    * To be overridden by subclasses if they need to use a different model implementation.
    */
   protected ModelFactory getModelFactory() {
      return ModelFactoryHS.INSTANCE;
   }

   @Override
   protected void createCacheManagers() throws Throwable {
      ConfigurationBuilder cfg = TestCacheManagerFactory.getDefaultCacheConfiguration(true);
      cfg.transaction()
            .transactionMode(TransactionMode.TRANSACTIONAL)
            .indexing().enable()
            .storage(LOCAL_HEAP)
            .addIndexedEntity(getModelFactory().getUserImplClass())
            .addIndexedEntity(getModelFactory().getAccountImplClass())
            .addIndexedEntity(getModelFactory().getTransactionImplClass());
      createClusteredCaches(1, cfg);
   }

   @Override
   protected void clearContent() {
      // Don't clear, this is destroying the index
   }
}
