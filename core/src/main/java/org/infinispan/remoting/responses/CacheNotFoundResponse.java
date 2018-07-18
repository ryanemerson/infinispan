package org.infinispan.remoting.responses;

import java.io.IOException;
import java.util.Set;

import org.infinispan.commons.marshall.AbstractExternalizer;
import org.infinispan.commons.marshall.UserObjectInput;
import org.infinispan.commons.marshall.UserObjectOutput;
import org.infinispan.commons.util.Util;
import org.infinispan.marshall.core.Ids;

/**
 * A response that signals the named cache is not running on the target node.
 *
 * @author Dan Berindei
 * @since 6.0
 */
public class CacheNotFoundResponse extends InvalidResponse {
   public static final CacheNotFoundResponse INSTANCE = new CacheNotFoundResponse();

   private CacheNotFoundResponse() {
   }

   public static class Externalizer extends AbstractExternalizer<CacheNotFoundResponse> {
      @Override
      public void writeObject(UserObjectOutput output, CacheNotFoundResponse response) throws IOException {
      }

      @Override
      public CacheNotFoundResponse readObject(UserObjectInput input) throws IOException, ClassNotFoundException {
         return INSTANCE;
      }

      @Override
      public Integer getId() {
         return Ids.CACHE_NOT_FOUND_RESPONSE;
      }

      @Override
      public Set<Class<? extends CacheNotFoundResponse>> getTypeClasses() {
         return Util.<Class<? extends CacheNotFoundResponse>>asSet(CacheNotFoundResponse.class);
      }
   }
}
