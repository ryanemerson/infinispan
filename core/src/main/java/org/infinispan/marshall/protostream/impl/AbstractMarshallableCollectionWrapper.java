package org.infinispan.marshall.protostream.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.infinispan.commons.marshall.MarshallingException;
import org.infinispan.commons.util.Util;
import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.infinispan.protostream.RawProtobufMarshaller;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.impl.WireFormat;

/**
 * An abstract class which provides the basis of {@link java.util.Collection} wrapper implementations which need to
 * delegate the marshalling of a {@link java.util.Collection}'s entries to a {@link
 * org.infinispan.commons.marshall.Marshaller} implementation at runtime.
 * <p>
 * This abstraction hides the details of the configured marshaller from our internal Pojos, so that all calls to the
 * marshaller required by the implementation class can be limited to the {@link AbstractMarshallableWrapper}
 * implementation.
 *
 * @author Ryan Emerson
 * @since 11.0
 */
public class AbstractMarshallableCollectionWrapper<T> {

   protected final Collection<T> collection;

   protected AbstractMarshallableCollectionWrapper(Collection<T> collection) {
      this.collection = collection;
   }

   @ProtoFactory
   AbstractMarshallableCollectionWrapper(List<byte[]> bytes) {
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

   @Override
   public String toString() {
      return Util.toStr(collection);
   }

   protected abstract static class Marshaller implements RawProtobufMarshaller<AbstractMarshallableCollectionWrapper<?>> {

      private final String typeName;
      private final org.infinispan.commons.marshall.Marshaller marshaller;

      public Marshaller(String typeName, org.infinispan.commons.marshall.Marshaller marshaller) {
         this.typeName = typeName;
         this.marshaller = marshaller;
      }

      abstract AbstractMarshallableCollectionWrapper<Object> newWrapperInstance(Collection<Object> o);

      @Override
      public String getTypeName() {
         return typeName;
      }

      @Override
      public Class getJavaClass() {
         return MarshallableUserCollection.class;
      }

      @Override
      public AbstractMarshallableCollectionWrapper<?> readFrom(ImmutableSerializationContext ctx,
                                                               RawProtoStreamReader in) throws IOException {
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
            return newWrapperInstance(entries);
         } catch (ClassNotFoundException e) {
            throw new MarshallingException(e);
         }
      }

      @Override
      public void writeTo(ImmutableSerializationContext ctx, RawProtoStreamWriter out,
                          AbstractMarshallableCollectionWrapper<?> wrapper) throws IOException {
         try {
            Collection<?> collection = wrapper.get();
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
   }
}
