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

import javax.transaction.xa.XAResource;

import org.infinispan.Cache;
import org.infinispan.conflict.EntryMergePolicyFactoryRegistry;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.persistence.factory.CacheStoreFactory;
import org.infinispan.persistence.factory.CacheStoreFactoryRegistry;
import org.jboss.as.clustering.infinispan.conflict.DeployedMergePolicyFactory;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.tm.XAResourceRecovery;
import org.jboss.tm.XAResourceRecoveryRegistry;

/**
 * @author Paul Ferraro
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class CacheService<K, V> implements Service<Cache<K, V>> {

    private final Dependencies dependencies;
    private final String name;
    private final String configurationName;

    private volatile Cache<K, V> cache;
    private volatile XAResourceRecovery recovery;

    private static final Logger log = Logger.getLogger(CacheService.class.getPackage().getName());

    public interface Dependencies {
        EmbeddedCacheManager getCacheContainer();
        XAResourceRecoveryRegistry getRecoveryRegistry();
        CacheStoreFactory getDeployedCacheStoreFactory();
        DeployedMergePolicyFactory getDeployedMergePolicyRegistry();
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

        GlobalComponentRegistry globalComponentRegistry = SecurityActions.getGlobalComponentRegistry(container);
        CacheStoreFactoryRegistry cacheStoreFactoryRegistry = globalComponentRegistry.getComponent(CacheStoreFactoryRegistry.class);
        cacheStoreFactoryRegistry.addCacheStoreFactory(this.dependencies.getDeployedCacheStoreFactory());
        EntryMergePolicyFactoryRegistry mergePolicyRegistry = globalComponentRegistry.getComponent(EntryMergePolicyFactoryRegistry.class);
        mergePolicyRegistry.addMergePolicyFactory(this.dependencies.getDeployedMergePolicyRegistry());

        this.cache = SecurityActions.startCache(container, this.name, this.configurationName);

        XAResourceRecoveryRegistry recoveryRegistry = this.dependencies.getRecoveryRegistry();
        if (recoveryRegistry != null) {
            this.recovery = new InfinispanXAResourceRecovery(this.name, container);
            recoveryRegistry.addXAResourceRecovery(this.recovery);
        }
        log.debugf("%s cache started", this.name);
    }

    @Override
    public void stop(StopContext context) {
        if ((this.cache != null) && this.cache.getStatus().allowInvocations()) {
            if (this.recovery != null) {
                this.dependencies.getRecoveryRegistry().removeXAResourceRecovery(this.recovery);
            }

            SecurityActions.stopCache(cache);
            log.debugf("%s cache stopped", this.name);
        }
    }

    static class InfinispanXAResourceRecovery implements XAResourceRecovery {
        private final String cacheName;
        private final EmbeddedCacheManager container;

        InfinispanXAResourceRecovery(String cacheName, EmbeddedCacheManager container) {
            this.cacheName = cacheName;
            this.container = container;
        }

        @Override
        public XAResource[] getXAResources() {
            return new XAResource[] { this.container.getCache(this.cacheName).getAdvancedCache().getXAResource() };
        }

        @Override
        public int hashCode() {
            return this.container.getCacheManagerConfiguration().globalJmxStatistics().cacheManagerName().hashCode() ^ this.cacheName.hashCode();
        }

        @Override
        public boolean equals(Object object) {
            if ((object == null) || !(object instanceof InfinispanXAResourceRecovery)) return false;
            InfinispanXAResourceRecovery recovery = (InfinispanXAResourceRecovery) object;
            return this.container.getCacheManagerConfiguration().globalJmxStatistics().cacheManagerName().equals(recovery.container.getCacheManagerConfiguration().globalJmxStatistics().cacheManagerName()) && this.cacheName.equals(recovery.cacheName);
        }

        @Override
        public String toString() {
            return container.getCacheManagerConfiguration().globalJmxStatistics().cacheManagerName() + "." + this.cacheName;
        }
    }
}
