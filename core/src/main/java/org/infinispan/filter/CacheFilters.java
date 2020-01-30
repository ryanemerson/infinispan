package org.infinispan.filter;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.infinispan.CacheStream;
import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.container.entries.CacheEntry;
import org.infinispan.container.impl.InternalEntryFactory;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.factories.scopes.Scope;
import org.infinispan.factories.scopes.Scopes;
import org.infinispan.marshall.protostream.impl.MarshallableObject;
import org.infinispan.metadata.Metadata;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * Static factory class that contains utility methods that can be used for performing proper transformations from
 * {@link KeyValueFilter}, {@link Converter} and {@link KeyValueFilterConverter} to appropriate distributed stream
 * instances.
 */
public final class CacheFilters {
   private CacheFilters() { }

   /**
    * Creates a new {@link Predicate} using the provided key value filter as a basis for the operation.  This is useful
    * for when using {@link Stream#filter(Predicate)} method on distributed streams.  The key,
    * value and metadata are all used to determine if the predicate returns true or not.
    * @param filter the filter to utilize
    * @param <K> key type
    * @param <V> value type
    * @return predicate based on the filter
    */
   public static <K, V> Predicate<CacheEntry<K, V>> predicate(KeyValueFilter<? super K, ? super V> filter) {
      return new KeyValueFilterAsPredicate<>(filter);
   }

   /**
    * Creates a new {@link Function} using the provided converter as a basis for the operation.  This is useful
    * for when using {@link Stream#map(Function)} method on distributed streams.  The key,
    * value and metadata are all used to determine the converted value.
    * @param converter the converter to utilize
    * @param <K> key type
    * @param <V> value type
    * @param <C> convertered value type
    * @return function based on the converter
    */
   public static <K, V, C> Function<CacheEntry<K, V>, CacheEntry<K, C>> function(
           Converter<? super K, ? super V, C> converter) {
      return new ConverterAsCacheEntryFunction<>(converter);
   }

   public static <K, V, C> CacheStream<C> filterAndConvertToValue(CacheStream<CacheEntry<K, V>> stream,
         KeyValueFilterConverter<? super K, ? super V, C> filterConverter) {
      // Have to use flatMap instead of map/filter as reactive streams spec doesn't allow null values
      return stream.flatMap(new FilterConverterAsValueFunction(filterConverter));
   }

   /**
    * Adds needed intermediate operations to the provided stream, returning a possibly new stream as a result of the
    * operations.  This method keeps the contract of filter and conversion being performed in only 1 call as the
    * {@link KeyValueFilterConverter} was designed to do.  The key,
    * value and metadata are all used to determine whether the value is returned and the converted value.
    * @param stream stream to perform the operations on
    * @param filterConverter converter to apply
    * @param <K>
    * @param <V>
    * @param <C>
    * @return
    */
   public static <K, V, C> Stream<CacheEntry<K, C>> filterAndConvert(Stream<CacheEntry<K, V>> stream,
           KeyValueFilterConverter<? super K, ? super V, C> filterConverter) {
      // Have to use flatMap instead of map/filter as reactive streams spec doesn't allow null values
      return stream.flatMap(flatMap(filterConverter));
   }

   public static <K, V, C> CacheStream<CacheEntry<K, C>> filterAndConvert(CacheStream<CacheEntry<K, V>> stream,
            KeyValueFilterConverter<? super K, ? super V, C> filterConverter) {
      // Have to use flatMap instead of map/filter as reactive streams spec doesn't allow null values
      return stream.flatMap(flatMap(filterConverter));
   }

   public static <K, V, C> Function<CacheEntry<K, V>, Stream<CacheEntry<K, C>>> flatMap(KeyValueFilterConverter<? super K, ? super V, C> filterConverter) {
      return new FilterConverterAsCacheEntryFunction<>(filterConverter);
   }

   @ProtoTypeId(ProtoStreamTypeIds.CACHE_FILTERS_KEY_VALUE_FILTER_AS_PREDICATE)
   @Scope(Scopes.NONE)
   public static class KeyValueFilterAsPredicate<K, V> implements Predicate<CacheEntry<K, V>> {
      private final KeyValueFilter<? super K, ? super V> filter;

      public KeyValueFilterAsPredicate(KeyValueFilter<? super K, ? super V> filter) {
         Objects.requireNonNull(filter);
         this.filter = filter;
      }

      @ProtoFactory
      KeyValueFilterAsPredicate(MarshallableObject<KeyValueFilter<? super K, ? super V>> filter) {
         this.filter = MarshallableObject.unwrap(filter);
      }

      @ProtoField(number = 1)
      MarshallableObject<KeyValueFilter<? super K, ? super V>> getFilter() {
         return MarshallableObject.create(filter);
      }

