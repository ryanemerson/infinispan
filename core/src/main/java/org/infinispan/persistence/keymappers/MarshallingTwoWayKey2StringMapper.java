package org.infinispan.persistence.keymappers;

import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.commons.marshall.StreamingMarshaller;

/**
 *
 * MarshallingTwoWayKey2StringMapper.
 *
 * @author Tristan Tarrant
 * @since 5.2
 */
public interface MarshallingTwoWayKey2StringMapper extends TwoWayKey2StringMapper {

   @Deprecated
   default void setMarshaller(StreamingMarshaller marshaller) {
      // no-op
   }

   default void setMarshaller(Marshaller marshaller) {
      // no-op
   }
}
