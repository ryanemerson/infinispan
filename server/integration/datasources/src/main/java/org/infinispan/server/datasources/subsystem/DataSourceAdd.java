package org.infinispan.server.datasources.subsystem;

import static org.infinispan.server.datasources.subsystem.DataSourceResource.DRIVER;
import static org.infinispan.server.datasources.subsystem.DataSourceResource.JNDI_NAME;

import java.sql.Driver;
import java.util.Arrays;

import javax.sql.DataSource;

import org.infinispan.server.datasources.services.datasource.DataSourceMeta;
import org.infinispan.server.datasources.services.datasource.DataSourceService;
import org.infinispan.server.datasources.services.driver.registry.DriverRegistry;
import org.infinispan.server.datasources.services.driver.registry.DriverRegistryService;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.NamingService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueInjectionService;

/**
 * @author Ryan Emerson
 * @since 9.2
 */
class DataSourceAdd extends AbstractAddStepHandler {
   static final DataSourceAdd INSTANCE = new DataSourceAdd();

   private DataSourceAdd() {
      super(Arrays.asList(DataSourceResource.DATASOURCE_ATTRIBUTE));
   }

   @Override
   protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
      // Create DatasourceMeta
      // Create DsService && add dependency on driver if used
      // Register DS with JNDI using meta
      final String dsName = context.getCurrentAddressValue();
      final String driverName = DRIVER.resolveModelAttribute(context, model).asString();
      final String jndiName = JNDI_NAME.resolveModelAttribute(context, model).asString();

      final ValueInjectionService<Driver> driverDemanderService = new ValueInjectionService<>();
      final ServiceName driverServiceName = JdbcDriverResource.createDriverServiceName(driverName);
      final ServiceName driverDemanderServiceName = ServiceName.JBOSS.append("driver-demander").append(jndiName);
      final ServiceTarget serviceTarget = context.getServiceTarget();
      final ServiceBuilder<?> driverDemanderBuilder = serviceTarget
            .addService(driverDemanderServiceName, driverDemanderService)
            .addDependency(driverServiceName, Driver.class, driverDemanderService.getInjector());
      driverDemanderBuilder.setInitialMode(ServiceController.Mode.ACTIVE);

      // TODO populate meta
      DataSourceMeta meta = new DataSourceMeta(
            "jdbc:mysql://127.0.0.1:3306/mysql_cache",
            "mysql",
            null,
            null,
            "java:jboss/datasources/MySQL",
            "MySQL",
            "ryan",
            "password"
            );
      DataSourceService dataSourceService = new DataSourceService(meta);

      // TODO put capability name somewhere else
      final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(jndiName);
      final ServiceName dataSourceServiceNameAlias = DataSourceService.getServiceName(bindInfo);
      final ServiceName dataSourceServiceName = context.getCapabilityServiceName("org.infinispan.data-source", dsName, DataSource.class);
      final ServiceBuilder<?> dataSourceServiceBuilder =
            serviceTarget.addService(dataSourceServiceName, dataSourceService)
                  .addAliases(dataSourceServiceNameAlias)
                  .addDependency(DriverRegistryService.SERVICE_NAME, DriverRegistry.class, dataSourceService.getDriverRegistryInjector())
                  .addDependency(NamingService.SERVICE_NAME);

      dataSourceServiceBuilder.install();
      driverDemanderBuilder.install();
   }
}
