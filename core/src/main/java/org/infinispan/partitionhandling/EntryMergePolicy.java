package org.infinispan.partitionhandling;

import org.infinispan.container.entries.CacheEntry;

/**
 * @author Ryan Emerson
 * @since 9.0
 */
public interface EntryMergePolicy {
   CacheEntry merge(CacheEntry preferredEntry, CacheEntry otherEntry);
}
