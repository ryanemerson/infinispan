package org.infinispan.stats;

/**
 * Similar to {@link Stats} but cluster wide.
 *
 * @author Vladimir Blagojevic
 * @since 7.1
 *
 */
public interface ClusterCacheStats extends Stats {

   /**
    * @return cluster wide read/writes ratio for the cache
    */
   double getReadWriteRatio();

   /**
    * @return cluster wide total percentage hit/(hit+miss) ratio for this cache
    */
   double getHitRatio();

   /**
    * @return the total number of exclusive locks available in the cluster
    */
   int getNumberOfLocksAvailable();

   /**
    * @return the total number of exclusive locks held in the cluster
    */
   int getNumberOfLocksHeld();

   /**
    * @return the total number of invalidations in the cluster
    */
   long getInvalidations();

   /**
    * @return the total number of actiavtions in the cluster
    */
   long getActivations();

   /**
    * @return the total number of passivations in the cluster
    */
   long getPassivations();

   /**
    * @return the total number of cacheloader load operations in the cluster
    */
   long getCacheLoaderLoads();

   /**
    * @return the total number of cacheloader misses in the cluster
    */
   long getCacheLoaderMisses();

   /**
    * @return the total number of cachewriter store operations in the cluster
    */
   long getStoreWrites();

   /**
    * @return the maximum amount of free memory in bytes across the cluster JVMs.
    */
   long getMemoryAvailable();

   /**
    * @return the maximum amount of memory that JVMs across the cluster will attempt to utilise in bytes.
    */
   long getMemoryMax();

   /**
    * @return the total amount of memory in the JVMs across the cluster in bytes.
    */
   long getMemoryTotal();

   /**
    * @return the amount of memory used by JVMs across the cluster in bytes.
    */
   long getMemoryUsed();
}
