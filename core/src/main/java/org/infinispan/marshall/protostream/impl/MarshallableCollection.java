package org.infinispan.marshall.protostream.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

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
@ProtoTypeId(ProtoStreamTypeIds.MARSHALLABLE_COLLECTION)
public class MarshallableCollection<T> {

   /**
    * @param entries an Array to be wrapped as a {@link MarshallableCollection}.
    * @return a new {@link MarshallableCollection} instance containing the passed object if the array is not null,
    * otherwise null.
    */
   public static <T> MarshallableCollection<T> create(T[] entries) {
      return new MarshallableCollection<>(Arrays.asList(entries));
   }

   /**
    * @param collection the {@link Collection} to be wrapped.
    * @return a new {@link MarshallableCollection} instance containing the passed object if the object is not null,
    * otherwise null.
    */
   public static <T> MarshallableCollection<T> create(Collection<T> collection) {
      return collection == null ? null : new MarshallableCollection<>(collection);
   }

   /**
    * @param wrapper the {@link MarshallableCollection} instance to unwrap.
    * @return the wrapped {@link Collection} or null if the provided wrapper does not exist.
    */
   public static <T> Collection<T> unwrap(MarshallableCollection<T> wrapper) {
      return wrapper == null ? null : wrapper.get();
   }

   /**
    * @param wrapper the {@link MarshallableCollection} instance to unwrap.
    * @return a {@link List} representation of the wrapped {@link Collection} or null if the provided wrapper does not exist.
    */
   public static <T> List<T> unwrapAsList(MarshallableCollection<T> wrapper) {
//      return unwrap(wrapper, c -> c instanceof List ? (List<T>) c : new ArrayList<>(c));
      if (wrapper == null)
         return null;

      Collection<T> collection = wrapper.get();
      return collection instanceof List ? (List<T>) collection : new ArrayList<>(collection);
   }

   /**
    * @param wrapper the {@link MarshallableCollection} instance to unwrap.
    * @return a {@link Set} representation of the wrapped {@link Collection} or null if the provided wrapper does not exist.
    */
   public static <T> Set<T> unwrapAsSet(MarshallableCollection<T> wrapper) {
      if (wrapper == null)
         return null;

      Collection<T> collection = wrapper.get();
      return collection instanceof Set ? (Set<T>) collection : new HashSet<>(collection);
   }

   // TODO replace unwrapAsList with this?
   public static <R extends Collection<T>, T> R unwrap(MarshallableCollection<T> wrapper, Function<Collection<T>, R> builder) {
      if (wrapper == null)
         return null;

      return builder.apply(wrapper.get());
   }

   private final Collection<T> collection;

   private MarshallableCollection(Collection<T> collection) {
      this.collection = collection;
   }

   @ProtoFactory
   MarshallableCollection(List<byte[]> bytes) {
      throw illegalState();
   }

   @ProtoField(number = 1, collectionImplementation = ArrayList.class)
   List<byte[]> getBytes() {
      throw illegalState();
   }

   public Collection<T> get() {
      return collection;
   }

   private IllegalStateException illegalState() {
      // no-op never actually used, as we override the default marshaller
      return new IllegalStateException(this.getClass().getSimpleName() + " marshaller not overridden in SerializationContext");
   }

   public static class Marshaller implements RawProtobufMarshaller<MarshallableCollection> {
      private final String typeName;
      private final org.infinispan.commons.marshall.Marshaller marshaller;

      public Marshaller(String typeName, org.infinispan.commons.marshall.Marshaller marshaller) {
         this.typeName = typeName;
         this.marshaller = marshaller;
      }

      @Override
      public MarshallableCollection readFrom(ImmutableSerializationContext ctx, RawProtoStreamReader in) throws IOException {
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
            return new MarshallableCollection<>(entries);
         } catch (ClassNotFoundException e) {
            throw new MarshallingException(e);
         }
      }

      @Override
      public void writeTo(ImmutableSerializationContext ctx, RawProtoStreamWriter out,
                          MarshallableCollection marshallableCollection) throws IOException {
         try {
            Collection<?> collection = marshallableCollection.get();
            if (collection != null) {
               for (Object entry : collection) {
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
      public Class<? extends MarshallableCollection> getJavaClass() {
         return MarshallableCollection.class;
      }

      @Override
      public String getTypeName() {
         return typeName;
      }
   }
}
