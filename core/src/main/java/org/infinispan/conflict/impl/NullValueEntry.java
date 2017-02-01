package org.infinispan.conflict.impl;

import static org.infinispan.commons.util.Util.toStr;

import org.infinispan.container.entries.ImmortalCacheEntry;

/**
 * @author Ryan Emerson
 * @since 9.1
 */
class NullValueEntry extends ImmortalCacheEntry {
   NullValueEntry(Object key) {
      super(key, null);
   }

   @Override
   public String toString() {
      return "NullValueEntry{" +
            "key=" + toStr(key) +
            ", value=null}";
   }
}
