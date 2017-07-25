/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.persistence.factory.CacheStoreFactory;
import org.infinispan.persistence.factory.CacheStoreFactoryRegistry;
import org.infinispan.server.infinispan.SecurityActions;
import org.infinispan.server.infinispan.task.ServerTaskRegistry;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;

/**
 * @author Paul Ferraro
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class CacheService<K, V> implements Service<Cache<K, V>> {

    private final Dependencies dependencies;
    private final String name;
    private final String configurationName;

    private volatile Cache<K, V> cache;

    private static final Logger log = Logger.getLogger(CacheService.class.getPackage().getName());

    public interface Dependencies {
        EmbeddedCacheManager getCacheContainer();
        CacheStoreFactory getDeployedCacheStoreFactory();
        ServerTaskRegistry getDeployedTaskRegistry();
    }

    public CacheService(String name, String configurationName, Dependencies dependencies) {
        this.name = name;
        this.configurationName = configurationName;
        this.dependencies = dependencies;
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.msc.value.Value#getValue()
     */
    @Override
    public Cache<K, V> getValue() {
        return this.cache;
    }

    @Override
    public void start(StartContext context) {
        EmbeddedCacheManager container = this.dependencies.getCacheContainer();

        CacheStoreFactoryRegistry cacheStoreFactoryRegistry = container.getGlobalComponentRegistry().getComponent(CacheStoreFactoryRegistry.class);
        cacheStoreFactoryRegistry.addCacheStoreFactory(this.dependencies.getDeployedCacheStoreFactory());

        container.getGlobalComponentRegistry().registerComponent(this.dependencies.getDeployedTaskRegistry(), ServerTaskRegistry.class);

        this.cache = SecurityActions.startCache(container, this.name, this.configurationName);

        log.debugf("%s cache started", this.name);
    }

    @Override
    public void stop(StopContext context) {
        if ((this.cache != null) && this.cache.getStatus().allowInvocations()) {
            SecurityActions.stopCache(cache);
            log.debugf("%s cache stopped", this.name);
        }
    }
}
