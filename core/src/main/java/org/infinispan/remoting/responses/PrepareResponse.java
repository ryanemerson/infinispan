package org.infinispan.remoting.responses;

import java.util.HashMap;
import java.util.Map;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.container.versioning.IncrementableEntryVersion;
import org.infinispan.marshall.protostream.impl.MarshallableArray;
import org.infinispan.marshall.protostream.impl.MarshallableCollection;
import org.infinispan.marshall.protostream.impl.MarshallableMap;
import org.infinispan.marshall.protostream.impl.MarshallableObject;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.transaction.impl.WriteSkewHelper;

/**
 * A {@link ValidResponse} used by Optimistic Transactions.
 * <p>
 * It contains the new {@link IncrementableEntryVersion} for each key updated.
 * <p>
 * To be extended in the future.
 *
 * @author Pedro Ruivo
 * @since 11.0
 */
@ProtoTypeId(ProtoStreamTypeIds.PREPARE_RESPONSE)
public class PrepareResponse extends ValidResponse {

   private Map<Object, IncrementableEntryVersion> newWriteSkewVersions;

   public static PrepareResponse asPrepareResponse(Object rv) {
      assert rv == null || rv instanceof PrepareResponse;
      return rv == null ? new PrepareResponse() : (PrepareResponse) rv;
   }

   public PrepareResponse() {
   }

   @ProtoFactory
   PrepareResponse(MarshallableObject<?> object, MarshallableCollection<?> collection,
                   MarshallableMap<?, ?> map, MarshallableArray<?> array) {
      super(null, null, map, null);
   }

   @Override
   public boolean isSuccessful() {
      return true;
   }

   @Override
   public Object getResponseValue() {
      throw new UnsupportedOperationException();
   }

   @Override
   public String toString() {
      return "PrepareResponse{" +
            "WriteSkewVersions=" + newWriteSkewVersions +
            '}';
   }

   public void merge(PrepareResponse remote) {
      if (remote.newWriteSkewVersions != null) {
         mergeEntryVersions(remote.newWriteSkewVersions);
      }
   }

   public Map<Object, IncrementableEntryVersion> mergeEntryVersions(
         Map<Object, IncrementableEntryVersion> entryVersions) {
      if (newWriteSkewVersions == null) {
         newWriteSkewVersions = new HashMap<>();
      }
      newWriteSkewVersions = WriteSkewHelper.mergeEntryVersions(newWriteSkewVersions, entryVersions);
      this.map = MarshallableMap.create(newWriteSkewVersions);
      return newWriteSkewVersions;
   }
}
