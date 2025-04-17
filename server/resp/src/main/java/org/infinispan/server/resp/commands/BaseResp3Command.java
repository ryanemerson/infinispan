package org.infinispan.server.resp.commands;

import org.infinispan.upgrade.ManagerVersion;
import org.infinispan.upgrade.VersionAwareComponent;

// TODO implement check for Resp commands
public interface BaseResp3Command extends VersionAwareComponent {
   long aclMask();

   @Override
   default ManagerVersion supportedSince() {
      return ManagerVersion.SIXTEEN;
   }
}
