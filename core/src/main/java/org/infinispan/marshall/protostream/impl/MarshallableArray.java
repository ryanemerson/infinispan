package org.infinispan.marshall.protostream.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.infinispan.commons.marshall.MarshallingException;
import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.commons.util.Util;
import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.infinispan.protostream.RawProtobufMarshaller;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.protostream.impl.WireFormat;

/**
 * A wrapper for collections of objects whose type is unknown until runtime. This is equivalent to utilising a
 * <code>Collection<MarshallableObject></code> without the overhead of creating a {@link MarshallableObject} per
 * entry.
 *
 * @author Ryan Emerson
 * @since 11.0
 */
@ProtoTypeId(ProtoStreamTypeIds.MARSHALLABLE_ARRAY)
public class MarshallableArray<T> {

   /**
    * @param entries an Array to be wrapped as a {@link MarshallableArray}.
    * @return a new {@link MarshallableArray} instance containing the passed object if the array is not null, otherwise
    * null.
    */
   public static <T> MarshallableArray<T> create(T[] entries) {
      return entries == null ? null : new MarshallableArray<>(entries);
   }

   /**
    * @param wrapper the {@link MarshallableArray} instance to unwrap.
    * @return the wrapped {@link Collection} or null if the provided wrapper does not exist.
    */
   public static <T> T[] unwrap(MarshallableArray<T> wrapper, T[] array) {
      if (wrapper == null)
         return null;

      if (wrapper.collection == null)
         throw new IllegalStateException(MarshallableArray.class.getSimpleName() + " has not been marshalled yet");

      wrapper.array = wrapper.collection.toArray(array);
      return wrapper.array;
   }

   private volatile T[] array;
   private volatile Collection<T> collection;

   private MarshallableArray(T[] array) {
      this.array = array;
   }

   private MarshallableArray(Collection<T> collection) {
      this.collection = collection;
   }

   @ProtoFactory
   MarshallableArray(List<byte[]> bytes) {
      throw illegalState();
   }

   @ProtoField(number = 1, collectionImplementation = ArrayList.class)
   List<byte[]> getBytes() {
      throw illegalState();
   }

   public T[] get() {
      return array;
   }

   private IllegalStateException illegalState() {
      // no-op never actually used, as we override the default marshaller
      return new IllegalStateException(this.getClass().getSimpleName() + " marshaller not overridden in SerializationContext");
   }

   public static class Marshaller implements RawProtobufMarshaller<MarshallableArray> {
      private final String typeName;
      private final org.infinispan.commons.marshall.Marshaller marshaller;

      public Marshaller(String typeName, org.infinispan.commons.marshall.Marshaller marshaller) {
         this.typeName = typeName;
         this.marshaller = marshaller;
      }

      @Override
      public MarshallableArray readFrom(ImmutableSerializationContext ctx, RawProtoStreamReader in) throws IOException {
         try {
            ArrayList<Object> entries = new ArrayList<>();
            boolean done = false;
            while (!done) {
               final int tag = in.readTag();
               switch (tag) {
                  case 0:
                     done = true;
                     break;
                  case 1 << 3 | WireFormat.WIRETYPE_LENGTH_DELIMITED: {
                     byte[] bytes = in.readByteArray();
                     Object entry = bytes.length == 0 ? null : marshaller.objectFromByteBuffer(bytes);
                     entries.add(entry);
                     break;
                  }
                  default: {
                     if (!in.skipField(tag)) done = true;
                  }
               }
            }
            // Initially store entries as collection, so that it does not have to be converted to an array twice
            return new MarshallableArray<>(entries);
         } catch (ClassNotFoundException e) {
            throw new MarshallingException(e);
         }
      }

      @Override
      public void writeTo(ImmutableSerializationContext ctx, RawProtoStreamWriter out,
                          MarshallableArray marshallableArray) throws IOException {
         try {
            Object[] array = marshallableArray.get();
            if (array != null) {
               for (Object entry : array) {
                  // If entry is null, write an empty byte array so that the null value can be recreated on the receiver.
                  byte[] bytes = entry == null ? Util.EMPTY_BYTE_ARRAY : marshaller.objectToByteBuffer(entry);
                  out.writeBytes(1, bytes);
               }
            }
         } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MarshallingException(e);
         }
      }

      @Override
      public Class<? extends MarshallableArray> getJavaClass() {
         return MarshallableArray.class;
      }

      @Override
      public String getTypeName() {
         return typeName;
      }
   }
}
