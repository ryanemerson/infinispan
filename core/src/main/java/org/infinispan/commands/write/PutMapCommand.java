package org.infinispan.commands.write;

import static org.infinispan.commons.util.Util.toStr;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.infinispan.commands.AbstractTopologyAffectedCommand;
import org.infinispan.commands.CommandInvocationId;
import org.infinispan.commands.MetadataAwareCommand;
import org.infinispan.commands.Visitor;
import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.context.InvocationContext;
import org.infinispan.context.impl.FlagBitSets;
import org.infinispan.marshall.protostream.impl.MarshallableMap;
import org.infinispan.marshall.protostream.impl.MarshallableObject;
import org.infinispan.metadata.Metadata;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.util.concurrent.locks.RemoteLockCommand;

/**
 * A command writing multiple key/value pairs with the same metadata.
 *
 * <p>Note: In scattered caches, push state transfer uses {@code PutMapCommand} but wraps the values in
 * {@code InternalCacheValue} instances in order to preserve the original metadata and timestamps.</p>
 *
 * @author Mircea.Markus@jboss.com
 * @since 4.0
 */
@ProtoTypeId(ProtoStreamTypeIds.PUT_MAP_COMMAND)
public class PutMapCommand extends AbstractTopologyAffectedCommand implements WriteCommand, MetadataAwareCommand, RemoteLockCommand {
   public static final byte COMMAND_ID = 9;

   private Map<Object, Object> map;
   private Metadata metadata;
   private boolean isForwarded = false;
   private CommandInvocationId commandInvocationId;

   @SuppressWarnings("unchecked")
   public PutMapCommand(Map<?, ?> map, Metadata metadata, long flagsBitSet, CommandInvocationId commandInvocationId) {
      super(flagsBitSet, -1);
      this.map = (Map<Object, Object>) map;
      this.metadata = metadata;
      this.commandInvocationId = commandInvocationId;
   }

   public PutMapCommand(PutMapCommand c) {
      this(c.map, c.metadata, c.flags, c.commandInvocationId);
      this.isForwarded = c.isForwarded;
   }

   @ProtoFactory
   PutMapCommand(long flagsWithoutRemote, int topologyId, MarshallableMap<Object, Object> wrappedMap,
                 MarshallableObject<Metadata> wrappedMetadata, boolean forwarded, CommandInvocationId commandInvocationId) {
      super(flagsWithoutRemote, topologyId);
      this.map = MarshallableMap.unwrap(wrappedMap);
      this.metadata = MarshallableObject.unwrap(wrappedMetadata);
      this.isForwarded = forwarded;
      this.commandInvocationId = commandInvocationId;
   }

   @ProtoField(number = 3, name="wrappedMap")
   MarshallableMap<Object, Object> getWrappedMap() {
      return MarshallableMap.create(map);
   }

   @ProtoField(number = 4, name = "metadata")
   MarshallableObject<Metadata> getWrappedMetadata() {
      return MarshallableObject.create(metadata);
   }

   @ProtoField(number = 5)
   public CommandInvocationId getCommandInvocationId() {
      return commandInvocationId;
   }

   /**
    * For non transactional caches that support concurrent writes (default), the commands are forwarded between nodes,
    * e.g.: - commands is executed on node A, but some of the keys should be locked on node B - the command is send to
    * the main owner (B) - B tries to acquire lock on the keys it owns, then forwards the commands to the other owners
    * as well - at this last stage, the command has the "isForwarded" flag set to true.
    */
   @ProtoField(number = 6, defaultValue = "false")
   public boolean isForwarded() {
      return isForwarded;
   }

   @Override
   public Object acceptVisitor(InvocationContext ctx, Visitor visitor) throws Throwable {
      return visitor.visitPutMapCommand(ctx, this);
   }

   @Override
   public Collection<?> getKeysToLock() {
      return isForwarded ? Collections.emptyList() : Collections.unmodifiableCollection(getAffectedKeys());
   }

   @Override
   public Object getKeyLockOwner() {
      return commandInvocationId;
   }

   @Override
   public boolean hasZeroLockAcquisition() {
      return hasAnyFlag(FlagBitSets.ZERO_LOCK_ACQUISITION_TIMEOUT);
   }

   @Override
   public boolean hasSkipLocking() {
      return hasAnyFlag(FlagBitSets.SKIP_LOCKING);
   }

   public Map<Object, Object> getMap() {
      return map;
   }

   public void setMap(Map<Object, Object> map) {
      this.map = map;
   }

   public final PutMapCommand withMap(Map<Object, Object> map) {
      setMap(map);
      return this;
   }

   @Override
   public byte getCommandId() {
      return COMMAND_ID;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      PutMapCommand that = (PutMapCommand) o;
      return Objects.equals(map, that.map) &&
            Objects.equals(metadata, that.metadata);
   }

   @Override
   public int hashCode() {
      return Objects.hash(map, metadata);
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("PutMapCommand{map={");
      if (!map.isEmpty()) {
         Iterator<Entry<Object, Object>> it = map.entrySet().iterator();
         int i = 0;
         for (;;) {
            Entry<Object, Object> e = it.next();
            sb.append(toStr(e.getKey())).append('=').append(toStr(e.getValue()));
            if (!it.hasNext()) {
               break;
            }
            if (i > 100) {
               sb.append(" ...");
               break;
            }
            sb.append(", ");
            i++;
         }
      }
      sb.append("}, flags=").append(printFlags())
         .append(", metadata=").append(metadata)
         .append(", isForwarded=").append(isForwarded)
         .append("}");
      return sb.toString();
   }

   @Override
   public boolean isSuccessful() {
      return true;
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
      // Do nothing
   }

   @Override
   public Collection<?> getAffectedKeys() {
      return map.keySet();
   }

   @Override
   public void fail() {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean isReturnValueExpected() {
      return !hasAnyFlag(FlagBitSets.IGNORE_RETURN_VALUES);
   }

   @Override
   public LoadType loadType() {
      return hasAnyFlag(FlagBitSets.IGNORE_RETURN_VALUES) ? LoadType.DONT_LOAD : LoadType.PRIMARY;
   }

   @Override
   public Metadata getMetadata() {
      return metadata;
   }

   @Override
   public void setMetadata(Metadata metadata) {
      this.metadata = metadata;
   }

   /**
    * @see #isForwarded()
    */
   public void setForwarded(boolean forwarded) {
      isForwarded = forwarded;
   }
}
