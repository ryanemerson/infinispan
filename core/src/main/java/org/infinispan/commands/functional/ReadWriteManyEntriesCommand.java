package org.infinispan.commands.functional;

import java.util.Collection;
import java.util.Map;
import java.util.function.BiFunction;

import org.infinispan.commands.CommandInvocationId;
import org.infinispan.commands.Visitor;
import org.infinispan.commands.functional.functions.InjectableComponent;
import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.context.InvocationContext;
import org.infinispan.encoding.DataConversion;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.functional.EntryView.ReadWriteEntryView;
import org.infinispan.functional.impl.Params;
import org.infinispan.marshall.protostream.impl.MarshallableMap;
import org.infinispan.marshall.protostream.impl.MarshallableObject;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

// TODO: the command does not carry previous values to backup, so it can cause
// the values on primary and backup owners to diverge in case of topology change
@ProtoTypeId(ProtoStreamTypeIds.READ_WRITE_MANY_ENTRIES_COMMAND)
public final class ReadWriteManyEntriesCommand<K, V, T, R> extends AbstractWriteManyCommand<K, V> {

   public static final byte COMMAND_ID = 53;

   @ProtoField(number = 8)
   MarshallableMap<?, ?> arguments;

   @ProtoField(number = 9)
   MarshallableObject<BiFunction<T, ReadWriteEntryView<K, V>, R>> f;

   @ProtoFactory
   ReadWriteManyEntriesCommand(CommandInvocationId commandInvocationId, boolean forwarded, int topologyId,
                               Params params, long flags, DataConversion keyDataConversion,
                               DataConversion valueDataConversion, MarshallableMap<?, ?> arguments,
                               MarshallableObject<BiFunction<T, ReadWriteEntryView<K, V>, R>> f) {
      super(commandInvocationId, forwarded, topologyId, params, flags, keyDataConversion, valueDataConversion);
      this.arguments = arguments;
      this.f = f;
   }

   public ReadWriteManyEntriesCommand(Map<?, ?> arguments,
                                      BiFunction<T, ReadWriteEntryView<K, V>, R> f,
                                      Params params,
                                      CommandInvocationId commandInvocationId,
                                      DataConversion keyDataConversion,
                                      DataConversion valueDataConversion) {
      super(commandInvocationId, params, keyDataConversion, valueDataConversion);
      this.setArguments(arguments);
      this.f = MarshallableObject.create(f);
   }

   public ReadWriteManyEntriesCommand(ReadWriteManyEntriesCommand command) {
      super(command);
      this.arguments = command.arguments;
      this.f = command.f;
   }

   @Override
   public void init(ComponentRegistry componentRegistry) {
      super.init(componentRegistry);
      if (f instanceof InjectableComponent)
         ((InjectableComponent) f).inject(componentRegistry);
   }

   public BiFunction<T, ReadWriteEntryView<K, V>, R> getBiFunction() {
      return MarshallableObject.unwrap(f);
   }

   public Map<?, ?> getArguments() {
      return MarshallableMap.unwrap(arguments);
   }

   public void setArguments(Map<?, ?> arguments) {
      this.arguments = MarshallableMap.create(arguments);
   }

   public final ReadWriteManyEntriesCommand<K, V, T, R> withArguments(Map<?, ?> entries) {
      setArguments(entries);
      return this;
   }

   @Override
   public byte getCommandId() {
      return COMMAND_ID;
   }

   @Override
   public Object acceptVisitor(InvocationContext ctx, Visitor visitor) throws Throwable {
      return visitor.visitReadWriteManyEntriesCommand(ctx, this);
   }

   @Override
   public Collection<?> getAffectedKeys() {
      return getArguments().keySet();
   }

   public LoadType loadType() {
      return LoadType.OWNER;
   }

   @Override
   public String toString() {
      return "ReadWriteManyEntriesCommand{" + "arguments=" + arguments +
            ", f=" + f.getClass().getName() +
            ", forwarded=" + forwarded +
            ", keyDataConversion=" + keyDataConversion +
            ", valueDataConversion=" + valueDataConversion +
            '}';
   }

   @Override
   public Collection<?> getKeysToLock() {
      return getAffectedKeys();
   }

   @Override
   public Mutation<K, V, ?> toMutation(Object key) {
      return new Mutations.ReadWriteWithValue(keyDataConversion, valueDataConversion, getArguments().get(key), getBiFunction());
   }
}
