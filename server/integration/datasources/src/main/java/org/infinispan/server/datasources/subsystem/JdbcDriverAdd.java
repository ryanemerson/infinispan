package org.infinispan.server.datasources.subsystem;

import static org.infinispan.server.datasources.DatasourcesLogger.ROOT_LOGGER;
import static org.infinispan.server.datasources.subsystem.JdbcDriverResource.DATASOURCE_CLASS;
import static org.infinispan.server.datasources.subsystem.JdbcDriverResource.DRIVER_CLASS;
import static org.infinispan.server.datasources.subsystem.JdbcDriverResource.MAJOR_VERSION;
import static org.infinispan.server.datasources.subsystem.JdbcDriverResource.MINOR_VERSION;
import static org.infinispan.server.datasources.subsystem.JdbcDriverResource.MODULE_SLOT;
import static org.infinispan.server.datasources.subsystem.JdbcDriverResource.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.lang.reflect.Constructor;
import java.sql.Driver;
import java.util.Iterator;
import java.util.ServiceLoader;

import org.infinispan.server.datasources.services.driver.DriverService;
import org.infinispan.server.datasources.services.driver.InstalledDriver;
import org.infinispan.server.datasources.services.driver.registry.DriverRegistry;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

public class JdbcDriverAdd extends AbstractAddStepHandler {
    static final JdbcDriverAdd INSTANCE = new JdbcDriverAdd();

    @Override
    public void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        final ModelNode address = operation.require(OP_ADDR);
        final String driverName = PathAddress.pathAddress(address).getLastElement().getValue();

        for (AttributeDefinition attribute : JdbcDriverResource.JDBC_DRIVER_ATTRIBUTES)
            attribute.validateAndSet(operation, model);

        model.get(NAME.getName()).set(driverName);//this shouldn't be here anymore
    }

    @Override
    public void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final ModelNode address = operation.require(OP_ADDR);
        final String driverName = PathAddress.pathAddress(address).getLastElement().getValue();
        if (operation.get(NAME.getName()).isDefined() && !driverName.equals(operation.get(NAME.getName()).asString())) {
            throw ROOT_LOGGER.driverNameAndResourceNameNotEquals(operation.get(NAME.getName()).asString(), driverName);
        }
        String moduleName = MODULE_SLOT.resolveModelAttribute(context, model).asString();
        final Integer majorVersion = model.hasDefined(MAJOR_VERSION.getName()) ? MAJOR_VERSION.resolveModelAttribute(context, model).asInt() : null;
        final Integer minorVersion = model.hasDefined(MINOR_VERSION.getName()) ? MINOR_VERSION.resolveModelAttribute(context, model).asInt() : null;
        final String driverClassName = model.hasDefined(DRIVER_CLASS.getName()) ? DRIVER_CLASS.resolveModelAttribute(context, model).asString() : null;
        final String dataSourceClassName = model.hasDefined(DATASOURCE_CLASS.getName()) ? DATASOURCE_CLASS.resolveModelAttribute(context, model).asString() : null;

        final ServiceTarget target = context.getServiceTarget();

        final ModuleIdentifier moduleId;
        final Module module;
        String slot = model.hasDefined(MODULE_SLOT.getName()) ? MODULE_SLOT.resolveModelAttribute(context, model).asString() : null;

        try {
            moduleId = ModuleIdentifier.create(moduleName, slot);
            module = Module.getCallerModuleLoader().loadModule(moduleId);
        } catch (ModuleLoadException e) {
            throw new OperationFailedException(ROOT_LOGGER.failedToLoadModuleDriver(moduleName), e);
        }

        if (driverClassName == null) {
            final ServiceLoader<Driver> serviceLoader = module.loadService(Driver.class);
            Iterator<Driver> iterator = serviceLoader.iterator();
            if (iterator.hasNext()) {
                //just consider first definition and create service for this. User can use different implementation only
                // w/ explicit declaration of driver-class attribute
                startDriverServices(target, moduleId, iterator.next(), driverName, majorVersion, minorVersion, dataSourceClassName);
            } else {
                ROOT_LOGGER.cannotFindDriverClassName(driverName);
            }
        } else {
            try {
                final Class<? extends Driver> driverClass = module.getClassLoader().loadClass(driverClassName).asSubclass(Driver.class);
                final Constructor<? extends Driver> constructor = driverClass.getConstructor();
                final Driver driver = constructor.newInstance();
                startDriverServices(target, moduleId, driver, driverName, majorVersion, minorVersion, dataSourceClassName);
            } catch (Exception e) {
                ROOT_LOGGER.cannotInstantiateDriverClass(driverClassName, e);
                throw new OperationFailedException(ROOT_LOGGER.cannotInstantiateDriverClass(driverClassName));
            }
        }
    }

    private void startDriverServices(final ServiceTarget target, final ModuleIdentifier moduleId, Driver driver, final String driverName,
                                           final Integer majorVersion, final Integer minorVersion, final String dataSourceClassName) throws IllegalStateException {
        final int majorVer = driver.getMajorVersion();
        final int minorVer = driver.getMinorVersion();

        if ((majorVersion != null && majorVersion != majorVer) || (minorVersion != null && minorVersion != minorVer))
            throw ROOT_LOGGER.driverVersionMismatch();

        final boolean compliant = driver.jdbcCompliant();
        if (compliant) {
            ROOT_LOGGER.deployingCompliantJdbcDriver(driver.getClass(), majorVer, minorVer);
        } else {
            ROOT_LOGGER.deployingNonCompliantJdbcDriver(driver.getClass(), majorVer, minorVer);
        }
        InstalledDriver driverMetadata = new InstalledDriver(driverName, moduleId, driver.getClass().getName(),
                dataSourceClassName, majorVer, minorVer, compliant);
        DriverService driverService = new DriverService(driverMetadata, driver);
        final ServiceBuilder<Driver> builder = target.addService(ServiceName.JBOSS.append(ModelKeys.JDBC_DRIVER, driverName.replaceAll("\\.", "_")), driverService)
                .addDependency(JdbcDriverResource.JDBC_DRIVER_REGISTRY_SERVICE, DriverRegistry.class,
                        driverService.getDriverRegistryServiceInjector())
                .setInitialMode(ServiceController.Mode.ACTIVE);
        builder.install();
    }
}
