package org.infinispan.tools.store.migrator;

import static org.infinispan.tools.store.migrator.Element.SOURCE;

import java.util.Properties;
import java.util.function.Function;

import org.infinispan.tools.store.migrator.file.SingleFileStoreReader;
import org.infinispan.tools.store.migrator.file.SoftIndexFileStoreIterator;
import org.infinispan.tools.store.migrator.jdbc.JdbcStoreReader;
import org.infinispan.tools.store.migrator.rocksdb.RocksDBReader;

class StoreIteratorFactory {

   static StoreIterator get(Properties properties) {
      StoreProperties props = new StoreProperties(SOURCE, properties);
      StoreType type = props.storeType();
      switch (type) {
         case JDBC_BINARY:
         case JDBC_MIXED:
         case JDBC_STRING:
            return new JdbcStoreReader(props);
      }

      Function<StoreProperties, StoreIterator> factory = fileStoreFactory(props.storeType());
      if (props.isSegmented())
         return new SegmentedFileStoreReader(props, factory);

      return factory.apply(props);
   }

   private static Function<StoreProperties, StoreIterator> fileStoreFactory(StoreType type) {
      switch (type) {
         case LEVELDB:
         case ROCKSDB:
            return RocksDBReader::new;
         case SINGLE_FILE_STORE:
            return SingleFileStoreReader::new;
         case SOFT_INDEX_FILE_STORE:
            return SoftIndexFileStoreIterator::new;
      }
      throw new IllegalArgumentException();
   }
}
