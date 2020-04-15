package org.infinispan.commands.functional;

import java.util.function.Function;

import org.infinispan.commands.Visitor;
import org.infinispan.commands.functional.functions.InjectableComponent;
import org.infinispan.commands.read.AbstractDataCommand;
import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.context.InvocationContext;
import org.infinispan.encoding.DataConversion;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.functional.EntryView.ReadEntryView;
import org.infinispan.functional.impl.EntryViews;
import org.infinispan.functional.impl.Params;
import org.infinispan.functional.impl.StatsEnvelope;
import org.infinispan.marshall.protostream.impl.MarshallableObject;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

@ProtoTypeId(ProtoStreamTypeIds.READ_ONLY_KEY_COMMAND)
public class ReadOnlyKeyCommand<K, V, R> extends AbstractDataCommand {

   public static final int COMMAND_ID = 62;

   @ProtoField(number = 5)
   final MarshallableObject<Function<ReadEntryView<K, V>, R>> f;

   @ProtoField(number = 6)
   final Params params;

   @ProtoField(number = 7)
   final DataConversion keyDataConversion;

   @ProtoField(number = 8)
   final DataConversion valueDataConversion;

   @ProtoFactory
   ReadOnlyKeyCommand(MarshallableObject<?> wrappedKey, long flagsWithoutRemote, int topologyId, int segment,
                      MarshallableObject<Function<ReadEntryView<K, V>, R>> f, Params params, DataConversion keyDataConversion,
                      DataConversion valueDataConversion) {
      super(wrappedKey, flagsWithoutRemote, topologyId, segment);
      this.f = f;
      this.params = params;
      this.keyDataConversion = keyDataConversion;
      this.valueDataConversion = valueDataConversion;
   }

   public ReadOnlyKeyCommand(Object key, Function<ReadEntryView<K, V>, R> f, int segment, Params params,
                             DataConversion keyDataConversion,
                             DataConversion valueDataConversion) {
      super(key, segment, params.toFlagsBitSet());
      this.f = MarshallableObject.create(f);
      this.params = params;
      this.keyDataConversion = keyDataConversion;
      this.valueDataConversion = valueDataConversion;
   }

   @Override
   public void init(ComponentRegistry componentRegistry) {
      componentRegistry.wireDependencies(keyDataConversion);
      componentRegistry.wireDependencies(valueDataConversion);
      if (f instanceof InjectableComponent)
         ((InjectableComponent) f).inject(componentRegistry);
   }

   @Override
   public byte getCommandId() {
      return COMMAND_ID;
   }

   @Override
   public Object acceptVisitor(InvocationContext ctx, Visitor visitor) throws Throwable {
      return visitor.visitReadOnlyKeyCommand(ctx, this);
   }

   @Override
   public LoadType loadType() {
      return LoadType.OWNER;
   }

   /**
    * Apply function on entry without any data
    */
   public Object performOnLostData() {
      return StatsEnvelope.create(getFunction().apply(EntryViews.noValue(getKey(), keyDataConversion)), true);
   }

   @Override
   public String toString() {
      return "ReadOnlyKeyCommand{" + ", key=" + key +
            ", f=" + f.getClass().getName() +
            ", keyDataConversion=" + keyDataConversion +
            ", valueDataConversion=" + valueDataConversion +
            '}';
   }

   public Function<ReadEntryView<K, V>, R> getFunction() {
      return MarshallableObject.unwrap(f);
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
}
