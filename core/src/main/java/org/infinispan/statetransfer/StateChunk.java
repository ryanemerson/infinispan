package org.infinispan.statetransfer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.protostream.WrappedMessage;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * Encapsulates a chunk of cache entries that belong to the same segment. This representation is suitable for sending it
 * to another cache during state transfer.
 *
 * @author anistor@redhat.com
 * @since 5.2
 */
@ProtoTypeId(ProtoStreamTypeIds.STATE_CHUNK)
public class StateChunk {

   /**
    * The id of the segment for which we push cache entries.
    */
   @ProtoField(number = 1, defaultValue = "-1")
   final int segmentId;

   /**
    * Indicates to receiver if there are more chunks to come for this segment.
    */
   @ProtoField(number = 2, defaultValue = "false")
   final boolean isLastChunk;

   /**
    * The cache entries. They are all guaranteed to be long to the same segment: segmentId.
    */
   @ProtoField(number = 3, collectionImplementation = ArrayList.class)
   final Collection<WrappedMessage> entries;
   final Collection<InternalCacheEntry<?, ?>> cacheEntries;


   @ProtoFactory
   @SuppressWarnings("unchecked")
   StateChunk(int segmentId, boolean isLastChunk, Collection<WrappedMessage> entries) {
      this.segmentId = segmentId;
      this.isLastChunk = isLastChunk;
      this.entries = entries;
      this.cacheEntries = entries.stream().map(WrappedMessage::getValue)
            .map(e -> (InternalCacheEntry<?,?>) e)
            .collect(Collectors.toList());
   }

   public static StateChunk create(int segmentId, boolean isLastChunk, InternalCacheEntry<?, ?>... entries) {
      StateChunk chunk = new StateChunk(segmentId, isLastChunk);
      for (InternalCacheEntry<?,?> entry : entries)
         chunk.add(entry);
      return chunk;
   }

   StateChunk(int segmentId, boolean isLastChunk) {
      this.segmentId = segmentId;
      this.isLastChunk = isLastChunk;
      this.entries = new ArrayList<>();
      this.cacheEntries = new ArrayList<>();
   }

   void add(InternalCacheEntry<?, ?> entry) {
      this.entries.add(new WrappedMessage(entry));
      this.cacheEntries.add(entry);
   }

   public int getSegmentId() {
      return segmentId;
   }

   public Collection<InternalCacheEntry<?, ?>> getCacheEntries() {
      return cacheEntries;
   }

   public boolean isLastChunk() {
      return isLastChunk;
   }

   @Override
   public String toString() {
      return "StateChunk{" +
            "segmentId=" + segmentId +
            ", cacheEntries=" + cacheEntries.size() +
            ", isLastChunk=" + isLastChunk +
            '}';
   }
}
