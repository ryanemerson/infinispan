package org.infinispan.marshall.persistence.impl;

import java.lang.invoke.SerializedLambda;

import org.infinispan.commons.configuration.ClassWhiteList;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.xsite.BackupSender;
import org.infinispan.xsite.XSiteAdminCommand;

/**
 * A white list for Infinispan internal java classes which do not have a {@link org.infinispan.commons.marshall.Externalizer}
 * instance defined.
 *
 * @author Ryan Emerson
 * @since 10.0
 */
class UserMarshallerWhiteList {
   static void addInternalClassesToWhiteList(ClassWhiteList list) {
      // Infinispan Enums with no Externalizer
      list.addClasses(
            BackupSender.BringSiteOnlineResponse.class,
            BackupSender.TakeSiteOfflineResponse.class,
            CacheMode.class,
            XSiteAdminCommand.AdminOperation.class,
            XSiteAdminCommand.Status.class
      );
      list.addClasses(Enum.class, Number.class, Object.class, Object[].class, SerializedLambda.class);
   }
}
