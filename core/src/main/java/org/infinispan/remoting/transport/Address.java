package org.infinispan.remoting.transport;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A destination for an Infinispan command or operation.
 *
 * @author Manik Surtani
 * @since 4.0
 */
public interface Address extends org.jgroups.Address {

   default org.jgroups.Address getJGroupsAddress() {
      return this;
   }

   @Override
   default int serializedSize() {
      jgroupsNotSupported();
      return 0;
   }

   @Override
   default void readFrom(DataInput in) throws IOException, ClassNotFoundException {
      jgroupsNotSupported();
   }

   @Override
   default void writeTo(DataOutput out) throws IOException {
      jgroupsNotSupported();
   }

   private void jgroupsNotSupported() {
      throw new UnsupportedOperationException("Address implementation is not supported with JGroups");
   }
}
