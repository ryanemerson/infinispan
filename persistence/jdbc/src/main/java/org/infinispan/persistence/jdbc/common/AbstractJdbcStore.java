package org.infinispan.persistence.jdbc.common;

import org.infinispan.commons.io.ByteBuffer;
import org.infinispan.persistence.jdbc.JdbcUtil;
import org.infinispan.persistence.jdbc.configuration.AbstractJdbcStoreConfiguration;
import org.infinispan.persistence.jdbc.connectionfactory.ConnectionFactory;
import org.infinispan.persistence.jdbc.connectionfactory.ManagedConnectionFactory;
import org.infinispan.persistence.jdbc.logging.Log;
import org.infinispan.persistence.jdbc.table.management.TableManager;
import org.infinispan.persistence.spi.AdvancedLoadWriteStore;
import org.infinispan.persistence.spi.InitializationContext;
import org.infinispan.persistence.spi.PersistenceException;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author Ryan Emerson
 */
public abstract class AbstractJdbcStore<K,V> implements AdvancedLoadWriteStore<K,V> {
   private final Log log;
   private AbstractJdbcStoreConfiguration configuration;
   protected ConnectionFactory connectionFactory;
   protected TableManager tableManager;
   protected InitializationContext ctx;
   protected String cacheName;

   protected abstract TableManager getTableManager();

   protected AbstractJdbcStore(Log log) {
      this.log = log;
   }

   @Override
   public void init(InitializationContext ctx) {
      this.ctx = ctx;
      this.configuration = ctx.getConfiguration();
      this.cacheName = ctx.getCache().getName();
   }

   @Override
   public void start() {
      if (configuration.manageConnectionFactory()) {
         ConnectionFactory factory = ConnectionFactory.getConnectionFactory(configuration.connectionFactory().connectionFactoryClass());
         factory.start(configuration.connectionFactory(), factory.getClass().getClassLoader());
         initializeConnectionFactory(factory);
      }
   }

   @Override
   public void stop() {
      Throwable cause = null;
      try {
         tableManager.stop();
      } catch (Throwable t) {
         cause = t.getCause();
         if (cause == null) cause = t;
         log.debug("Exception while stopping", t);
      }

      try {
         if (configuration.connectionFactory() instanceof ManagedConnectionFactory) {
            log.tracef("Stopping mananged connection factory: %s", connectionFactory);
            connectionFactory.stop();
         }
      } catch (Throwable t) {
         if (cause == null) {
            cause = t;
         } else {
            t.addSuppressed(cause);
         }
         log.debug("Exception while stopping", t);
      }
      if (cause != null) {
         throw new PersistenceException("Exceptions occurred while stopping store", cause);
      }
   }

   @Override
   public void clear() {
      Connection conn = null;
      Statement statement = null;
      try {
         String sql = tableManager.getDeleteAllRowsSql();
         conn = connectionFactory.getConnection();
         statement = conn.createStatement();
         int result = statement.executeUpdate(sql);
         if (log.isTraceEnabled()) {
            log.tracef("Successfully removed %d rows.", result);
         }
      } catch (SQLException ex) {
         log.failedClearingJdbcCacheStore(ex);
         throw new PersistenceException("Failed clearing cache store", ex);
      } finally {
         JdbcUtil.safeClose(statement);
         connectionFactory.releaseConnection(conn);
      }
   }

   /**
    * Keeps a reference to the connection factory for further use. Also initializes the {@link
    * TableManager} that needs connections. This method should be called when you don't
    * want the store to manage the connection factory, perhaps because it is using an shared connection factory: see
    * {@link org.infinispan.persistence.jdbc.stores.mixed.JdbcMixedStore} for such an example of this.
    */
   public void initializeConnectionFactory(ConnectionFactory connectionFactory) throws PersistenceException {
      this.connectionFactory = connectionFactory;
      tableManager = getTableManager();
      tableManager.setCacheName(cacheName);
      tableManager.start();
   }

   public ConnectionFactory getConnectionFactory() {
      return connectionFactory;
   }

   protected void waitForFutureToComplete(Future<Void> future) {
      try {
         future.get();
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
      } catch (ExecutionException e) {
         log.errorExecutingSubmittedStoreTask(e);
         throw new PersistenceException(e);
      }
   }

   protected ByteBuffer marshall(Object obj) throws PersistenceException, InterruptedException {
      try {
         return ctx.getMarshaller().objectToBuffer(obj);
      } catch (IOException e) {
         log.errorMarshallingObject(e, obj);
         throw new PersistenceException("I/O failure while marshalling object: " + obj, e);
      }
   }

   @SuppressWarnings("unchecked")
   protected <T> T unmarshall(InputStream inputStream) throws PersistenceException {
      try {
         return (T) ctx.getMarshaller().objectFromInputStream(inputStream);
      } catch (IOException e) {
         log.ioErrorUnmarshalling(e);
         throw new PersistenceException("I/O error while unmarshalling from stream", e);
      } catch (ClassNotFoundException e) {
         log.unexpectedClassNotFoundException(e);
         throw new PersistenceException("*UNEXPECTED* ClassNotFoundException. This should not happen as Bucket class exists", e);
      }
   }
}
