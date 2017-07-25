package org.infinispan.server.datasources.services.driver.registry;

import java.util.Set;

import org.infinispan.server.datasources.services.driver.InstalledDriver;

/**
 * A registry for JDBC drivers installed in the system.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public interface DriverRegistry {

   /**
    * Register an installed JDBC driver
    *
    * @param driver the driver
    */
   void registerInstalledDriver(InstalledDriver driver) throws IllegalArgumentException;

   /**
    * Unregister an installed JDBC driver
    *
    * @param driver the driver
    */
   void unregisterInstalledDriver(InstalledDriver driver);

   /**
    * Get the installed drivers
    *
    * @return The set of drivers
    */
   Set<InstalledDriver> getInstalledDrivers();

   InstalledDriver getInstalledDriver(String name);
}
