package org.infinispan.persistence.marshaller;

import org.infinispan.metadata.EmbeddedMetadata;
import org.infinispan.metadata.Metadata;
import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.SerializationContext;

/**
 * // TODO: Document this
 *
 * @author remerson
 * @since 4.0
 */
public class MetadataMarshallerProvider implements SerializationContext.MarshallerProvider {
   @Override
   public BaseMarshaller<?> getMarshaller(String typeName) {
      if (typeName.equals("persistence.Metadata")) {
         return new EmbeddedMetadata.Marshaller();
      }
      return null;  // TODO: Customise this generated block
   }

   @Override
   public BaseMarshaller<?> getMarshaller(Class<?> javaClass) {
      if (Metadata.class.isAssignableFrom(javaClass)) {
         return new EmbeddedMetadata.Marshaller();
      }
      return null;  // TODO: Customise this generated block
   }
}
