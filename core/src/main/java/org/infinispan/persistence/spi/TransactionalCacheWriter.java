package org.infinispan.persistence.spi;

import org.infinispan.persistence.support.BatchModification;

import javax.transaction.Transaction;

/**
 * Defines the functionality of a transactional store.  This allows the implementing store to participate in the cache's 2PC
 * protocol to ensure consistency between the underlying store and the cache.
 *
 * @author Ryan Emerson
 * @since 9.0
 */
public interface TransactionalCacheWriter<K, V> extends AdvancedCacheWriter<K, V> {

   /**
    * Write modifications to the store in the prepare phase, as this is the only way we know the FINAL values of the entries.
    * This is required to handle scenarios where an objects value is changed after the put command has been executed, but
    * before the commit is called on the Tx.
    *
    * @param transaction the current transactional context.
    * @param batchModification an object containing the write/remove operations required for this transaction.
    * @throws PersistenceException if an error occurs when communicating/performing writes on the underlying store.
    */
   void prepareWithModifications(Transaction transaction, BatchModification batchModification) throws PersistenceException;

   /**
    * Commit the provided transaction's changes to the underlying store.
    *
    * @param transaction the current transactional context.
    */
   void commit(Transaction transaction);

   /**
    * Rollback the provided transaction's changes to the underlying store.
    *
    * @param transaction the current transactional context.
    */
   void rollback(Transaction transaction);
}
