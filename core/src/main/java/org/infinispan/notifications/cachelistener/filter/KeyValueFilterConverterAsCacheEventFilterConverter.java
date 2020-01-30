package org.infinispan.notifications.cachelistener.filter;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.factories.scopes.Scope;
import org.infinispan.factories.scopes.Scopes;
import org.infinispan.filter.KeyValueFilterConverter;
import org.infinispan.marshall.protostream.impl.MarshallableObject;
import org.infinispan.metadata.Metadata;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * {@link CacheEventFilterConverter} that uses an underlying {@link KeyValueFilterConverter} to do the conversion and
 * filtering. The new value and metadata are used as arguments to the underlying filter converter as it doesn't take
 * both new and old.
 * @author wburns
 * @since 9.4
 */
@ProtoTypeId(ProtoStreamTypeIds.KEY_VALUE_FILTER_CONVERTER_AS_CACHE_KEY_VALUE_CONVERTER)
@Scope(Scopes.NONE)
public class KeyValueFilterConverterAsCacheEventFilterConverter<K, V, C> implements CacheEventFilterConverter<K, V, C> {
   private final KeyValueFilterConverter<K, V, C> keyValueFilterConverter;

   public KeyValueFilterConverterAsCacheEventFilterConverter(KeyValueFilterConverter<K, V, C> keyValueFilterConverter) {
      this.keyValueFilterConverter = keyValueFilterConverter;
   }

   @ProtoFactory
   KeyValueFilterConverterAsCacheEventFilterConverter(MarshallableObject<KeyValueFilterConverter<K, V, C>> converter) {
      this.keyValueFilterConverter = MarshallableObject.unwrap(converter);
   }

   @ProtoField(number = 1)
   MarshallableObject<KeyValueFilterConverter<K, V, C>> getConverter() {
      return MarshallableObject.create(keyValueFilterConverter);
   }

   @Override
   public C filterAndConvert(K key, V oldValue, Metadata oldMetadata, V newValue, Metadata newMetadata, EventType eventType) {
      return keyValueFilterConverter.convert(key, newValue, newMetadata);
   }

   @Override
   public C convert(K key, V oldValue, Metadata oldMetadata, V newValue, Metadata newMetadata, EventType eventType) {
      return keyValueFilterConverter.convert(key, newValue, newMetadata);
   }

   @Override
   public boolean accept(K key, V oldValue, Metadata oldMetadata, V newValue, Metadata newMetadata, EventType eventType) {
      return keyValueFilterConverter.accept(key, newValue, newMetadata);
   }

   @Inject
   protected void injectDependencies(ComponentRegistry cr) {
      cr.wireDependencies(keyValueFilterConverter);
   }
}
