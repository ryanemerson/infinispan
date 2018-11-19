package org.infinispan.marshall.core;

import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.factories.KnownComponentNames;
import org.infinispan.factories.annotations.ComponentName;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.factories.annotations.Start;
import org.infinispan.factories.annotations.Stop;
import org.infinispan.factories.scopes.Scope;
import org.infinispan.factories.scopes.Scopes;
import org.infinispan.marshall.persistence.PersistenceMarshaller;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * A globally-scoped marshaller. This is needed so that the transport layer can unmarshall requests even before it's
 * known which cache's marshaller can do the job.
 *
 * @author Galder Zamarreño
 * @since 5.0
 */
@Scope(Scopes.GLOBAL)
public class GlobalMarshaller extends AbstractProtostreamMarshaller {

   private static final Log log = LogFactory.getLog(GlobalMarshaller.class);
   private static final boolean trace = log.isTraceEnabled();

   @Inject @ComponentName(KnownComponentNames.PERSISTENCE_MARSHALLER)
   private PersistenceMarshaller persistenceMarshaller;

   public GlobalMarshaller() {
      // TODO register wrapper
      // ctx.registerMarshaller(new GlobalMarshaller.WrappedObjectMarshaller("wrappedObject", "core.PersistenceObject", persistenceMarshaller));
   }

   @Override
   @Start(priority = 8) // Should start after the externalizer table and before transport
   public void start() {
      GlobalConfiguration globalCfg = gcr.getGlobalConfiguration();
   }

   @Override
   @Stop(priority = 130) // Stop after transport to avoid send/receive and marshaller not being ready
   public void stop() {
   }


   @Override
   public boolean isMarshallable(Object o) {
      return getSerializationContext().canMarshall(o.getClass()) || persistenceMarshaller.isMarshallable(o);
   }
}
