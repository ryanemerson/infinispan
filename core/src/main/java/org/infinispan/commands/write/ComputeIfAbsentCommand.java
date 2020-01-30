package org.infinispan.commands.write;

import java.util.Objects;
import java.util.function.Function;

import org.infinispan.commands.CommandInvocationId;
import org.infinispan.commands.MetadataAwareCommand;
import org.infinispan.commands.Visitor;
import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.context.InvocationContext;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.marshall.protostream.impl.MarshallableObject;
import org.infinispan.marshall.protostream.impl.MarshallableUserObject;
import org.infinispan.metadata.Metadata;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

@ProtoTypeId(ProtoStreamTypeIds.COMPUTE_IF_ABSENT_COMMAND)
public class ComputeIfAbsentCommand extends AbstractDataWriteCommand implements MetadataAwareCommand {

   public static final int COMMAND_ID = 69;

   private transient boolean successful = true;

   @ProtoField(number = 6)
   final MarshallableObject<Function<?, ?>> mappingFunction;

   @ProtoField(number = 7)
   MarshallableObject<Metadata> metadata;

   @ProtoFactory
   ComputeIfAbsentCommand(MarshallableUserObject<?> wrappedKey, long flagsWithoutRemote, int topologyId, int segment,
                          CommandInvocationId commandInvocationId, MarshallableObject<Function<?, ?>> mappingFunction,
                          MarshallableObject<Metadata> metadata) {
      super(wrappedKey, flagsWithoutRemote, topologyId, segment, commandInvocationId);
      this.mappingFunction = mappingFunction;
      this.metadata = metadata;
   }

   public ComputeIfAbsentCommand(Object key, Function<?, ?> mappingFunction, int segment, long flagsBitSet,
                                 CommandInvocationId commandInvocationId, Metadata metadata) {
      super(key, segment, flagsBitSet, commandInvocationId);
      this.mappingFunction = MarshallableObject.create(mappingFunction);
      this.setMetadata(metadata);
   }

   @Override
   public void init(ComponentRegistry componentRegistry) {
      componentRegistry.wireDependencies(mappingFunction);
   }

   @Override
   public Metadata getMetadata() {
      return MarshallableObject.unwrap(metadata);
   }

   @Override
   public void setMetadata(Metadata metadata) {
      this.metadata = MarshallableObject.create(metadata);
   }

   @Override
   public boolean isSuccessful() {
      return successful;
   }

   @Override
   public boolean isConditional() {
      return false;
   }

   @Override
   public ValueMatcher getValueMatcher() {
      return ValueMatcher.MATCH_ALWAYS;
   }

   @Override
   public void setValueMatcher(ValueMatcher valueMatcher) {
      //implementation not needed
   }

   @Override
   public void fail() {
      successful = false;
   }

   public Function getMappingFunction() {
      return MarshallableObject.unwrap(mappingFunction);
   }

   @Override
   public byte getCommandId() {
      return COMMAND_ID;
   }

   @Override
   public Object acceptVisitor(InvocationContext ctx, Visitor visitor) throws Throwable {
      return visitor.visitComputeIfAbsentCommand(ctx, this);
   }

   @Override
   public LoadType loadType() {
      return LoadType.PRIMARY;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      if (!super.equals(o)) return false;

      ComputeIfAbsentCommand that = (ComputeIfAbsentCommand) o;
      if (!Objects.equals(metadata, that.metadata)) return false;
      // TODO fix this for .that ... wait until all tests passing to make sure this doesn't subtly break anything
      return Objects.equals(mappingFunction, this.mappingFunction);
   }

   @Override
   public int hashCode() {
      return Objects.hash(super.hashCode(), mappingFunction, metadata);
   }

   @Override
   public String toString() {
      return "ComputeIfAbsentCommand{" +
            "key=" + key +
            ", mappingFunction=" + mappingFunction +
            ", metadata=" + metadata +
            ", flags=" + printFlags() +
            ", successful=" + isSuccessful() +
            ", valueMatcher=" + getValueMatcher() +
            ", topologyId=" + getTopologyId() +
            '}';
   }

   @Override
   public final boolean isReturnValueExpected() {
      return true;
   }
}
