package org.infinispan.partitionhandling;

/**
 * @author Ryan Emerson
 * @since 9.1
 */
public enum PartitionHandling {
   DENY_READ_WRITES, // If the partition does not have all owners for a given segment, both reads and writes are denied for all keys in that segment.
   ALLOW_READS, // Allows reads for a given key if it exists in this partition, but only allows writes if this partition contains all owners of a segment.
   ALLOW_READ_WRITES // Allow entries on each partition to diverge, with conflicts resolved during merge.
}
