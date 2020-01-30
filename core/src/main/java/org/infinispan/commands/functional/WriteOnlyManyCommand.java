package org.infinispan.commands.functional;

import java.util.Collection;
import java.util.function.Consumer;

import org.infinispan.commands.CommandInvocationId;
import org.infinispan.commands.Visitor;
import org.infinispan.commands.functional.functions.InjectableComponent;
import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.context.InvocationContext;
import org.infinispan.encoding.DataConversion;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.functional.EntryView.WriteEntryView;
import org.infinispan.functional.impl.Params;
import org.infinispan.marshall.protostream.impl.MarshallableCollection;
import org.infinispan.marshall.protostream.impl.MarshallableObject;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

@ProtoTypeId(ProtoStreamTypeIds.WRITE_ONLY_MANY_COMMAND)
public final class WriteOnlyManyCommand<K, V> extends AbstractWriteManyCommand<K, V> {

   public static final byte COMMAND_ID = 56;

   @ProtoField(number = 8)
   MarshallableCollection<?> keys;

   @ProtoField(number = 9)
   final MarshallableObject<Consumer<WriteEntryView<K, V>>> f;

   @ProtoFactory
   WriteOnlyManyCommand(CommandInvocationId commandInvocationId, boolean forwarded, int topologyId, Params params,
                        long flags, DataConversion keyDataConversion, DataConversion valueDataConversion,
                        MarshallableCollection<?> keys, MarshallableObject<Consumer<WriteEntryView<K, V>>> f) {
      super(commandInvocationId, forwarded, topologyId, params, flags, keyDataConversion, valueDataConversion);
      this.keys = keys;
      this.f = f;
   }

   public WriteOnlyManyCommand(Collection<?> keys, Consumer<WriteEntryView<K, V>> f, Params params,
                               CommandInvocationId commandInvocationId, DataConversion keyDataConversion,
                               DataConversion valueDataConversion) {
      super(commandInvocationId, params, keyDataConversion, valueDataConversion);
      this.setKeys(keys);
      this.f = MarshallableObject.create(f);
   }

   public WriteOnlyManyCommand(WriteOnlyManyCommand<K, V> command) {
      super(command);
      this.keys = command.keys;
      this.f = command.f;
   }

   @Override
   public void init(ComponentRegistry componentRegistry) {
      super.init(componentRegistry);
      if (f instanceof InjectableComponent)
         ((InjectableComponent) f).inject(componentRegistry);
   }

   public Consumer<WriteEntryView<K, V>> getConsumer() {
      return MarshallableObject.unwrap(f);
   }

   public void setKeys(Collection<?> keys) {
      this.keys = MarshallableCollection.create(keys);
   }

   public final WriteOnlyManyCommand<K, V> withKeys(Collection<?> keys) {
      setKeys(keys);
      return this;
   }

   @Override
   public byte getCommandId() {
      return COMMAND_ID;
   }

   @Override
   public Object acceptVisitor(InvocationContext ctx, Visitor visitor) throws Throwable {
      return visitor.visitWriteOnlyManyCommand(ctx, this);
   }

   @Override
   public Collection<?> getAffectedKeys() {
      return MarshallableCollection.unwrap(keys);
   }

   @Override
   public LoadType loadType() {
      return LoadType.DONT_LOAD;
   }

   @Override
   public String toString() {
      return "WriteOnlyManyCommand{" + "keys=" + keys +
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
      return new Mutations.Write<>(keyDataConversion, valueDataConversion, getConsumer());
   }
}
