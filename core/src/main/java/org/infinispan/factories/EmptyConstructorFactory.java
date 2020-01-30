package org.infinispan.factories;

import static org.infinispan.util.logging.Log.CONTAINER;

import org.infinispan.commons.time.TimeService;
import org.infinispan.factories.annotations.DefaultFactoryFor;
import org.infinispan.factories.scopes.Scope;
import org.infinispan.factories.scopes.Scopes;
import org.infinispan.globalstate.GlobalConfigurationManager;
import org.infinispan.globalstate.GlobalStateManager;
import org.infinispan.globalstate.impl.GlobalConfigurationManagerImpl;
import org.infinispan.globalstate.impl.GlobalStateManagerImpl;
import org.infinispan.marshall.protostream.impl.SerializationContextRegistry;
import org.infinispan.marshall.protostream.impl.SerializationContextRegistryImpl;
import org.infinispan.remoting.inboundhandler.GlobalInboundInvocationHandler;
import org.infinispan.remoting.inboundhandler.InboundInvocationHandler;
import org.infinispan.topology.PersistentUUIDManager;
import org.infinispan.topology.PersistentUUIDManagerImpl;
import org.infinispan.util.EmbeddedTimeService;
import org.infinispan.util.concurrent.DataOperationOrderer;
import org.infinispan.util.logging.events.EventLogManager;
import org.infinispan.util.logging.events.impl.EventLogManagerImpl;
import org.infinispan.xsite.BackupReceiverRepository;
import org.infinispan.xsite.BackupReceiverRepositoryImpl;

/**
 * Factory for building global-scope components which have default empty constructors
 *
 * @author Manik Surtani
 * @author <a href="mailto:galder.zamarreno@jboss.com">Galder Zamarreno</a>
 * @since 4.0
 */
@DefaultFactoryFor(classes = {
      BackupReceiverRepository.class, EventLogManager.class,
      InboundInvocationHandler.class, PersistentUUIDManager.class,
      TimeService.class, DataOperationOrderer.class,
      GlobalStateManager.class, GlobalConfigurationManager.class,
      SerializationContextRegistry.class
})
@Scope(Scopes.GLOBAL)
public class EmptyConstructorFactory extends AbstractComponentFactory implements AutoInstantiableFactory {

   @Override
   @SuppressWarnings("unchecked")
   public Object construct(String componentName) {
      if (componentName.equals(BackupReceiverRepository.class.getName()))
         return new BackupReceiverRepositoryImpl();
      else if (componentName.equals(InboundInvocationHandler.class.getName()))
         return new GlobalInboundInvocationHandler();
      else if (componentName.equals(TimeService.class.getName()))
         return new EmbeddedTimeService();
      else if (componentName.equals(EventLogManager.class.getName()))
         return new EventLogManagerImpl();
      else if (componentName.equals(PersistentUUIDManager.class.getName()))
         return new PersistentUUIDManagerImpl();
      else if (componentName.equals(GlobalStateManager.class.getName()))
         return new GlobalStateManagerImpl();
      else if (componentName.equals(GlobalConfigurationManager.class.getName()))
         return new GlobalConfigurationManagerImpl();
      else if (componentName.equals(DataOperationOrderer.class.getName()))
         return new DataOperationOrderer();
      else if (componentName.equals(SerializationContextRegistry.class.getName()))
         return new SerializationContextRegistryImpl();

      throw CONTAINER.factoryCannotConstructComponent(componentName);
   }
}
