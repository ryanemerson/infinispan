package org.infinispan.notifications.cachelistener.cluster;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import org.infinispan.Cache;
import org.infinispan.commons.marshall.AbstractExternalizer;
import org.infinispan.commons.marshall.UserObjectInput;
import org.infinispan.commons.marshall.UserObjectOutput;
import org.infinispan.distexec.DistributedCallable;
import org.infinispan.marshall.core.Ids;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * This DistributedCallable is used to remove registered {@link RemoteClusterListener} on each of the various nodes
 * when a cluster listener is unregistered from the cache.
 *
 * @author wburns
 * @since 7.0
 */
public class ClusterListenerRemoveCallable<K, V> implements DistributedCallable<K, V, Void> {
   private static final Log log = LogFactory.getLog(ClusterListenerRemoveCallable.class);
   private static final boolean trace = log.isTraceEnabled();

   private transient Cache<K, V> cache;

   private final UUID identifier;

   public ClusterListenerRemoveCallable(UUID identifier) {
      this.identifier = identifier;
   }

   @Override
   public void setEnvironment(Cache<K, V> cache, Set<K> inputKeys) {
      this.cache = cache;
   }

   @Override
   public Void call() throws Exception {
      // Remove the listener from the cache now
      Set<Object> listeners = cache.getListeners();
      for (Object listener : listeners) {
         if (listener instanceof RemoteClusterListener) {
            RemoteClusterListener clusterListener = (RemoteClusterListener)listener;
            if (identifier.equals(clusterListener.getId())) {
               if (trace) {
                  log.tracef("Removing local cluster listener due to parent cluster listener was removed : %s", identifier);
               }
               clusterListener.removeListener();
            }
         }
      }
      return null;
   }

   public static class Externalizer extends AbstractExternalizer<ClusterListenerRemoveCallable> {
      @Override
      public Set<Class<? extends ClusterListenerRemoveCallable>> getTypeClasses() {
         return Collections.singleton(ClusterListenerRemoveCallable.class);
      }

      @Override
      public void writeObject(UserObjectOutput output, ClusterListenerRemoveCallable object) throws IOException {
         output.writeObject(object.identifier);
      }

      @Override
      public ClusterListenerRemoveCallable readObject(UserObjectInput input) throws IOException, ClassNotFoundException {
         return new ClusterListenerRemoveCallable((UUID)input.readObject());
      }

      @Override
      public Integer getId() {
         return Ids.CLUSTER_LISTENER_REMOVE_CALLABLE;
      }
   }
}
