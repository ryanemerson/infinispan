package org.infinispan.conflict.impl;

import java.util.Map;
import java.util.stream.Stream;

import org.infinispan.conflict.ConflictManager;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.distribution.ch.ConsistentHash;
import org.infinispan.remoting.transport.Address;

/**
 * The interface used internally to resolve conflicts during split-brain heals.
 *
 * @author Ryan Emerson
 * @since 9.1
 */
public interface MergeConflictManager<K, V> extends ConflictManager<K, V> {
   Stream<Map<Address, InternalCacheEntry<K, V>>> getConflicts(ConsistentHash hash);

   void resolveConflicts(ConsistentHash mergeHash);
}
