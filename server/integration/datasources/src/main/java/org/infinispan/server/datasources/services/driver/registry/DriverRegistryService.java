package org.infinispan.server.datasources.services.driver.registry;

import static org.infinispan.server.datasources.DatasourcesLogger.ROOT_LOGGER;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * The JDBC driver registry service
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public final class DriverRegistryService implements Service<DriverRegistry> {

    private final DriverRegistry value;

    /**
     * Create an instance
     */
    public DriverRegistryService() {
        this.value = new DriverRegistryImpl();
    }

    @Override
    public DriverRegistry getValue() throws IllegalStateException {
        return value;
    }

    @Override
    public void start(StartContext context) throws StartException {
        ROOT_LOGGER.debugf("Starting service %s", this.getClass().getSimpleName());
    }

    @Override
    public void stop(StopContext context) {
        ROOT_LOGGER.debugf("Stopping service %s", this.getClass().getSimpleName());
    }
}
