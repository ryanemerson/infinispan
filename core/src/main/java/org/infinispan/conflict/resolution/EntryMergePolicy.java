package org.infinispan.conflict.resolution;

import org.infinispan.commons.util.Experimental;
import org.infinispan.container.entries.CacheEntry;

/**
 * The policy to be applied to detected conflicts on partition merges.
 *
 * @author Ryan Emerson
 */
@Experimental
public interface EntryMergePolicy<K, V> {
   CacheEntry<K, V> merge(CacheEntry<K, V> preferredEntry, CacheEntry<K, V> otherEntry);
}
