package org.infinispan.interceptors.distribution;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.container.versioning.EntryVersion;
import org.infinispan.container.versioning.NumericVersion;
import org.infinispan.container.versioning.SimpleClusteredVersion;
import org.infinispan.marshall.protostream.impl.MarshallableUserObject;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

@ProtoTypeId(ProtoStreamTypeIds.VERSIONED_RESULT)
public class VersionedResult {
   public final Object result;
   public final EntryVersion version;

   public VersionedResult(Object result, EntryVersion version) {
      this.result = result;
      this.version = version;
   }

   @ProtoFactory
   VersionedResult(MarshallableUserObject<?> result, NumericVersion numericVersion, SimpleClusteredVersion clusteredVersion) {
      this.result = MarshallableUserObject.unwrap(result);
      this.version = numericVersion != null ? numericVersion : clusteredVersion;
   }

   @ProtoField(number = 1)
   MarshallableUserObject<?> getResult() {
      return MarshallableUserObject.create(result);
   }

   @ProtoField(number = 2)
   NumericVersion getNumericVersion() {
      return version instanceof NumericVersion ? (NumericVersion) version : null;
   }

   @ProtoField(number = 3)
   SimpleClusteredVersion getClusteredVersion() {
      return version instanceof SimpleClusteredVersion ? (SimpleClusteredVersion) version : null;
   }

   @Override
   public String toString() {
      return "VersionedResult{" + result + " (" + version + ")}";
   }
}
