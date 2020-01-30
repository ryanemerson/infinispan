package org.infinispan.commands.functional.functions;

import java.io.Serializable;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.infinispan.cache.impl.BiFunctionMapper;
import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.functional.EntryView;
import org.infinispan.marshall.protostream.impl.MarshallableObject;
import org.infinispan.marshall.protostream.impl.MarshallableUserObject;
import org.infinispan.metadata.Metadata;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.util.UserRaisedFunctionalException;

@ProtoTypeId(ProtoStreamTypeIds.MERGE_FUNCTION)
public class MergeFunction<K, V> implements Function<EntryView.ReadWriteEntryView<K, V>, V>, InjectableComponent, Serializable {
   private BiFunction<? super V, ? super V, ? extends V> remappingFunction;
   private V value;
   private Metadata metadata;

   public MergeFunction(V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction, Metadata metadata) {
      this.remappingFunction = remappingFunction;
      this.value = value;
      this.metadata = metadata;
   }

   @ProtoFactory
   MergeFunction(MarshallableUserObject<V> value, MarshallableObject<BiFunction<? super V, ? super V, ? extends V>> remappingFunction,
                 MarshallableObject<Metadata> metadata) {
      this(MarshallableUserObject.unwrap(value), MarshallableObject.unwrap(remappingFunction),
            MarshallableObject.unwrap(metadata));
   }

   @ProtoField(number = 1)
   MarshallableUserObject<V> getValue() {
      return MarshallableUserObject.create(value);
   }

   @ProtoField(number = 2)
   MarshallableObject<BiFunction<? super V, ? super V, ? extends V>> getRemappingFunction() {
      return MarshallableObject.create(remappingFunction);
   }

   @ProtoField(number = 3)
   MarshallableObject<Metadata> getMetadata() {
      return MarshallableObject.create(metadata);
   }

   @Override
   public V apply(EntryView.ReadWriteEntryView<K, V> entry) {
      try {
         V merged = value;
         if (entry.find().isPresent()) {
            V t = entry.get();
            if (remappingFunction instanceof BiFunctionMapper) {
               BiFunctionMapper mapper = (BiFunctionMapper) this.remappingFunction;
               Object toStorage = mapper.getValueDataConversion().toStorage(t);
               merged = remappingFunction.apply((V) toStorage, value);
            } else {
               merged = remappingFunction.apply(t, value);
            }
         }
         if (merged == null) {
            entry.set(merged);
         } else if (remappingFunction instanceof BiFunctionMapper) {
            BiFunctionMapper mapper = (BiFunctionMapper) this.remappingFunction;
            Object fromStorage = mapper.getValueDataConversion().fromStorage(merged);
            entry.set((V) fromStorage, metadata);
         } else {
            entry.set(merged, metadata);
         }
         return merged;
      } catch (Exception ex) {
         throw new UserRaisedFunctionalException(ex);
      }
   }

   @Override
   public void inject(ComponentRegistry registry) {
      registry.wireDependencies(this);
      registry.wireDependencies(remappingFunction);
   }
}
