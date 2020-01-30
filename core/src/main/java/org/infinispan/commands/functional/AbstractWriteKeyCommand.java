package org.infinispan.commands.functional;

import static org.infinispan.commons.util.Util.toStr;

import org.infinispan.commands.CommandInvocationId;
import org.infinispan.commands.write.AbstractDataWriteCommand;
import org.infinispan.commands.write.ValueMatcher;
import org.infinispan.encoding.DataConversion;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.functional.impl.Params;
import org.infinispan.marshall.protostream.impl.MarshallableUserObject;
import org.infinispan.protostream.annotations.ProtoField;

public abstract class AbstractWriteKeyCommand<K, V> extends AbstractDataWriteCommand implements FunctionalCommand<K, V> {

   transient boolean successful = true;

   @ProtoField(number = 6)
   final Params params;

   @ProtoField(number = 7)
   ValueMatcher valueMatcher;

   @ProtoField(number = 8)
   final DataConversion keyDataConversion;

   @ProtoField(number = 9)
   final DataConversion valueDataConversion;


   protected AbstractWriteKeyCommand(MarshallableUserObject<?> wrappedKey, long flags, int topologyId, int segment,
                                     CommandInvocationId commandInvocationId, Params params, ValueMatcher valueMatcher,
                                     DataConversion keyDataConversion, DataConversion valueDataConversion) {
      super(wrappedKey, flags, topologyId, segment, commandInvocationId);
      this.params = params;
      this.valueMatcher = valueMatcher;
      this.keyDataConversion = keyDataConversion;
      this.valueDataConversion = valueDataConversion;
   }

   protected AbstractWriteKeyCommand(Object key, ValueMatcher valueMatcher, int segment,
                                     CommandInvocationId id, Params params,
                                     DataConversion keyDataConversion,
                                     DataConversion valueDataConversion) {
      super(key, segment, params.toFlagsBitSet(), id);
      this.valueMatcher = valueMatcher;
      this.params = params;
      this.keyDataConversion = keyDataConversion;
      this.valueDataConversion = valueDataConversion;
   }

   @Override
   public void init(ComponentRegistry componentRegistry) {
      componentRegistry.wireDependencies(keyDataConversion);
      componentRegistry.wireDependencies(valueDataConversion);
   }

   @Override
   public ValueMatcher getValueMatcher() {
      return valueMatcher;
   }

   @Override
   public void setValueMatcher(ValueMatcher valueMatcher) {
      this.valueMatcher = valueMatcher;
   }

   @Override
   public boolean isSuccessful() {
      return successful;
   }

   @Override
   public Params getParams() {
      return params;
   }

   @Override
   public void fail() {
      successful = false;
   }

   @Override
   public String toString() {
      return getClass().getSimpleName() +
            " {key=" + toStr(key) +
            ", flags=" + printFlags() +
            ", commandInvocationId=" + commandInvocationId +
            ", params=" + params +
            ", valueMatcher=" + valueMatcher +
            ", successful=" + successful +
            "}";
   }

   @Override
   public DataConversion getKeyDataConversion() {
      return keyDataConversion;
   }

   @Override
   public DataConversion getValueDataConversion() {
      return valueDataConversion;
   }
}
