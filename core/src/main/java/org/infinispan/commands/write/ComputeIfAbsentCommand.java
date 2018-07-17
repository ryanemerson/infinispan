package org.infinispan.commands.write;

import static org.infinispan.commons.util.Util.toStr;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Function;

import org.infinispan.commands.CommandInvocationId;
import org.infinispan.commands.MetadataAwareCommand;
import org.infinispan.commands.Visitor;
import org.infinispan.commons.io.UnsignedNumeric;
import org.infinispan.commons.marshall.UserObjectInput;
import org.infinispan.container.entries.MVCCEntry;
import org.infinispan.context.InvocationContext;
import org.infinispan.context.impl.FlagBitSets;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.marshall.MarshalledEntryUtil;
import org.infinispan.marshall.core.MarshalledEntryFactory;
import org.infinispan.marshall.core.MarshalledEntryImpl;
import org.infinispan.commons.marshall.UserObjectOutput;
import org.infinispan.metadata.Metadata;
import org.infinispan.metadata.Metadatas;
import org.infinispan.notifications.cachelistener.CacheNotifier;
import org.infinispan.util.UserRaisedFunctionalException;

public class ComputeIfAbsentCommand extends AbstractDataWriteCommand implements MetadataAwareCommand {

   public static final int COMMAND_ID = 69;

   private Function mappingFunction;
   private Metadata metadata;
   private CacheNotifier<Object, Object> notifier;
   private boolean successful = false;

   public ComputeIfAbsentCommand() {
   }

   public ComputeIfAbsentCommand(Object key,
                                 Function mappingFunction,
                                 int segment, long flagsBitSet,
                                 CommandInvocationId commandInvocationId,
                                 Metadata metadata,
                                 CacheNotifier notifier,
                                 ComponentRegistry componentRegistry) {

      super(key, segment, flagsBitSet, commandInvocationId);
      this.mappingFunction = mappingFunction;
      this.metadata = metadata;
      this.notifier = notifier;
      componentRegistry.wireDependencies(this.mappingFunction);
   }

   public void init(CacheNotifier notifier, ComponentRegistry componentRegistry) {
      //noinspection unchecked
      this.notifier = notifier;
      componentRegistry.wireDependencies(mappingFunction);
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

   @Override
   public Object perform(InvocationContext ctx) throws Throwable {
      MVCCEntry<Object, Object> e = (MVCCEntry) ctx.lookupEntry(key);

      if (e == null) {
         throw new IllegalStateException("Not wrapped");
      }

      Object value = e.getValue();

      if (value == null) {
         try {
            value = mappingFunction.apply(key);
         } catch (RuntimeException ex) {
            throw new UserRaisedFunctionalException(ex);
         }

         if (value != null) {
            e.setValue(value);
            Metadatas.updateMetadata(e, metadata);
            if (e.isCreated()) {
               notifier.notifyCacheEntryCreated(key, value, metadata, true, ctx, this);
            }
            if (e.isRemoved()) {
               e.setCreated(true);
               e.setExpired(false);
               e.setRemoved(false);
            }
            e.setChanged(true);
         }
         successful = true;
      }
      return value;
   }

   public Function getMappingFunction() {
      return mappingFunction;
   }

   @Override
   public byte getCommandId() {
      return COMMAND_ID;
   }

   @Override
   public void writeTo(UserObjectOutput output, MarshalledEntryFactory entryFactory) throws IOException {
      output.writeEntry(key, null, metadata);
      output.writeObject(mappingFunction);
      UnsignedNumeric.writeUnsignedInt(output, segment);
      CommandInvocationId.writeTo(output, commandInvocationId);
      output.writeLong(FlagBitSets.copyWithoutRemotableFlags(getFlagsBitSet()));
   }

   @Override
   public void readFrom(UserObjectInput input) throws IOException, ClassNotFoundException {
      MarshalledEntryImpl me = MarshalledEntryUtil.read(input);
      key = me.getKey();
      metadata = me.metadata();
      mappingFunction = (Function) input.readObject();
      segment = UnsignedNumeric.readUnsignedInt(input);
      commandInvocationId = CommandInvocationId.readFrom(input);
      setFlagsBitSet(input.readLong());
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
      return Objects.equals(mappingFunction, this.mappingFunction);
   }

   @Override
   public int hashCode() {
      return Objects.hash(super.hashCode(), mappingFunction, metadata);
   }

   @Override
   public String toString() {
      return "ComputeIfAbsentCommand{" +
            "key=" + toStr(key) +
            ", mappingFunction=" + toStr(mappingFunction) +
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
