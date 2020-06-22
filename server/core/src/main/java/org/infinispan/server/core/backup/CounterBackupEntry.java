package org.infinispan.server.core.backup;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.counter.api.CounterConfiguration;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * ProtoStream entity used to represent counter instances.
 *
 * @author Ryan Emerson
 * @since 11.0
 */
@ProtoTypeId(ProtoStreamTypeIds.COUNTER_BACKUP_ENTRY)
public class CounterBackupEntry {

   @ProtoField(number = 1)
   String name;

   @ProtoField(number = 2)
   CounterConfiguration configuration;

   @ProtoField(number = 3, defaultValue = "-1")
   long value;
}
