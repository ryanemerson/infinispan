package org.infinispan.remoting.responses;

import java.util.Collection;
import java.util.Map;

import org.infinispan.commons.util.IntSet;

/**
 * A successful response
 *
 * @author Manik Surtani
 * @since 4.0
 */
public interface SuccessfulResponse<T> extends ValidResponse<T> {

   SuccessfulResponse SUCCESSFUL_EMPTY_RESPONSE = new SuccessfulObjResponse<>(null);

   static SuccessfulResponse<?> create(Object rv) {
      if (rv == null)
         return SUCCESSFUL_EMPTY_RESPONSE;

      if (rv instanceof Collection<?> collection && !(rv instanceof IntSet))
         return new SuccessfulCollectionResponse<>(collection);

      if (rv.getClass().isArray()) {
         if (rv instanceof byte[] bytes) {
            return new SuccessfulBytesResponse(bytes);
         }
         // suppress unchecked
         return new SuccessfulArrayResponse<>((Object[]) rv);
      }
      if (rv instanceof Map<?, ?> map)
         return new SuccessfulMapResponse<>(map);

      return new SuccessfulObjResponse<>(rv);
   }
}
