package org.infinispan.commands.remote;

import org.infinispan.commands.AbstractTopologyAffectedCommand;
import org.infinispan.commands.VisitableCommand;
import org.infinispan.commands.Visitor;
import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.context.InvocationContext;
import org.infinispan.marshall.protostream.impl.MarshallableUserObject;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * {@link org.infinispan.commands.VisitableCommand} that fetches the keys belonging to a group.
 *
 * @author Pedro Ruivo
 * @since 7.0
 */
@ProtoTypeId(ProtoStreamTypeIds.GET_KEYS_IN_GROUP_COMMAND)
public class GetKeysInGroupCommand extends AbstractTopologyAffectedCommand implements VisitableCommand {

   public static final byte COMMAND_ID = 43;

   @ProtoField(number = 3)
   final MarshallableUserObject<?> groupName;
   /*
   local state to avoid checking everywhere if the node in which this command is executed is the group owner.
    */
   private transient boolean isGroupOwner;

   @ProtoFactory
   GetKeysInGroupCommand(long flagsWithoutRemote, int topologyId, MarshallableUserObject<?> groupName) {
      super(flagsWithoutRemote, topologyId);
      this.groupName = groupName;
   }

   public GetKeysInGroupCommand(long flagsBitSet, Object groupName) {
      this(flagsBitSet, -1, MarshallableUserObject.create(groupName));
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
      return visitor.visitGetKeysInGroupCommand(ctx, this);
   }

   @Override
   public LoadType loadType() {
      return LoadType.OWNER;
   }

   public Object getGroupName() {
      return MarshallableUserObject.unwrap(groupName);
   }

   @Override
   public String toString() {
      return "GetKeysInGroupCommand{" +
            "groupName='" + groupName + '\'' +
            ", flags=" + printFlags() +
            '}';
   }

   public boolean isGroupOwner() {
      return isGroupOwner;
   }

   public void setGroupOwner(boolean isGroupOwner) {
      this.isGroupOwner = isGroupOwner;
   }
}
