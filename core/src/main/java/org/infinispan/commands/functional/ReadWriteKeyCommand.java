package org.infinispan.commands.functional;

import java.util.function.Function;

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
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

// TODO: the command does not carry previous values to backup, so it can cause
// the values on primary and backup owners to diverge in case of topology change
@ProtoTypeId(ProtoStreamTypeIds.READ_WRITE_KEY_COMMAND)
public final class ReadWriteKeyCommand<K, V, R> extends AbstractWriteKeyCommand<K, V> {

   public static final byte COMMAND_ID = 50;

   @ProtoField(number = 10)
   final MarshallableObject<Function<ReadWriteEntryView<K, V>, R>> f;

   @ProtoFactory
   ReadWriteKeyCommand(MarshallableObject<?> wrappedKey, long flagsWithoutRemote, int topologyId, int segment,
                       CommandInvocationId commandInvocationId, Params params, ValueMatcher valueMatcher,
                       DataConversion keyDataConversion, DataConversion valueDataConversion,
                       MarshallableObject<Function<ReadWriteEntryView<K, V>, R>> f) {
      super(wrappedKey, flagsWithoutRemote, topologyId, segment, commandInvocationId, params, valueMatcher,
            keyDataConversion, valueDataConversion);
      this.f = f;
   }

   public ReadWriteKeyCommand(Object key, Function<ReadWriteEntryView<K, V>, R> f, int segment,
                              CommandInvocationId id, ValueMatcher valueMatcher, Params params,
                              DataConversion keyDataConversion,
                              DataConversion valueDataConversion) {
      super(key, valueMatcher, segment, id, params, keyDataConversion, valueDataConversion);
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
      return visitor.visitReadWriteKeyCommand(ctx, this);
   }

   @Override
   public LoadType loadType() {
      return LoadType.OWNER;
   }

   @Override
   public Mutation<K, V, ?> toMutation(Object key) {
      return new Mutations.ReadWrite<>(keyDataConversion, valueDataConversion, getFunction());
   }

   public Function<ReadWriteEntryView<K, V>, R> getFunction() {
      return MarshallableObject.unwrap(f);
   }

   @Override
   public String toString() {
      return "ReadWriteKeyCommand{" + "key=" + key +
            ", f=" + f.getClass().getName() +
            ", flags=" + printFlags() +
            ", commandInvocationId=" + commandInvocationId +
            ", topologyId=" + getTopologyId() +
            ", params=" + params +
            ", valueMatcher=" + valueMatcher +
            ", successful=" + successful +
            ", keyDataConversion=" + keyDataConversion +
            ", valueDataConversion=" + valueDataConversion +
            '}';
   }
}
