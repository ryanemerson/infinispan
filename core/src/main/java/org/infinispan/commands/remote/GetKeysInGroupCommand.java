package org.infinispan.commands.remote;

import org.infinispan.commands.AbstractTopologyAffectedCommand;
import org.infinispan.commands.VisitableCommand;
import org.infinispan.commands.Visitor;
import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.context.InvocationContext;
import org.infinispan.marshall.protostream.impl.MarshallableObject;
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

   private Object groupName;
   /*
   local state to avoid checking everywhere if the node in which this command is executed is the group owner.
    */
   private transient boolean isGroupOwner;

   public GetKeysInGroupCommand(long flagsBitSet, Object groupName) {
      super(flagsBitSet, -1);
      this.groupName = groupName;
   }

   @ProtoFactory
   GetKeysInGroupCommand(long flagsWithoutRemote, int topologyId, MarshallableObject<?> wrappedGroupName) {
      super(flagsWithoutRemote, topologyId);
      this.groupName = MarshallableObject.unwrap(wrappedGroupName);
   }

   @ProtoField(number = 3, name = "groupName")
   MarshallableObject<?> getWrappedGroupName() {
      return MarshallableObject.create(groupName);
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
      return groupName;
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
