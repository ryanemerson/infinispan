package org.infinispan.server.datasources.subsystem;

import static org.infinispan.server.datasources.DatasourcesLogger.ROOT_LOGGER;
import static org.infinispan.server.datasources.subsystem.JdbcDriverAdd.startDriverServices;
import static org.infinispan.server.datasources.subsystem.JdbcDriverResource.DATASOURCE_CLASS;
import static org.infinispan.server.datasources.subsystem.JdbcDriverResource.DRIVER_CLASS;
import static org.infinispan.server.datasources.subsystem.JdbcDriverResource.MAJOR_VERSION;
import static org.infinispan.server.datasources.subsystem.JdbcDriverResource.MINOR_VERSION;
import static org.infinispan.server.datasources.subsystem.JdbcDriverResource.MODULE_SLOT;
import static org.infinispan.server.datasources.subsystem.JdbcDriverResource.NAME;

import java.lang.reflect.Constructor;
import java.sql.Driver;
import java.util.ServiceLoader;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;

class JdbcDriverRemove extends AbstractRemoveStepHandler {
   static final JdbcDriverRemove INSTANCE = new JdbcDriverRemove();

   protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
      final String driverName = model.get(NAME.getName()).asString();

      final ServiceRegistry registry = context.getServiceRegistry(true);
      ServiceName jdbcServiceName = JdbcDriverResource.createDriverServiceName(driverName);
      ServiceController<?> jdbcServiceController = registry.getService(jdbcServiceName);
      context.removeService(jdbcServiceController);

   }

   protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) {
      final String driverName = model.require(NAME.getName()).asString();
      final String moduleName = model.require(MODULE_SLOT.getName()).asString();
      final Integer majorVersion = model.hasDefined(MAJOR_VERSION.getName()) ? model.get(MAJOR_VERSION.getName()).asInt() : null;
      final Integer minorVersion = model.hasDefined(MINOR_VERSION.getName()) ? model.get(MINOR_VERSION.getName()).asInt() : null;
      final String driverClassName = model.hasDefined(DRIVER_CLASS.getName()) ? model.get(DRIVER_CLASS.getName()).asString() : null;
      final String dataSourceClassName = model.hasDefined(DATASOURCE_CLASS.getName()) ? model.get(DATASOURCE_CLASS.getName()).asString() : null;
      final ServiceTarget target = context.getServiceTarget();

      final ModuleIdentifier moduleId;
      final Module module;
      try {
         moduleId = ModuleIdentifier.fromString(moduleName);
         module = Module.getCallerModuleLoader().loadModule(moduleId);
      } catch (ModuleLoadException e) {
         context.getFailureDescription().set(ROOT_LOGGER.failedToLoadModuleDriver(moduleName));
         return;
      }

      if (driverClassName == null) {
         final ServiceLoader<Driver> serviceLoader = module.loadService(Driver.class);
         if (serviceLoader != null)
            for (Driver driver : serviceLoader) {
               startDriverServices(target, moduleId, driver, driverName, majorVersion, minorVersion, dataSourceClassName);
            }
      } else {
         try {
            final Class<? extends Driver> driverClass = module.getClassLoader().loadClass(driverClassName)
                  .asSubclass(Driver.class);
            final Constructor<? extends Driver> constructor = driverClass.getConstructor();
            final Driver driver = constructor.newInstance();
            startDriverServices(target, moduleId, driver, driverName, majorVersion, minorVersion, dataSourceClassName);
         } catch (Exception e) {
            ROOT_LOGGER.cannotInstantiateDriverClass(driverClassName, e);
         }
      }
   }
}
