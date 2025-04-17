package org.infinispan.commands;

import org.infinispan.commons.CacheException;

public class UnsupportedCommandException extends RuntimeException {
   public UnsupportedCommandException(String message) {
      super(message);
   }
}
