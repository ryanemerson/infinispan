package org.infinispan.tools.jdbc.migrator;

import java.util.Iterator;

import org.infinispan.commons.marshall.StreamingMarshaller;
import org.infinispan.marshall.core.MarshalledEntry;
import org.infinispan.persistence.jdbc.connectionfactory.ConnectionFactory;
import org.infinispan.persistence.jdbc.table.management.TableManager;

/**
 * @author Ryan Emerson
 * @since 9.0
 */
class MixedJdbcIterator implements Iterator<MarshalledEntry>, AutoCloseable {
   BinaryJdbcIterator binaryIt;
   StringJdbcIterator stringIt;

   MixedJdbcIterator(ConnectionFactory connectionFactory, TableManager binaryTm, TableManager stringTm,
                     StreamingMarshaller marshaller) {
      binaryIt = new BinaryJdbcIterator(connectionFactory, binaryTm, marshaller);
      stringIt = new StringJdbcIterator(connectionFactory, stringTm, marshaller);
   }

   @Override
   public boolean hasNext() {
      return binaryIt.hasNext() || stringIt.hasNext();
   }

   @Override
   public MarshalledEntry next() {
      if (binaryIt.hasNext())
         return binaryIt.next();
      return stringIt.next();
   }

   @Override
   public void close() throws Exception {
      binaryIt.close();
      stringIt.close();
   }
}
