package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.PathAddress.pathAddress;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.Optional;

import org.infinispan.counter.api.CounterManager;
import org.infinispan.counter.api.Storage;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.server.infinispan.SecurityActions;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.services.path.ResolvePathHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Resource description for the addressable resource
 * /subsystem=infinispan/cache-container=X/counter=*
 *
 * @author Pedro Ruivo
 * @author Vladimir Blagojevic
 * @since 9.2
 */
public class CounterConfigurationResource extends SimpleResourceDefinition {

   //atributes
   static final SimpleAttributeDefinition COUNTER_NAME = new SimpleAttributeDefinitionBuilder(ModelKeys.NAME,
         ModelType.STRING, false)
         .setXmlName(Attribute.NAME.getLocalName())
         .setAllowExpression(false)
         .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
         .build();

   static final SimpleAttributeDefinition STORAGE = new SimpleAttributeDefinitionBuilder(ModelKeys.STORAGE,
         ModelType.STRING, false)
         .setXmlName(Attribute.STORAGE.getLocalName())
         .setAllowExpression(false)
         .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
         .setAllowedValues(Storage.VOLATILE.toString(), Storage.PERSISTENT.toString())
         .setDefaultValue(new ModelNode().set(Storage.VOLATILE.toString()))
         .build();

   static final SimpleAttributeDefinition INITIAL_VALUE = new SimpleAttributeDefinitionBuilder(ModelKeys.INITIAL_VALUE,
         ModelType.LONG, true)
         .setXmlName(Attribute.INITIAL_VALUE.getLocalName())
         .setAllowExpression(false)
         .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
         .setDefaultValue(new ModelNode().set(0))
         .build();

   static final SimpleAttributeDefinition VALUE = new SimpleAttributeDefinitionBuilder(ModelKeys.VALUE,
         ModelType.LONG, true)
         .setXmlName(Attribute.VALUE.getLocalName())
         .setAllowExpression(false)
         .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
         .build();

   static final AttributeDefinition[] ATTRIBUTES = { COUNTER_NAME, STORAGE, INITIAL_VALUE };

   public CounterConfigurationResource(PathElement pathElement, ResourceDescriptionResolver descriptionResolver,
         ResolvePathHandler resolvePathHandler, AbstractAddStepHandler addHandler, OperationStepHandler removeHandler,
         boolean runtimeRegistration) {
      super(pathElement, descriptionResolver, addHandler, removeHandler);
   }

   @Override
   public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
      super.registerAttributes(resourceRegistration);
      final OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(ATTRIBUTES);
      for (AttributeDefinition attr : ATTRIBUTES) {
         resourceRegistration.registerReadWriteAttribute(attr, null, writeHandler);
      }
   }

   private static PathElement counterElement(OperationContext context, ModelNode operation)
           throws OperationFailedException {
       final PathAddress address = pathAddress(operation.require(OP_ADDR));
       final PathElement counterElement = address.getElement(address.size() - 1);
       return counterElement;
   }

   private static String counterName(OperationContext context, ModelNode operation) throws OperationFailedException {
       PathElement counterElement = counterElement(context, operation);
       return counterElement.getValue();
   }

   private static OperationFailedException counterManagerNotFound() {
       return new OperationFailedException("CounterManager not found in server.");
   }

   public static class CounterConfigurationRemoveCommand extends BaseCounterConfigurationManagerCommand {
       public static final CounterConfigurationRemoveCommand INSTANCE = new CounterConfigurationRemoveCommand();

       @Override
       protected ModelNode invoke(CounterManager counterManager, OperationContext context, ModelNode operation)
               throws Exception {

           final String counterName = counterName(context, operation);
           counterManager.remove(counterName);
           return new ModelNode();
       }
   }

   private static abstract class BaseCounterConfigurationManagerCommand extends CacheContainerCommands {

       BaseCounterConfigurationManagerCommand() {
           //path to container from counter address has two elements
           super(3);
       }

       abstract ModelNode invoke(CounterManager counterManager, OperationContext context, ModelNode operation)
               throws Exception;

       @Override
       protected final ModelNode invokeCommand(EmbeddedCacheManager cacheManager, OperationContext context,
               ModelNode operation) throws Exception {
           Optional<CounterManager> optCounterManager = SecurityActions.findCounterManager(cacheManager);
           CounterManager counterManager = optCounterManager.orElseThrow(CounterConfigurationResource::counterManagerNotFound);
           return invoke(counterManager, context, operation);
       }
   }
}