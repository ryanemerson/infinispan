package org.infinispan.notifications.cachelistener.cluster;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.infinispan.Cache;
import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.commons.util.Util;
import org.infinispan.encoding.DataConversion;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.marshall.protostream.impl.MarshallableObject;
import org.infinispan.notifications.cachelistener.CacheNotifier;
import org.infinispan.notifications.cachelistener.ListenerHolder;
import org.infinispan.notifications.cachelistener.filter.CacheEventConverter;
import org.infinispan.notifications.cachelistener.filter.CacheEventFilter;
import org.infinispan.notifications.cachemanagerlistener.CacheManagerNotifier;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.remoting.transport.Address;
import org.infinispan.remoting.transport.jgroups.JGroupsAddress;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * This DistributedCallable is used to install a {@link RemoteClusterListener} on the resulting node.  This class
 * also has checks to ensure that if the listener is attempted to be installed from more than 1 source only 1 will be
 * installed as well if a node goes down while installing will also remove the listener.
 *
 * @author wburns
 * @since 7.0
 */
@ProtoTypeId(ProtoStreamTypeIds.CLUSTER_LISTENER_REPLICATE_CALLABLE)
public class ClusterListenerReplicateCallable<K, V> implements Function<EmbeddedCacheManager, Void>,
      BiConsumer<EmbeddedCacheManager, Cache<K, V>> {
   private static final Log log = LogFactory.getLog(ClusterListenerReplicateCallable.class);
   private static final boolean trace = log.isTraceEnabled();

   private final CacheEventFilter<K, V> filter;
   private final CacheEventConverter<K, V, ?> converter;
   private final UUID identifier;

   // TODO marshall annotations and identifier with ProtoStream directly IPROTO-137
   private volatile Set<Class<? extends Annotation>> filterAnnotations;
   private volatile Set<String> filterAnnotationClassNames;

   @ProtoField(number = 1)
   final String cacheName;

   @ProtoField(number = 2, javaType = JGroupsAddress.class)
   final Address origin;

   @ProtoField(number = 3, defaultValue = "false")
   final boolean sync;

   @ProtoField(number = 4)
   final DataConversion keyDataConversion;

   @ProtoField(number = 5)
   final DataConversion valueDataConversion;

   @ProtoField(number = 6, defaultValue = "false")
   final boolean useStorageFormat;

   private ClusterListenerReplicateCallable(CacheEventFilter<K, V> filter, CacheEventConverter<K, V, ?> converter,
                                            UUID identifier, Set<Class<? extends Annotation>> filterAnnotations,
                                            Set<String> filterAnnotationClassNames, String cacheName, Address origin, boolean sync,
                                            DataConversion keyDataConversion, DataConversion valueDataConversion,
                                            boolean useStorageFormat) {
      this.filter = filter;
      this.converter = converter;
      this.filterAnnotations = filterAnnotations;
      this.filterAnnotationClassNames = filterAnnotationClassNames;
      this.identifier = identifier;
      this.cacheName = cacheName;
      this.origin = origin;
      this.sync = sync;
      this.keyDataConversion = keyDataConversion;
      this.valueDataConversion = valueDataConversion;
      this.useStorageFormat = useStorageFormat;
   }

   public ClusterListenerReplicateCallable(String cacheName, UUID identifier, Address origin, CacheEventFilter<K, V> filter,
                                           CacheEventConverter<K, V, ?> converter, boolean sync,
                                           Set<Class<? extends Annotation>> filterAnnotations,
                                           DataConversion keyDataConversion, DataConversion valueDataConversion, boolean useStorageFormat) {
      this(filter, converter, identifier, filterAnnotations, null, cacheName, origin, sync, keyDataConversion, valueDataConversion, useStorageFormat);

      if (trace)
         log.tracef("Created clustered listener replicate callable for: %s", filterAnnotations);
   }

   @ProtoFactory
   ClusterListenerReplicateCallable(String cacheName, String identifier, JGroupsAddress origin, MarshallableObject<CacheEventFilter<K, V>> filter,
                                    MarshallableObject<CacheEventConverter<K, V, ?>> converter, boolean sync,
                                    Set<String> filterAnnotationClassNames, DataConversion keyDataConversion,
                                    DataConversion valueDataConversion, boolean useStorageFormat) {
      this(MarshallableObject.unwrap(filter), MarshallableObject.unwrap(converter), UUID.fromString(identifier), null,
            filterAnnotationClassNames, cacheName, origin, sync, keyDataConversion, valueDataConversion, useStorageFormat);
   }

   @ProtoField(number = 7)
   String getIdentifier() {
      return identifier.toString();
   }

   @ProtoField(number = 8, collectionImplementation = HashSet.class)
   Collection<String> getFilterAnnotationClassNames() {
      if (filterAnnotationClassNames == null)
         filterAnnotationClassNames = filterAnnotations.stream().map(Class::getName).collect(Collectors.toSet());
      return filterAnnotationClassNames;
   }

   @ProtoField(number = 9)
   MarshallableObject<CacheEventFilter<K, V>> getFilter() {
      return MarshallableObject.create(filter);
   }

   @ProtoField(number = 10)
   MarshallableObject<CacheEventConverter<K, V, ?>> getConverter() {
      return MarshallableObject.create(converter);
   }

   @Override
   public Void apply(EmbeddedCacheManager cacheManager) {
      Cache<K, V> cache = cacheManager.getCache(cacheName);
      accept(cacheManager, cache);
      return null;
   }

   @Override
   public void accept(EmbeddedCacheManager cacheManager, Cache<K, V> cache) {
      ComponentRegistry componentRegistry = SecurityActions.getComponentRegistry(cache.getAdvancedCache());

      CacheNotifier<K, V> cacheNotifier = componentRegistry.getComponent(CacheNotifier.class);
      CacheManagerNotifier cacheManagerNotifier = componentRegistry.getComponent(CacheManagerNotifier.class);
      Address ourAddress = cache.getCacheManager().getAddress();
      ClusterEventManager<K, V> eventManager = componentRegistry.getComponent(ClusterEventManager.class);
      if (filter != null) {
         componentRegistry.wireDependencies(filter);
      }
      if (converter != null && converter != filter) {
         componentRegistry.wireDependencies(converter);
      }

      // Only register listeners if we aren't the ones that registered the cluster listener
      if (!ourAddress.equals(origin)) {
         // Make sure the origin is around otherwise don't register the listener - some way with identifier (CHM maybe?)
         if (cacheManager.getMembers().contains(origin)) {
            // Prevent multiple invocations to get in here at once, which should prevent concurrent registration of
            // the same id.  Note we can't use a static CHM due to running more than 1 node in same JVM
            synchronized (cacheNotifier) {
               boolean alreadyInstalled = false;
               // First make sure the listener is not already installed, if it is we don't do anything.
               for (Object installedListener : cacheNotifier.getListeners()) {
                  if (installedListener instanceof RemoteClusterListener &&
                        identifier.equals(((RemoteClusterListener) installedListener).getId())) {
                     alreadyInstalled = true;
                     break;
                  }
               }
               if (!alreadyInstalled) {
                  RemoteClusterListener listener = new RemoteClusterListener(identifier, origin, cacheNotifier,
                        cacheManagerNotifier, eventManager, sync);
                  ListenerHolder listenerHolder = new ListenerHolder(listener, keyDataConversion, valueDataConversion, useStorageFormat);
                  if (filterAnnotations == null) {
                     filterAnnotations = new HashSet<>(filterAnnotationClassNames.size());
                     ClassLoader classLoader = cacheManager.getCacheManagerConfiguration().classLoader();
                     for (String clazz : filterAnnotationClassNames) {
                        filterAnnotations.add(Util.loadClass(clazz, classLoader));
                     }
                  }
                  cacheNotifier.addFilteredListener(listenerHolder, filter, converter, filterAnnotations);
                  cacheManagerNotifier.addListener(listener);
                  // It is possible the member is now gone after registered, if so we have to remove just to be sure
                  if (!cacheManager.getMembers().contains(origin)) {
                     cacheNotifier.removeListener(listener);
                     cacheManagerNotifier.removeListener(listener);
                     if (trace) {
                        log.tracef("Removing local cluster listener for remote cluster listener that was just registered, as the origin %s went away concurrently", origin);
                     }
                  } else if (trace) {
                     log.tracef("Registered local cluster listener for remote cluster listener from origin %s with id %s",
                           origin, identifier);
                  }
               } else if (trace) {
                  log.tracef("Local cluster listener from origin %s with id %s was already installed, ignoring",
                        origin, identifier);
               }
            }
         } else if (trace) {
            log.tracef("Not registering local cluster listener for remote cluster listener from origin %s, as the origin went away",
                  origin);
         }
      } else if (trace) {
         log.trace("Not registering local cluster listener as we are the node who registered the cluster listener");
      }
   }

   @Override
   public String toString() {
      return "ClusterListenerReplicateCallable{" +
            "cacheName='" + cacheName + '\'' +
            ", identifier=" + identifier +
            ", origin=" + origin +
            ", sync=" + sync +
            '}';
   }
}
