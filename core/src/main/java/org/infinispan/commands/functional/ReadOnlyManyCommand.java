package org.infinispan.commands.functional;

import java.util.Collection;
import java.util.function.Function;

import org.infinispan.commands.AbstractTopologyAffectedCommand;
import org.infinispan.commands.Visitor;
import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.context.InvocationContext;
import org.infinispan.encoding.DataConversion;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.functional.EntryView.ReadEntryView;
import org.infinispan.functional.impl.Params;
import org.infinispan.marshall.protostream.impl.MarshallableObject;
import org.infinispan.marshall.protostream.impl.MarshallableUserCollection;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

@ProtoTypeId(ProtoStreamTypeIds.READ_ONLY_MANY_COMMAND)
public class ReadOnlyManyCommand<K, V, R> extends AbstractTopologyAffectedCommand {
   public static final int COMMAND_ID = 63;

   @ProtoField(number = 3)
   protected MarshallableUserCollection<?> keys;

   @ProtoField(number = 4)
   protected MarshallableObject<Function<ReadEntryView<K, V>, R>> function;

   @ProtoField(number = 5)
   protected Params params;

   @ProtoField(number = 6)
   protected DataConversion keyDataConversion;

   @ProtoField(number = 7)
   protected DataConversion valueDataConversion;

   @ProtoFactory
   ReadOnlyManyCommand(long flagsWithoutRemote, int topologyId, MarshallableUserCollection<?> keys,
                       MarshallableObject<Function<ReadEntryView<K, V>, R>> function, Params params,
                       DataConversion keyDataConversion, DataConversion valueDataConversion) {
      super(flagsWithoutRemote, topologyId);
      this.keys = keys;
      this.function = function;
      this.params = params;
      this.keyDataConversion = keyDataConversion;
      this.valueDataConversion = valueDataConversion;
   }

   public ReadOnlyManyCommand(Collection<?> keys,
                              Function<ReadEntryView<K, V>, R> function,
                              Params params,
                              DataConversion keyDataConversion,
                              DataConversion valueDataConversion) {
      this(params.toFlagsBitSet(), -1, MarshallableUserCollection.create(keys), MarshallableObject.create(function),
            params, keyDataConversion, valueDataConversion);
   }

   public ReadOnlyManyCommand(ReadOnlyManyCommand<K, V, R> c) {
      this(c.flags, c.topologyId, c.keys, c.function, c.params, c.keyDataConversion, c.valueDataConversion);
   }

   @Override
   public void init(ComponentRegistry componentRegistry) {
      componentRegistry.wireDependencies(keyDataConversion);
      componentRegistry.wireDependencies(valueDataConversion);
   }

   public Collection<?> getKeys() {
      return MarshallableUserCollection.unwrap(keys);
   }

   public void setKeys(Collection<?> keys) {
      this.keys = MarshallableUserCollection.create(keys);
   }

   public final ReadOnlyManyCommand<K, V, R> withKeys(Collection<?> keys) {
      setKeys(keys);
      return this;
   }

   @Override
   public byte getCommandId() {
      return COMMAND_ID;
   }

   @Override
   public boolean isReturnValueExpected() {
      return true;
   }

   @Override
   public Object acceptVisitor(InvocationContext ctx, Visitor visitor) throws Throwable {
      return visitor.visitReadOnlyManyCommand(ctx, this);
   }

   @Override
   public LoadType loadType() {
      return LoadType.OWNER;
   }


   public DataConversion getKeyDataConversion() {
      return keyDataConversion;
   }

   public DataConversion getValueDataConversion() {
      return valueDataConversion;
   }

   public Params getParams() {
      return params;
   }

   public Function<ReadEntryView<K, V>, R> getFunction() {
      return MarshallableObject.unwrap(function);
   }

   @Override
   public String toString() {
      return "ReadOnlyManyCommand{" + ", keys=" + keys +
            ", f=" + function.getClass().getName() +
            ", keyDataConversion=" + keyDataConversion +
            ", valueDataConversion=" + valueDataConversion +
            '}';
   }
}
