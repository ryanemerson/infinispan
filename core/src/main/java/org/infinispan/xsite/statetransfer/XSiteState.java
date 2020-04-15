package org.infinispan.xsite.statetransfer;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.marshall.protostream.impl.MarshallableObject;
import org.infinispan.metadata.Metadata;
import org.infinispan.persistence.spi.MarshallableEntry;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * Represents the state of a single key to be sent to a backup site. It contains the only needed information, i.e., the
 * key, current value and associated metadata.
 *
 * @author Pedro Ruivo
 * @since 7.0
 */
@ProtoTypeId(ProtoStreamTypeIds.XSITE_STATE)
public class XSiteState {

   @ProtoField(number = 1)
   final MarshallableObject<?> key;

   @ProtoField(number = 2)
   final MarshallableObject<?> value;

   @ProtoField(number = 3)
   final MarshallableObject<Metadata> metadata;

   @ProtoFactory
   XSiteState(MarshallableObject<?> key, MarshallableObject<?> value, MarshallableObject<Metadata> metadata) {
      this.key = key;
      this.value = value;
      this.metadata = metadata;
   }

   public final Object key() {
      return MarshallableObject.unwrap(key);
   }

   public final Object value() {
      return MarshallableObject.unwrap(value);
   }

   public final Metadata metadata() {
      return MarshallableObject.unwrap(metadata);
   }

   public static XSiteState fromDataContainer(InternalCacheEntry entry) {
      return new XSiteState(
            MarshallableObject.create(entry.getKey()),
            MarshallableObject.create(entry.getValue()),
            MarshallableObject.create(entry.getMetadata())
      );
   }

   public static XSiteState fromCacheLoader(MarshallableEntry entry) {
      // We can't use any of the MarshallableEntry bytes as they rely on the persistence marshaller
      // TODO is this correct? Metadata and user types are part of the persistence context, so should be to create
      // MarshallableObject with bytes directly
      return new XSiteState(
            MarshallableObject.create(entry.getKey()),
            MarshallableObject.create(entry.getValue()),
            MarshallableObject.create(entry.getMetadata())
      );
   }

   @Override
   public String toString() {
      return "XSiteState{" +
            "key=" + key +
            ", value=" + value +
            ", metadata=" + metadata +
            '}';
   }
}
