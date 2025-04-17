package org.infinispan.upgrade;

public interface VersionAwareComponent {
   /**
    * Returns a {@link ManagerVersion} representing the Infinispan version in which this functionality was added. This value
    * is used to ensure that when the cluster contains different Infinispan versions, only actions compatible with the
    * oldest version are permitted.
    *
    * @return a {@link ManagerVersion} corresponding to the Infinispan version this functionality was added.
    */
   ManagerVersion supportedSince();
}
