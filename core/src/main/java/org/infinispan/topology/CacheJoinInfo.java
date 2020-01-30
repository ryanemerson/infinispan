package org.infinispan.topology;

import java.util.Objects;
import java.util.Optional;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.distribution.ch.ConsistentHashFactory;
import org.infinispan.marshall.protostream.impl.MarshallableObject;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * This class contains the information that a cache needs to supply to the coordinator when starting up.
 *
 * @author Dan Berindei
 * @since 5.2
 */
@ProtoTypeId(ProtoStreamTypeIds.CACHE_JOIN_INFO)
public class CacheJoinInfo {
   // Global configuration
   private final ConsistentHashFactory<?> consistentHashFactory;
   private final MarshallableObject<ConsistentHashFactory<?>> wrappedConsistentHashFactory;
   private final int numSegments;
   private final int numOwners;
   private final long timeout;
   private final CacheMode cacheMode;

   // Per-node configuration
   private final float capacityFactor;

   // Per-node state info
   private final PersistentUUID persistentUUID;
   private final Optional<Integer> persistentStateChecksum;

   @ProtoFactory
   CacheJoinInfo(MarshallableObject<ConsistentHashFactory<?>> wrappedConsistentHashFactory, int numSegments, int numOwners,
                 long timeout, CacheMode cacheMode, float capacityFactor,
                 PersistentUUID persistentUUID, Integer persistentStateChecksum) {
      this.wrappedConsistentHashFactory = wrappedConsistentHashFactory;
      this.consistentHashFactory = MarshallableObject.unwrap(wrappedConsistentHashFactory);
      this.numSegments = numSegments;
      this.numOwners = numOwners;
      this.timeout = timeout;
      this.cacheMode = cacheMode;
      this.capacityFactor = capacityFactor;
      this.persistentUUID = persistentUUID;
      this.persistentStateChecksum = Optional.ofNullable(persistentStateChecksum);
   }

   public CacheJoinInfo(ConsistentHashFactory<?> consistentHashFactory, int numSegments, int numOwners, long timeout,
                        CacheMode cacheMode, float capacityFactor, PersistentUUID persistentUUID,
                        Optional<Integer> persistentStateChecksum) {
      this(MarshallableObject.create(consistentHashFactory), numSegments, numOwners, timeout, cacheMode,
            capacityFactor, persistentUUID, persistentStateChecksum.orElse(null));
   }

   public ConsistentHashFactory getConsistentHashFactory() {
      return consistentHashFactory;
   }

   @ProtoField(number = 1)
   MarshallableObject<ConsistentHashFactory<?>> getWrappedConsistentHashFactory() {
      return wrappedConsistentHashFactory;
   }

   @ProtoField(number = 2, defaultValue = "-1")
   public int getNumSegments() {
      return numSegments;
   }

   @ProtoField(number = 3, defaultValue = "-1")
   public int getNumOwners() {
      return numOwners;
   }

   @ProtoField(number = 4, defaultValue = "-1")
   public long getTimeout() {
      return timeout;
   }

   @ProtoField(number = 6)
   public CacheMode getCacheMode() {
      return cacheMode;
   }

   @ProtoField(number = 7, defaultValue = "0.0")
   public float getCapacityFactor() {
      return capacityFactor;
   }

   @ProtoField(number = 8)
   public PersistentUUID getPersistentUUID() {
      return persistentUUID;
   }

   @ProtoField(number = 9)
   public Optional<Integer> getPersistentStateChecksum() {
      return persistentStateChecksum;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      CacheJoinInfo that = (CacheJoinInfo) o;
      return numSegments == that.numSegments &&
            numOwners == that.numOwners &&
            timeout == that.timeout &&
            Float.compare(that.capacityFactor, capacityFactor) == 0 &&
            Objects.equals(consistentHashFactory, that.consistentHashFactory) &&
            Objects.equals(wrappedConsistentHashFactory, that.wrappedConsistentHashFactory) &&
            cacheMode == that.cacheMode &&
            Objects.equals(persistentUUID, that.persistentUUID) &&
            Objects.equals(persistentStateChecksum, that.persistentStateChecksum);
   }

   @Override
   public int hashCode() {
      return Objects.hash(consistentHashFactory, wrappedConsistentHashFactory, numSegments, numOwners, timeout,
            cacheMode, capacityFactor, persistentUUID, persistentStateChecksum);
   }

   @Override
   public String toString() {
      return "CacheJoinInfo{" +
            "consistentHashFactory=" + consistentHashFactory +
            ", numSegments=" + numSegments +
            ", numOwners=" + numOwners +
            ", timeout=" + timeout +
            ", cacheMode=" + cacheMode +
            ", persistentUUID=" + persistentUUID +
            ", persistentStateChecksum=" + persistentStateChecksum +
            '}';
   }
}
