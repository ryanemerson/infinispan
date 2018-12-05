package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.operations.common.Util.createAddOperation;

import java.io.IOException;

import org.infinispan.commons.util.Features;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

/**
 * A simple test to ensure that if the soft-index-file-store feature is disabled, then the {@link
 * SoftIndexConfigurationResource} is not registered as a subModel.
 *
 * @author Ryan Emerson
 * @since 10.0
 */
public class SoftIndexFeatureDisabledTestCase extends OperationTestCaseBase {

   private static final String SIMPLE_CACHE_XML = "simple-cache.xml";
   private static final String CONTAINER_NAME = "local";

   @Override
   protected String getSubsystemXml() throws IOException {
      return readResource(SIMPLE_CACHE_XML);
   }

   @Test
   public void testSoftIndexFeatureNotRegistered() throws Exception {
      KernelServices service = createKernelServicesBuilder().setSubsystemXml(getSubsystemXml()).build();

      String feature = Features.FEATURE_PREFIX + SoftIndexConfigurationResource.FEATURE;
      System.setProperty(feature, "true");
      PathAddress persistenceAddr = createConfigurationWithEmptyPersistence(service);
      PathAddress sifsAddr = persistenceAddr.append(ModelKeys.SOFT_INDEX_FILE_STORE, "sifs");

      executeAndAssertOutcome(service, createAddOperation(sifsAddr), SUCCESS);
      readStoreAndAssertOutcome(service, sifsAddr, SUCCESS);

      System.setProperty(feature, "false");
      service = createKernelServicesBuilder().setSubsystemXml(getSubsystemXml()).build();
      createConfigurationWithEmptyPersistence(service);
      executeAndAssertOutcome(service, createAddOperation(sifsAddr), FAILED);
   }

   private void readStoreAndAssertOutcome(KernelServices service, PathAddress address, String outcome) {
      ModelNode readOp = new ModelNode();
      readOp.get(OP).set(READ_RESOURCE_OPERATION);
      readOp.get(OP_ADDR).set(address.toModelNode());
      ModelNode result = service.executeOperation(readOp);
      Assert.assertEquals(result.asString(), outcome, result.get(OUTCOME).asString());
   }

   private PathAddress createConfigurationWithEmptyPersistence(KernelServices service) {
      PathAddress cacheConfAddres = getCacheConfigurationAddress(CONTAINER_NAME, ModelKeys.LOCAL_CACHE_CONFIGURATION, "example");
      PathAddress persistenceAddr = cacheConfAddres.append(PersistenceConfigurationResource.PATH);
      executeAndAssertOutcome(service, createAddOperation(cacheConfAddres), SUCCESS);
      executeAndAssertOutcome(service, createAddOperation(persistenceAddr), SUCCESS);
      return persistenceAddr;
   }
}
