package org.infinispan.commands.functional;

import java.util.function.BiFunction;

import org.infinispan.commands.CommandInvocationId;
import org.infinispan.commands.Visitor;
import org.infinispan.commands.functional.functions.InjectableComponent;
import org.infinispan.commands.write.ValueMatcher;
import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.context.InvocationContext;
import org.infinispan.encoding.DataConversion;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.functional.EntryView.ReadWriteEntryView;
import org.infinispan.functional.impl.Params;
import org.infinispan.marshall.protostream.impl.MarshallableObject;
import org.infinispan.marshall.protostream.impl.MarshallableUserObject;
import org.infinispan.metadata.Metadata;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

@ProtoTypeId(ProtoStreamTypeIds.READ_WRITE_KEY_VALUE_COMMAND)
public final class ReadWriteKeyValueCommand<K, V, T, R> extends AbstractWriteKeyCommand<K, V> {

   public static final byte COMMAND_ID = 51;

   // TODO should this be MarshallableObject?
   @ProtoField(number = 10)
   final MarshallableUserObject<?> argument;

   @ProtoField(number = 11)
   final MarshallableObject<BiFunction<T, ReadWriteEntryView<K, V>, R>> f;

   @ProtoField(number = 12)
   MarshallableUserObject<?> prevValue;

   @ProtoField(number = 13)
   MarshallableObject<Metadata> prevMetadata;

   @ProtoFactory
   ReadWriteKeyValueCommand(MarshallableUserObject<?> wrappedKey, long flagsWithoutRemote, int topologyId, int segment,
                                   CommandInvocationId commandInvocationId, Params params, ValueMatcher valueMatcher,
                                   DataConversion keyDataConversion, DataConversion valueDataConversion,
                                   MarshallableUserObject<?> argument, MarshallableObject<BiFunction<T,
         ReadWriteEntryView<K, V>, R>> f, MarshallableUserObject<?> prevValue, MarshallableObject<Metadata> prevMetadata) {
      super(wrappedKey, flagsWithoutRemote, topologyId, segment, commandInvocationId, params, valueMatcher, keyDataConversion, valueDataConversion);
      this.argument = argument;
      this.f = f;
      this.prevValue = prevValue;
      this.prevMetadata = prevMetadata;
   }

   public ReadWriteKeyValueCommand(Object key, Object argument, BiFunction<T, ReadWriteEntryView<K, V>, R> f,
                                   int segment, CommandInvocationId id, ValueMatcher valueMatcher, Params params,
                                   DataConversion keyDataConversion, DataConversion valueDataConversion) {
      super(key, valueMatcher, segment, id, params, keyDataConversion, valueDataConversion);
      this.argument = MarshallableUserObject.create(argument);
      this.f = MarshallableObject.create(f);
   }

   @Override
   public void init(ComponentRegistry componentRegistry) {
      super.init(componentRegistry);
      if (f instanceof InjectableComponent)
         ((InjectableComponent) f).inject(componentRegistry);
   }

   @Override
   public byte getCommandId() {
      return COMMAND_ID;
   }

   @Override
   public boolean isConditional() {
      return true;
   }

   @Override
   public Object acceptVisitor(InvocationContext ctx, Visitor visitor) throws Throwable {
      return visitor.visitReadWriteKeyValueCommand(ctx, this);
   }

   @Override
   public LoadType loadType() {
      return LoadType.OWNER;
   }

   @Override
   public String toString() {
      return "ReadWriteKeyValueCommand{" +
            "key=" + key +
            ", argument=" + argument +
            ", f=" + getBiFunction().getClass().getName() +
            ", prevValue=" + prevValue +
            ", prevMetadata=" + prevMetadata +
            ", flags=" + printFlags() +
            ", commandInvocationId=" + commandInvocationId +
            ", topologyId=" + getTopologyId() +
            ", valueMatcher=" + valueMatcher +
            ", successful=" + successful +
            ", keyDataConversion=" + keyDataConversion +
            ", valueDataConversion=" + valueDataConversion +
            "}";
   }

   @Override
   public Mutation<K, V, R> toMutation(Object key) {
      return new Mutations.ReadWriteWithValue<>(keyDataConversion, valueDataConversion, getArgument(), getBiFunction());
   }

   public void setPrevValueAndMetadata(Object prevValue, Metadata prevMetadata) {
      this.prevValue = MarshallableUserObject.create(prevValue);
      this.prevMetadata = MarshallableObject.create(prevMetadata);
   }

   public Object getArgument() {
      return MarshallableUserObject.unwrap(argument);
   }

   public BiFunction<T, ReadWriteEntryView<K, V>, R> getBiFunction() {
      return MarshallableObject.unwrap(f);
   }

   public Object getPrevValue() {
      return MarshallableUserObject.unwrap(prevValue);
   }

   public Metadata getPrevMetadata() {
      return MarshallableObject.unwrap(prevMetadata);
   }
}
