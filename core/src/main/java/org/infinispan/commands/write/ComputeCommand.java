package org.infinispan.commands.write;

import static org.infinispan.commons.util.Util.toStr;

import java.util.Objects;
import java.util.function.BiFunction;

import org.infinispan.commands.CommandInvocationId;
import org.infinispan.commands.MetadataAwareCommand;
import org.infinispan.commands.Visitor;
import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.context.InvocationContext;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.marshall.protostream.impl.MarshallableObject;
import org.infinispan.metadata.Metadata;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

@ProtoTypeId(ProtoStreamTypeIds.COMPUTE_COMMAND)
public class ComputeCommand extends AbstractDataWriteCommand implements MetadataAwareCommand {

   public static final int COMMAND_ID = 68;

   private BiFunction<?, ?, ?> remappingBiFunction;
   private Metadata metadata;
   private boolean computeIfPresent;
   private transient boolean successful = true;

   public ComputeCommand(Object key, BiFunction<?, ?, ?> remappingBiFunction, boolean computeIfPresent, int segment,
                         long flagsBitSet, CommandInvocationId commandInvocationId, Metadata metadata) {
      super(key, segment, flagsBitSet, commandInvocationId);
      this.remappingBiFunction = remappingBiFunction;
      this.computeIfPresent = computeIfPresent;
      this.setMetadata(metadata);
   }

   @ProtoFactory
   ComputeCommand(MarshallableObject<?> wrappedKey, long flagsWithoutRemote, int topologyId, int segment,
                  CommandInvocationId commandInvocationId, MarshallableObject<BiFunction<?, ?, ?>> wrappedRemappingBiFunction,
                  MarshallableObject<Metadata> wrappedMetadata, boolean computeIfPresent) {
      super(wrappedKey, flagsWithoutRemote, topologyId, segment, commandInvocationId);
      this.remappingBiFunction = MarshallableObject.unwrap(wrappedRemappingBiFunction);
      this.metadata = MarshallableObject.unwrap(wrappedMetadata);
      this.computeIfPresent = computeIfPresent;
   }

   @ProtoField(number = 6, name = "remappingBiFunction")
   MarshallableObject<BiFunction<?, ?, ?>> getWrappedRemappingBiFunction() {
      return MarshallableObject.create(remappingBiFunction);
   }

   @ProtoField(number = 7, name = "metadata")
   MarshallableObject<Metadata> getWrappedMetadata() {
      return MarshallableObject.create(metadata);
   }

   @ProtoField(number = 8, defaultValue = "false")
   public boolean isComputeIfPresent() {
      return computeIfPresent;
   }

   public void setComputeIfPresent(boolean computeIfPresent) {
      this.computeIfPresent = computeIfPresent;
   }

   @Override
   public void init(ComponentRegistry componentRegistry) {
      componentRegistry.wireDependencies(remappingBiFunction);
   }

   @Override
   public Metadata getMetadata() {
      return metadata;
   }

   @Override
   public void setMetadata(Metadata metadata) {
      this.metadata = metadata;
   }

   @Override
   public boolean isSuccessful() {
      return successful;
   }

   @Override
   public boolean isConditional() {
      return isComputeIfPresent();
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

   @Override
   public byte getCommandId() {
      return COMMAND_ID;
   }

   public BiFunction getRemappingBiFunction() {
      return remappingBiFunction;
   }

   @Override
   public Object acceptVisitor(InvocationContext ctx, Visitor visitor) throws Throwable {
      return visitor.visitComputeCommand(ctx, this);
   }

   @Override
   public LoadType loadType() {
      return LoadType.OWNER;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      if (!super.equals(o)) return false;

      ComputeCommand that = (ComputeCommand) o;
      if (!Objects.equals(metadata, that.metadata)) return false;
      return Objects.equals(computeIfPresent, that.computeIfPresent);
   }

   @Override
   public int hashCode() {
      return Objects.hash(super.hashCode(), computeIfPresent, remappingBiFunction, metadata);
   }

   @Override
   public String toString() {
      return "ComputeCommand{" +
            "key=" + toStr(key) +
            ", isComputeIfPresent=" + toStr(computeIfPresent) +
            ", remappingBiFunction=" + toStr(remappingBiFunction) +
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
