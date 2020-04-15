package org.infinispan.commands.functional;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.encoding.DataConversion;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.factories.KnownComponentNames;
import org.infinispan.functional.EntryView;
import org.infinispan.marshall.core.EncoderRegistry;
import org.infinispan.marshall.persistence.PersistenceMarshaller;
import org.infinispan.marshall.protostream.impl.MarshallableObject;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.registry.InternalCacheRegistry;

/**
 * Helper class for marshalling, also hiding implementations of {@link Mutation} from the interface.
 */
public final class Mutations {
   private Mutations() {
   }

   static abstract class BaseMutation<K, V, R> implements Mutation<K, V, R> {

      @ProtoField(number = 1)
      protected final DataConversion keyDataConversion;

      @ProtoField(number = 2)
      protected final DataConversion valueDataConversion;

      BaseMutation(DataConversion keyDataConversion, DataConversion valueDataConversion) {
         this.keyDataConversion = keyDataConversion;
         this.valueDataConversion = valueDataConversion;
      }

      public DataConversion keyDataConversion() {
         return keyDataConversion;
      }

      public DataConversion valueDataConversion() {
         return valueDataConversion;
      }

      @Override
      public void inject(ComponentRegistry registry) {
         GlobalConfiguration globalConfiguration = registry.getGlobalComponentRegistry().getGlobalConfiguration();
         EncoderRegistry encoderRegistry = registry.getComponent(EncoderRegistry.class);
         Configuration configuration = registry.getComponent(Configuration.class);
         InternalCacheRegistry icr = registry.getComponent(InternalCacheRegistry.class);
         String cacheName = registry.getComponent(String.class, KnownComponentNames.CACHE_NAME);
         PersistenceMarshaller persistenceMarshaller = registry.getComponent(PersistenceMarshaller.class, KnownComponentNames.PERSISTENCE_MARSHALLER);
         keyDataConversion.injectDependencies(persistenceMarshaller, cacheName, icr, globalConfiguration, encoderRegistry, configuration);
         valueDataConversion.injectDependencies(persistenceMarshaller, cacheName, icr, globalConfiguration, encoderRegistry, configuration);
      }
   }

   @ProtoTypeId(ProtoStreamTypeIds.MUTATIONS_READ_WRITE)
   public static class ReadWrite<K, V, R> extends BaseMutation<K, V, R> {

      @ProtoField(number = 3, name = "function")
      final MarshallableObject<Function<EntryView.ReadWriteEntryView<K, V>, R>> f;

      @ProtoFactory
      ReadWrite(DataConversion keyDataConversion, DataConversion valueDataConversion,
                MarshallableObject<Function<EntryView.ReadWriteEntryView<K, V>, R>> f) {
         super(keyDataConversion, valueDataConversion);
         this.f = f;
      }

      ReadWrite(DataConversion keyDataConversion, DataConversion valueDataConversion, Function<EntryView.ReadWriteEntryView<K, V>, R> f) {
         this(keyDataConversion, valueDataConversion, MarshallableObject.create(f));
      }

      @Override
      public R apply(EntryView.ReadWriteEntryView<K, V> view) {
         return f.get().apply(view);
      }
   }

   @ProtoTypeId(ProtoStreamTypeIds.MUTATIONS_READ_WRITE_WITH_VALUE)
   public static class ReadWriteWithValue<K, V, T, R> extends BaseMutation<K, V, R> {

      @ProtoField(number = 3)
      final MarshallableObject<?> argument;

      @ProtoField(number = 4, name = "function")
      final MarshallableObject<BiFunction<T, EntryView.ReadWriteEntryView<K, V>, R>> f;

      @ProtoFactory
      ReadWriteWithValue(DataConversion keyDataConversion, DataConversion valueDataConversion,
                         MarshallableObject<?> argument,
                         MarshallableObject<BiFunction<T, EntryView.ReadWriteEntryView<K, V>, R>> f) {
         super(keyDataConversion, valueDataConversion);
         this.argument = argument;
         this.f = f;
      }

      ReadWriteWithValue(DataConversion keyDataConversion, DataConversion valueDataConversion, Object argument, BiFunction<T, EntryView.ReadWriteEntryView<K, V>, R> f) {
         this(keyDataConversion, valueDataConversion,  MarshallableObject.create(argument), MarshallableObject.create(f));
      }

      @Override
      public R apply(EntryView.ReadWriteEntryView<K, V> view) {
         return f.get().apply((T) valueDataConversion.fromStorage(argument.get()), view);
      }
   }

   @ProtoTypeId(ProtoStreamTypeIds.MUTATIONS_WRITE)
   public static class Write<K, V> extends BaseMutation<K, V, Void> {

      @ProtoField(number = 3, name = "function")
      final MarshallableObject<Consumer<EntryView.WriteEntryView<K, V>>> f;

      @ProtoFactory
      Write(DataConversion keyDataConversion, DataConversion valueDataConversion,
            MarshallableObject<Consumer<EntryView.WriteEntryView<K, V>>> f) {
         super(keyDataConversion, valueDataConversion);
         this.f = f;
      }

      Write(DataConversion keyDataConversion, DataConversion valueDataConversion, Consumer<EntryView.WriteEntryView<K, V>> f) {
         this(keyDataConversion, valueDataConversion, MarshallableObject.create(f));
      }

      @Override
      public Void apply(EntryView.ReadWriteEntryView<K, V> view) {
         f.get().accept(view);
         return null;
      }
   }

   @ProtoTypeId(ProtoStreamTypeIds.MUTATIONS_WRITE_WITH_VALUE)
   public static class WriteWithValue<K, V, T> extends BaseMutation<K, V, Void> {

      @ProtoField(number = 3)
      final MarshallableObject<?> argument;

      @ProtoField(number = 4, name = "function")
      final MarshallableObject<BiConsumer<T, EntryView.WriteEntryView<K, V>>> f;

      @ProtoFactory
      WriteWithValue(DataConversion keyDataConversion, DataConversion valueDataConversion,
                     MarshallableObject<?> argument, MarshallableObject<BiConsumer<T, EntryView.WriteEntryView<K, V>>> f) {
         super(keyDataConversion, valueDataConversion);
         this.argument = argument;
         this.f = f;
      }

      WriteWithValue(DataConversion keyDataConversion, DataConversion valueDataConversion, Object argument, BiConsumer<T, EntryView.WriteEntryView<K, V>> f) {
         this(keyDataConversion, valueDataConversion, MarshallableObject.create(argument), MarshallableObject.create(f));
      }

      @Override
      public Void apply(EntryView.ReadWriteEntryView<K, V> view) {
         f.get().accept((T) valueDataConversion.fromStorage(argument.get()), view);
         return null;
      }
   }
}
