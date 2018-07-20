package org.infinispan.persistence.spi;

import org.infinispan.commons.io.ByteBuffer;
import org.infinispan.commons.io.ByteBufferImpl;
import org.infinispan.protostream.annotations.ProtoField;

/**
 * A marshallable object containing serialized representations of cache values and metadata, that can be used to store
 * values, metadata and timestamps as a single entity.
 *
 * @author Ryan Emerson
 * @since 10.0
 */
public interface MarshalledValue {

   @ProtoField(number = 1, name = "value", javaType = ByteBufferImpl.class)
   ByteBuffer getValueBytes();

   @ProtoField(number = 2, name = "metadata", javaType = ByteBufferImpl.class)
   ByteBuffer getMetadataBytes();

   @ProtoField(number = 3, name = "created", defaultValue = "-1")
   long getCreated();

   @ProtoField(number = 4, name = "lastUsed", defaultValue = "-1")
   long getLastUsed();
}
