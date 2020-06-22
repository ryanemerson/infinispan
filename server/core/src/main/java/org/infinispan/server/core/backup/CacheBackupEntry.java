package org.infinispan.server.core.backup;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.metadata.impl.PrivateMetadata;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * ProtoStream entity used to represent individual cache entries.
 *
 * @author Ryan Emerson
 * @since 11.0
 */
@ProtoTypeId(ProtoStreamTypeIds.CACHE_BACKUP_ENTRY)
public class CacheBackupEntry {

   @ProtoField(number = 1)
   byte[] key;

   @ProtoField(number = 2)
   byte[] value;

   @ProtoField(number = 3)
   byte[] metadata;

   @ProtoField(number = 4)
   PrivateMetadata internalMetadata;

   @ProtoField(number = 5, defaultValue = "-1")
   long created;

   @ProtoField(number = 6, defaultValue = "-1")
   long lastUsed;
}