      @Override
      public boolean test(CacheEntry<K, V> kvCacheEntry) {
         return filter.accept(kvCacheEntry.getKey(), kvCacheEntry.getValue(), kvCacheEntry.getMetadata());
      }

      @Inject
      public void inject(ComponentRegistry registry) {
         registry.wireDependencies(filter);
      }
   }

   @Scope(Scopes.NONE)
   public static class ConverterAsCacheEntryFunction<K, V, C> implements Function<CacheEntry<K, V>, CacheEntry<K, C>> {
      private final Converter<? super K, ? super V, C> converter;
      private InternalEntryFactory factory;

      public ConverterAsCacheEntryFunction(Converter<? super K, ? super V, C> converter) {
         Objects.requireNonNull(converter);
         this.converter = converter;
      }

      @ProtoFactory
      ConverterAsCacheEntryFunction(MarshallableObject<Converter<? super K, ? super V, C>> converter) {
         this.converter = MarshallableObject.unwrap(converter);
      }

      @ProtoField(number = 1)
      MarshallableObject<Converter<? super K, ? super V, C>> getConverter() {
         return MarshallableObject.create(converter);
      }

      @Inject
      public void inject(InternalEntryFactory factory, ComponentRegistry registry) {
         this.factory = factory;
         registry.wireDependencies(converter);
      }

      @Override
      public CacheEntry<K, C> apply(CacheEntry<K, V> kvCacheEntry) {
         K key = kvCacheEntry.getKey();
         V value = kvCacheEntry.getValue();
         Metadata metadata = kvCacheEntry.getMetadata();
         C converted = converter.convert(key, value, metadata);
         if (value == converted) {
            return (CacheEntry<K, C>) kvCacheEntry;
         }
         return factory.create(key, converted, metadata);
      }
   }

   @ProtoTypeId(ProtoStreamTypeIds.CACHE_FILTERS_CONVERTER_AS_CACHE_ENTRY_FUNCTION)
   @Scope(Scopes.NONE)
   public static class FilterConverterAsCacheEntryFunction<K, V, C> implements Function<CacheEntry<K, V>, Stream<CacheEntry<K, C>>> {
      private final KeyValueFilterConverter<? super K, ? super V, C> converter;
      private InternalEntryFactory factory;

      public FilterConverterAsCacheEntryFunction(KeyValueFilterConverter<? super K, ? super V, C> converter) {
         Objects.requireNonNull(converter);
         this.converter = converter;
      }

      @ProtoFactory
      FilterConverterAsCacheEntryFunction(MarshallableObject<KeyValueFilterConverter<? super K, ? super V, C>> converter) {
         this.converter = MarshallableObject.unwrap(converter);
      }

      @ProtoField(number = 1)
      MarshallableObject<KeyValueFilterConverter<? super K, ? super V, C>> getConverter() {
         return MarshallableObject.create(converter);
      }

      @Inject
      public void inject(InternalEntryFactory factory, ComponentRegistry registry) {
         this.factory = factory;
         registry.wireDependencies(converter);
      }

      @Override
      public Stream<CacheEntry<K, C>> apply(CacheEntry<K, V> kvCacheEntry) {
         K key = kvCacheEntry.getKey();
         V value = kvCacheEntry.getValue();
         Metadata metadata = kvCacheEntry.getMetadata();
         C converted = converter.filterAndConvert(key, value, metadata);
         if (converted == null) {
            return Stream.empty();
         }
         return Stream.of(factory.create(key, converted, metadata));
      }
   }

   @ProtoTypeId(ProtoStreamTypeIds.CACHE_FILTERS_FILTER_CONVERTER_AS_VALUE_FUNCTION)
   @Scope(Scopes.NONE)
   public static class FilterConverterAsValueFunction<K, V, C> implements Function<CacheEntry<K, V>, Stream<C>> {
      private final KeyValueFilterConverter<? super K, ? super V, C> converter;

      public FilterConverterAsValueFunction(KeyValueFilterConverter<? super K, ? super V, C> converter) {
         Objects.requireNonNull(converter);
         this.converter = converter;
      }

      @ProtoFactory
      FilterConverterAsValueFunction(MarshallableObject<KeyValueFilterConverter<? super K, ? super V, C>> converter) {
         this.converter = MarshallableObject.unwrap(converter);
      }

      @ProtoField(number = 1)
      MarshallableObject<KeyValueFilterConverter<? super K, ? super V, C>> getConverter() {
         return MarshallableObject.create(converter);
      }

      @Inject
      public void inject(ComponentRegistry registry) {
         registry.wireDependencies(converter);
      }

      @Override
      public Stream<C> apply(CacheEntry<K, V> entry) {
         C converted = converter.filterAndConvert(entry.getKey(), entry.getValue(), entry.getMetadata());
         if (converted == null) {
            return Stream.empty();
         }
         return Stream.of(converted);
      }
   }
}
