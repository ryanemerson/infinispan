package org.infinispan.tools.jdbc.migrator;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.NoSuchElementException;

import org.infinispan.commons.io.ByteBuffer;
import org.infinispan.commons.marshall.StreamingMarshaller;
import org.infinispan.marshall.core.MarshalledEntry;
import org.infinispan.marshall.core.MarshalledEntryImpl;
import org.infinispan.persistence.jdbc.connectionfactory.ConnectionFactory;
import org.infinispan.persistence.jdbc.table.management.TableManager;
import org.infinispan.persistence.spi.PersistenceException;
import org.infinispan.util.KeyValuePair;

/**
 * @author Ryan Emerson
 * @since 9.0
 */
class StringJdbcIterator extends AbstractJdbcEntryIterator {

   public StringJdbcIterator(ConnectionFactory connectionFactory, TableManager tableManager, StreamingMarshaller marshaller) {
      super(connectionFactory, tableManager, marshaller);
   }

   @Override
   public boolean hasNext() {
      return rowIndex < numberOfRows;
   }

   @Override
   public MarshalledEntry next() {
      try {
         if (rs.next()) {
            rowIndex++;
            String key = rs.getString(2); // TODO involve mapper
            KeyValuePair<ByteBuffer, ByteBuffer> icv = unmarshall(rs.getBinaryStream(1));
            return new MarshalledEntryImpl(key, icv.getKey(), icv.getValue(), marshaller);
         } else {
            close();
            throw new NoSuchElementException();
         }
      } catch (SQLException e) {
         throw new PersistenceException("SQL error while fetching all StoredEntries", e);
      }
   }

   @SuppressWarnings("unchecked")
   private KeyValuePair<ByteBuffer, ByteBuffer> unmarshall(InputStream inputStream) throws PersistenceException {
      try {
         return (KeyValuePair<ByteBuffer, ByteBuffer>) marshaller.objectFromInputStream(inputStream);
      } catch (IOException e) {
         throw new PersistenceException("I/O error while unmarshalling from stream", e);
      } catch (ClassNotFoundException e) {
         throw new PersistenceException(e);
      }
   }
}
