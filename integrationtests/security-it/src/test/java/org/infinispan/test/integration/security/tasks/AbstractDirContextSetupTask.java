package org.infinispan.test.integration.security.tasks;

import org.infinispan.test.integration.security.elytron.DirContext;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;

public abstract class AbstractDirContextSetupTask implements ServerSetupTask {

   protected ManagementClient managementClient;
   protected DirContext[] contexts;

   @Override
   public void setup(ManagementClient managementClient, String s) throws Exception {
      this.managementClient = managementClient;
      this.contexts = getDirContexts();
      for (DirContext context : contexts) {

      }
   }

   @Override
   public void tearDown(ManagementClient managementClient, String s) throws Exception {

   }

   protected abstract DirContext[] getDirContexts() throws Exception;
}
