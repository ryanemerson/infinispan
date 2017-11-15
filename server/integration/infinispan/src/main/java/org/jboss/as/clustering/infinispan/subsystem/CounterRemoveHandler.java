package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 * Remove a counter
 *
 * @author Vladimir Blagojevic
 */
public class CounterRemoveHandler extends AbstractRemoveStepHandler {

   @Override
   protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model)
         throws OperationFailedException {
      CounterResource.CounterRemoveCommand.INSTANCE.execute(context, operation);
   }
}
