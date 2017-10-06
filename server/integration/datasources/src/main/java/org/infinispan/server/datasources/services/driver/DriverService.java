package org.infinispan.server.datasources.services.driver;

import static org.infinispan.server.datasources.DatasourcesLogger.ROOT_LOGGER;

import java.sql.Driver;

import org.infinispan.server.datasources.services.driver.registry.DriverRegistry;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Service wrapper for a {@link Driver}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DriverService implements Service<Driver> {

   private final InjectedValue<DriverRegistry> injectedDriverRegistry = new InjectedValue<>();

   private final InstalledDriver driverMetaData;
   private final Driver driver;

   public DriverService(InstalledDriver driverMetaData, Driver driver) {
      assert driverMetaData != null : ROOT_LOGGER.nullVar("driverMetaData");
      assert driver != null : ROOT_LOGGER.nullVar("driver");
      this.driverMetaData = driverMetaData;
      this.driver = driver;
   }

   @Override
   public Driver getValue() throws IllegalStateException, IllegalArgumentException {
      return driver;
   }

   @Override
   public void start(StartContext context) throws StartException {
      injectedDriverRegistry.getValue().registerInstalledDriver(driverMetaData);
      ROOT_LOGGER.startedDriverService(driverMetaData.getDriverName());

   }

   @Override
   public void stop(StopContext context) {
      injectedDriverRegistry.getValue().unregisterInstalledDriver(driverMetaData);
      ROOT_LOGGER.stoppedDriverService(driverMetaData.getDriverName());
   }

   public Injector<DriverRegistry> getDriverRegistryServiceInjector() {
      return injectedDriverRegistry;
   }

}
