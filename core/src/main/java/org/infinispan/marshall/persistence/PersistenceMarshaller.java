package org.infinispan.marshall.persistence;

import java.io.IOException;

import org.infinispan.commons.CacheException;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.factories.KnownComponentNames;
import org.infinispan.factories.annotations.Start;
import org.infinispan.factories.scopes.Scope;
import org.infinispan.factories.scopes.Scopes;
import org.infinispan.marshall.core.AbstractProtostreamMarshaller;
import org.infinispan.marshall.core.GlobalMarshaller;
import org.infinispan.marshall.core.JBossUserMarshaller;
import org.infinispan.marshall.persistence.impl.PersistenceContext;
import org.infinispan.metadata.impl.InternalMetadataImpl;
import org.infinispan.protostream.SerializationContext;

/**
 * A Protostream based {@link org.infinispan.commons.marshall.StreamAwareMarshaller} implementation that is responsible
 * for marshalling/unmarshalling objects which are to be persisted.
 * <p>
 * Known internal objects, such as {@link InternalMetadataImpl}, are defined in "resources/persistence.proto" and are
 * marshalled using the provided {@link org.infinispan.protostream.MessageMarshaller} implementation. Non-core modules
 * can register additional {@link org.infinispan.protostream.MessageMarshaller} and .proto files via the {@link
 * SerializationContext} return by {@link #getSerializationContext()}.
 * <p>
 * If no {@link org.infinispan.protostream.MessageMarshaller} are registered for a provided object, then the marshalling
 * of said object is delegated to the user marshaller {@link org.infinispan.configuration.global.SerializationConfiguration#MARSHALLER}.
 * The bytes generated by the user marshaller are then wrapped in a {@link org.infinispan.marshall.core.AbstractProtostreamMarshaller.WrappedObject}
 * and marshalled by ProtoStream.
 *
 * @author Ryan Emerson
 * @since 10.0
 */
@Scope(Scopes.GLOBAL)
public class PersistenceMarshaller extends AbstractProtostreamMarshaller {

   private Marshaller userMarshaller;
   private SerializationContext blacklistContext;

   public PersistenceMarshaller() {
   }

   // Must be before PersistenceManager
   @Start(priority = 8)
   @Override
   public void start() {
      GlobalConfiguration globalConfig = gcr.getGlobalConfiguration();
      Marshaller marshaller = globalConfig.serialization().marshaller();
      if (marshaller == null) {
         marshaller = new JBossUserMarshaller(gcr);
      }
      marshaller.start();
      this.userMarshaller = marshaller;
      // We blacklist any classes in the global serialization context as they should not be persisted
      this.blacklistContext = gcr.getComponent(GlobalMarshaller.class, KnownComponentNames.INTERNAL_MARSHALLER).getSerializationContext();
      try {
         PersistenceContext.init(gcr, this);
      } catch (IOException e) {
         throw new CacheException("Exception encountered when initialising the PersistenceMarshaller SerializationContext", e);
      }
   }

   public Marshaller getUserMarshaller() {
      return userMarshaller;
   }

   @Override
   public boolean isMarshallable(Object o) {
      return !isBlacklisted(o) && (isPersistenceClass(o) || isUserMarshallable(o));
   }

   private boolean isBlacklisted(Object o) {
      return blacklistContext.canMarshall(o.getClass());
   }

   private boolean isPersistenceClass(Object o) {
      return getSerializationContext().canMarshall(o.getClass());
   }

   private boolean isUserMarshallable(Object o) {
      try {
         return userMarshaller.isMarshallable(o);
      } catch (Exception ignore) {
         return false;
      }
   }
}
