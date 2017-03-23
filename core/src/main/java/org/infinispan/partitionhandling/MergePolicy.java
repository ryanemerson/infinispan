package org.infinispan.partitionhandling;

/**
 * @author Ryan Emerson
 * @since 9.0
 */
public enum MergePolicy {
   CUSTOM,
   PRIMARY_ALWAYS,
   PRIMARY_NON_NULL,
   VERSION_BASED;
}
