package org.infinispan.server.datasources.services.datasource;

import javax.sql.DataSource;

import org.infinispan.server.datasources.subsystem.ModelKeys;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * @author Ryan Emerson
 * @since 9.2
 */
public class DataSourceService implements Service<DataSource> {
   public static final ServiceName SERVICE_NAME_BASE = ServiceName.JBOSS.append(ModelKeys.DATA_SOURCE);

   private final String dsName;
   private final ContextNames.BindInfo jndiName;
   private final ClassLoader classLoader; // The class loader to use. If null the Driver class loader will be used instead.

   DataSourceService(final String dsName, final ContextNames.BindInfo jndiName, final ClassLoader classLoader ) {
      this.dsName = dsName;
      this.classLoader = classLoader;
      this.jndiName = jndiName;
   }

   @Override
   public void start(StartContext context) throws StartException {
      final ServiceContainer container = context.getController().getServiceContainer();
      // Deploy the DataSource here
//      container.addService()
   }

   @Override
   public void stop(StopContext context) {

   }

   @Override
   public DataSource getValue() throws IllegalStateException, IllegalArgumentException {
      return null;
   }
}
