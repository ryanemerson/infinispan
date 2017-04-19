package org.infinispan.conflict;

import org.infinispan.commons.CacheConfigurationException;
import org.infinispan.container.entries.CacheEntry;

/**
 * @author Ryan Emerson
 * @since 9.1
 */
public class MergePolicies {

   public static final EntryMergePolicy PREFERRED_ALWAYS = (preferredEntry, otherEntries) -> preferredEntry;

   public static final EntryMergePolicy PREFERRED_NON_NULL = (preferredEntry, otherEntries) -> {
      if (preferredEntry != null || otherEntries.isEmpty()) return preferredEntry;

      return (CacheEntry) otherEntries.get(0);
   };

   public static EntryMergePolicy fromString(String policyName) {
      switch (policyName.toUpperCase()) {
         case "PREFERRED_ALWAYS":
            return PREFERRED_ALWAYS;
         case "PREFERRED_NON_NULL":
            return PREFERRED_NON_NULL;
         default:
            throw new CacheConfigurationException(String.format("Unknown merge policy %s", policyName));
      }
   }
}
