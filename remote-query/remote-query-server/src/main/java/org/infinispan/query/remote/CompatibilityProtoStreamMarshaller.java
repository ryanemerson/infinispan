package org.infinispan.query.remote;

import org.infinispan.commons.marshall.proto.ProtoStreamMarshaller;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.factories.scopes.Scope;
import org.infinispan.factories.scopes.Scopes;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.query.remote.impl.ProtobufMetadataManagerImpl;

/**
 * A per {@link EmbeddedCacheManager} marshaller that should be used as compatibility mode marshaller in server. An
 * instance cannot be shared between multiple cache managers.
 *
 * @author anistor@redhat.com
 * @since 6.0
 */
@Scope(Scopes.GLOBAL)
public class CompatibilityProtoStreamMarshaller extends ProtoStreamMarshaller {

   @Inject protected EmbeddedCacheManager cacheManager;

   public CompatibilityProtoStreamMarshaller() {
   }

   @Override
   public SerializationContext getSerializationContext() {
      if (cacheManager == null) {
         throw new IllegalStateException("cacheManager not set");
      }
      return ProtobufMetadataManagerImpl.getSerializationContext(cacheManager);
   }
}
