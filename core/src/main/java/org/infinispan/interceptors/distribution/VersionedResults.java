package org.infinispan.interceptors.distribution;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.container.versioning.EntryVersion;
import org.infinispan.marshall.protostream.impl.MarshallableCollection;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

@ProtoTypeId(ProtoStreamTypeIds.VERSIONED_RESULTS)
public class VersionedResults {
   public final Object[] values;
   public final EntryVersion[] versions;

   public VersionedResults(Object[] values, EntryVersion[] versions) {
      this.values = values;
      this.versions = versions;
   }

   @ProtoFactory
   VersionedResults(MarshallableCollection<Object> values, MarshallableCollection<EntryVersion> versions) {
      this.values = MarshallableCollection.unwrapAsArray(values, Object[]::new);
      this.versions = MarshallableCollection.unwrapAsArray(versions, EntryVersion[]::new);
   }

   @ProtoField(number = 1)
   MarshallableCollection<Object> getValues() {
      return MarshallableCollection.create(values);
   }

   // We have to marshall as MarshallableCollection, instead of two fields for SimpleClusteredVersion and NumericVersion
   // as index of version corresponds to values collection.
   @ProtoField(number = 2)
   MarshallableCollection<EntryVersion> getVersions() {
      return MarshallableCollection.create(versions);
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("VersionedResults{");
      for (int i = 0; i < values.length; ++i) {
         sb.append(values[i]).append(" (").append(versions[i]).append(')');
         if (i != values.length - 1) sb.append(", ");
      }
      sb.append('}');
      return sb.toString();
   }
}
