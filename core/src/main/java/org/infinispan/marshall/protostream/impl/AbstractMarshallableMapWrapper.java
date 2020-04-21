package org.infinispan.marshall.protostream.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.infinispan.protostream.RawProtobufMarshaller;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.impl.GeneratedMarshallerBase;
import org.infinispan.protostream.impl.BaseMarshallerDelegate;
import org.infinispan.protostream.impl.SerializationContextImpl;
import org.infinispan.protostream.impl.WireFormat;
import org.infinispan.util.KeyValuePair;

/**
 * An abstract class which provides the basis of {@link java.util.Map} wrapper implementations which need to
 * delegate the marshalling of a {@link java.util.Map}'s entries to a {@link
 * org.infinispan.commons.marshall.Marshaller} implementation at runtime.
 * <p>
 * This abstraction hides the details of the configured marshaller from our internal Pojos, so that all calls to the
 * marshaller required by the implementation class can be limited to the {@link AbstractMarshallableWrapper}
 * implementation.
 *
 * @author Ryan Emerson
 * @since 11.0
 */
// TODO merge with MarshallableMap
abstract class AbstractMarshallableMapWrapper<K, V> {

   final Map<K, V> map;

   @ProtoFactory
   AbstractMarshallableMapWrapper(List<KeyValuePair<K, V>> entries) {
      // no-op never actually used, as we override the default marshaller
      throw illegalState();
   }

   protected AbstractMarshallableMapWrapper(Map<K, V> map) {
      this.map = map;
   }

   @ProtoField(number = 1)
   List<KeyValuePair<K, V>> getEntries() {
      throw illegalState();
   }

   public Map<K, V> get() {
      return map;
   }

   private IllegalStateException illegalState() {
      return new IllegalStateException(this.getClass().getSimpleName() + " marshaller not overridden in SerializationContext");
   }

   @Override
   public String toString() {
      return map == null ? null : map.toString();
   }

   public abstract static class Marshaller extends GeneratedMarshallerBase implements RawProtobufMarshaller<AbstractMarshallableMapWrapper<?, ?>> {
      private BaseMarshallerDelegate<KeyValuePair> kvpMarshaller;
      private final String typeName;

      public Marshaller(String typeName) {
         this.typeName = typeName;
      }

      abstract AbstractMarshallableMapWrapper<Object, Object> newWrapperInstance(Map<Object, Object> map);

      @Override
      public String getTypeName() {
         return typeName;
      }

      @Override
      public AbstractMarshallableMapWrapper<?, ?> readFrom(ImmutableSerializationContext ctx, RawProtoStreamReader in) throws IOException {
         if (kvpMarshaller == null)
            kvpMarshaller = ((SerializationContextImpl) ctx).getMarshallerDelegate(org.infinispan.util.KeyValuePair.class);

         Map<Object, Object> map = new HashMap<>();
         boolean done = false;
         while (!done) {
            final int tag = in.readTag();
            switch (tag) {
               case 0:
                  done = true;
                  break;
               case 1 << 3 | WireFormat.WIRETYPE_LENGTH_DELIMITED: {
                  int length = in.readRawVarint32();
                  int oldLimit = in.pushLimit(length);
                  KeyValuePair<?,?> kvp = readMessage(kvpMarshaller, in);
                  in.checkLastTagWas(0);
                  in.popLimit(oldLimit);
                  map.put(kvp.getKey(), kvp.getValue());
                  break;
               }
               default: {
                  if (!in.skipField(tag)) done = true;
               }
            }
         }
         return newWrapperInstance(map);
      }

      @Override
      public void writeTo(ImmutableSerializationContext ctx, RawProtoStreamWriter out, AbstractMarshallableMapWrapper<?, ?> wrapper) throws IOException {
         if (kvpMarshaller == null)
            kvpMarshaller = ((SerializationContextImpl) ctx).getMarshallerDelegate(org.infinispan.util.KeyValuePair.class);

         Map<?, ?> map = wrapper.get();
         if (map != null) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
               // TODO is this the best way to represent entries?
               // It means that key and value are always represented as MarshallableObject
               // Just use custom Entry class that stores bytes for both?
               // Saving of 2 bytes per entry
               // MarshallableMap passes GlobalMarshaller
               // MarshallableUserMap then passes just the user marshaller
               KeyValuePair<?, ?> kvp = new KeyValuePair<>(entry.getKey(), entry.getValue());
               writeNestedMessage(kvpMarshaller, out, 1, kvp);
            }
         }
      }
   }
}
