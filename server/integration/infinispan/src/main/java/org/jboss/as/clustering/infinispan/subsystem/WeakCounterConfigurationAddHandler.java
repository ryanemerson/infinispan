package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.counter.api.CounterConfiguration;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 * Add operation handler for /subsystem=infinispan/cache-container=clustered/counter=*
 * 
 * @author Vladimir Blagojevic
 * 
 */
public class WeakCounterConfigurationAddHandler extends CounterConfigurationAddHandler {

   @Override
   protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model)
         throws OperationFailedException {
      super.performRuntime(context, operation, model);
   }

   void populate(ModelNode fromModel, ModelNode toModel) throws OperationFailedException {
      super.populate(fromModel, toModel);
      for (AttributeDefinition attr : WeakCounterConfigurationResource.WEAK_ATTRIBUTES) {
         attr.validateAndSet(fromModel, toModel);
      }
   }

   /**
    * Implementation of abstract method processModelNode
    *
    */
   @Override
   void processModelNode(OperationContext context, String containerName, ModelNode counter,
         CounterConfiguration.Builder builder) throws OperationFailedException {
      super.processModelNode(context, containerName, counter, builder);

      Integer concurrency = WeakCounterConfigurationResource.CONCURRENCY.resolveModelAttribute(context, counter).asInt();
      builder.concurrencyLevel(concurrency);
   }
}
