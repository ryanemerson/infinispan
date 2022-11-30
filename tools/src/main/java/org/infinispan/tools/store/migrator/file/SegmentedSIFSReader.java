package org.infinispan.tools.store.migrator.file;

import java.nio.file.Path;

import org.infinispan.tools.store.migrator.AbstractSegmentedFileStoreReader;
import org.infinispan.tools.store.migrator.StoreIterator;
import org.infinispan.tools.store.migrator.StoreProperties;

public class SegmentedSIFSReader extends AbstractSegmentedFileStoreReader {
   public SegmentedSIFSReader(StoreProperties properties) {
      super(properties);
   }

   public StoreIterator newIterator(StoreProperties properties) {
      return new SoftIndexFileStoreIterator(properties);
   }

   public Path segmentLocation(String cache, int segment, Path root) {
      return root.resolve(Integer.toString(segment))
            .resolve(cache)
            .resolve("data");
   }
}
