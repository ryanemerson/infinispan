package org.infinispan.conflict;

/**
 * @author Ryan Emerson
 * @since 9.1
 */
public enum MergePolicy {
   CUSTOM,
   PREFERRED_ALWAYS,
   PREFERRED_NON_NULL,
   VERSION_BASED;
}
