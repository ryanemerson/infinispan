package org.infinispan.tools.jdbc.migrator;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import org.infinispan.commons.marshall.StreamingMarshaller;
import org.infinispan.marshall.core.MarshalledEntry;
import org.infinispan.persistence.jdbc.JdbcUtil;
import org.infinispan.persistence.jdbc.configuration.JdbcStringBasedStoreConfiguration;
import org.infinispan.persistence.jdbc.connectionfactory.ConnectionFactory;
import org.infinispan.persistence.jdbc.connectionfactory.PooledConnectionFactory;
import org.infinispan.persistence.jdbc.logging.Log;
import org.infinispan.persistence.jdbc.table.management.TableManager;
import org.infinispan.persistence.jdbc.table.management.TableManagerFactory;
import org.infinispan.persistence.spi.PersistenceException;
import org.infinispan.util.logging.LogFactory;

/**
 * @author Ryan Emerson
 * @since 9.0
 */
class JdbcStoreReader implements Iterable<MarshalledEntry>, AutoCloseable {

   private static final Log log = LogFactory.getLog(JdbcStoreReader.class, Log.class);

   private final StoreType storeType;
   private final StreamingMarshaller marshaller;
   private final ConnectionFactory connectionFactory;
   private final TableManager tableManager;

   JdbcStoreReader(String cacheName, StoreType storeType, StreamingMarshaller marshaller,
                   JdbcStringBasedStoreConfiguration storeConfig) {
      this.storeType = storeType;
      this.marshaller = marshaller;

      PooledConnectionFactory connectionFactory = new PooledConnectionFactory();
      connectionFactory.start(storeConfig.connectionFactory(), Thread.currentThread().getContextClassLoader());
      this.connectionFactory = connectionFactory;

      tableManager = TableManagerFactory.getManager(connectionFactory, storeConfig);
      tableManager.setCacheName(cacheName);
   }

   @Override
   public void close() throws Exception {
      tableManager.stop();
      connectionFactory.stop();
   }

   public Iterator<MarshalledEntry> iterator() {
      switch (storeType) {
         case BINARY:
            return new BinaryIterator();
         case MIXED:
            // TODO return CombinedIterator that gets binary first, then String
         case STRING:
            // TODO return string it
            return null;
         default:
            throw new IllegalArgumentException("Unknown Store Type: " + storeType);
      }
   }

   class BinaryIterator implements Iterator<MarshalledEntry>, AutoCloseable {
      private Iterator<MarshalledEntry> iterator = Collections.emptyIterator();
      private Connection conn;
      private PreparedStatement ps;
      private ResultSet rs;
      private int numberOfRows = 0;
      private int rowIndex = 0;

      BinaryIterator() {
         Statement st = null;
         ResultSet countRs = null;
         try {
            conn = connectionFactory.getConnection();

            st = conn.createStatement();
            countRs = st.executeQuery(tableManager.getCountRowsSql());
            countRs.next();
            numberOfRows = countRs.getInt(1);

            ps = conn.prepareStatement(tableManager.getLoadAllRowsSql(), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            ps.setFetchSize(tableManager.getFetchSize());
            rs = ps.executeQuery();
         } catch (SQLException e) {
            throw new PersistenceException("SQL error while fetching all StoredEntries", e);
         } finally {
            JdbcUtil.safeClose(st);
            JdbcUtil.safeClose(countRs);
         }
      }

      @Override
      public boolean hasNext() {
         return iterator.hasNext() || rowIndex < numberOfRows;
      }

      @Override
      public MarshalledEntry next() {
         if (!iterator.hasNext()) {
            iterator = getNextBucketIterator();
         }
         rowIndex++;
         return iterator.next();
      }

      private Iterator<MarshalledEntry> getNextBucketIterator() {
         try {
            if (rs.next()) {
               InputStream inputStream = rs.getBinaryStream(1);
               Map<Object, MarshalledEntry> bucketEntries = unmarshallBucketEntries(inputStream);
               numberOfRows += bucketEntries.size() - 1; // Guaranteed that bucket size will never be 0
               return bucketEntries.values().iterator();
            } else {
               close();
               throw new NoSuchElementException();
            }
         } catch (SQLException e) {
            throw new PersistenceException("SQL error while fetching all StoredEntries", e);
         }
      }

      private Map<Object, MarshalledEntry> unmarshallBucketEntries(InputStream inputStream) {
         try {
            return (Map<Object, MarshalledEntry>) marshaller.objectFromInputStream(inputStream);
         } catch (IOException e) {
            log.ioErrorUnmarshalling(e);
            throw new PersistenceException("I/O error while unmarshalling from stream", e);
         } catch (ClassNotFoundException e) {
            log.unexpectedClassNotFoundException(e);
            throw new PersistenceException("*UNEXPECTED* ClassNotFoundException. This should not happen as Bucket class exists", e);
         }
      }

      @Override
      public void close() {
         JdbcUtil.safeClose(rs);
         JdbcUtil.safeClose(ps);
         connectionFactory.releaseConnection(conn);
      }
   }
}
