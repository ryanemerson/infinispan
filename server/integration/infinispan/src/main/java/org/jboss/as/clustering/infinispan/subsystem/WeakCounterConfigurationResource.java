package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
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
public class WeakCounterConfigurationResource extends CounterConfigurationResource {

   public static final PathElement PATH = PathElement.pathElement(ModelKeys.WEAK_COUNTER);

   static final SimpleAttributeDefinition CONCURRENCY = new SimpleAttributeDefinitionBuilder(ModelKeys.CONCURRENCY,
         ModelType.INT, true)
           .setXmlName(Attribute.CONCURRENCY.getLocalName())
           .setAllowExpression(false)
           .setDefaultValue(new ModelNode().set(64))
           .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES).build();

   static final AttributeDefinition[] WEAK_ATTRIBUTES = { CONCURRENCY };

   public WeakCounterConfigurationResource(ResolvePathHandler resolvePathHandler, boolean runtimeRegistration) {
      super(WeakCounterConfigurationResource.PATH, new InfinispanResourceDescriptionResolver(ModelKeys.COUNTERS), resolvePathHandler,
            new WeakCounterConfigurationAddHandler(), new CounterConfigurationRemoveHandler(), runtimeRegistration);
   }

   @Override
   public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
      super.registerAttributes(resourceRegistration);

      final OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(WEAK_ATTRIBUTES);
      for (AttributeDefinition attr : WEAK_ATTRIBUTES) {
         resourceRegistration.registerReadWriteAttribute(attr, null, writeHandler);
      }
   }
}
