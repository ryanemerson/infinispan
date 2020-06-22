package org.infinispan.server.core.backup;

import org.infinispan.counter.api.CounterConfiguration;
import org.infinispan.protostream.annotations.ProtoField;

/**
 * // TODO: Document this
 *
 * @author Ryan Emerson
 * @since 11.0
 */
public class CounterBackupEntry {

   CounterBackupEntry() {
   }

   @ProtoField(number = 1)
   String name;
   @ProtoField(number = 2)
   CounterConfiguration configuration;
   @ProtoField(number = 3, defaultValue = "-1")
   long value;
}
