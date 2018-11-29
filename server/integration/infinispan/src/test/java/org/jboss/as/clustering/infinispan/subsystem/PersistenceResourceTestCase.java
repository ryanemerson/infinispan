package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

/**
 * // TODO: Document this
 *
 * @author Ryan Emerson
 * @since 10.0
 */
public class PersistenceResourceTestCase extends OperationSequencesTestCase {
   @Test
   public void testCacheContainerAddRemoveAddSequence() throws Exception {

      // Parse and install the XML into the controller
      String subsystemXml = getSubsystemXml() ;
      KernelServices servicesA = createKernelServicesBuilder().setSubsystemXml(subsystemXml).build();

      ModelNode addContainerOp = getCacheContainerAddOperation("maximal2");

      executeAndAssertOutcome(servicesA, addContainerOp, SUCCESS); // add a cache container

      // TODO test aliases
   }
}
