package org.infinispan.marshall.protostream.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * A wrapper for collections of objects whose type is unknown until runtime. This is equivalent to utilising a
 * <code>Collection<MarshallableUserObject></code> without the overhead of creating a {@link MarshallableUserObject} per
 * entry.
 *
 * @author Ryan Emerson
 * @since 11.0
 */
// TODO remove
@ProtoTypeId(ProtoStreamTypeIds.MARSHALLABLE_USER_COLLECTION)
public class MarshallableUserCollection<T> extends AbstractMarshallableCollectionWrapper<T> {

   /**
    * @param entries an Array to be wrapped as a {@link MarshallableUserCollection}.
    * @return a new {@link MarshallableUserCollection} instance containing the passed object if the array is not null,
    * otherwise null.
    */
   public static <T> MarshallableUserCollection<T> create(T... entries) {
      return new MarshallableUserCollection<>(Arrays.stream(entries).collect(Collectors.toList()));
   }

   /**
    * @param collection the {@link Collection} to be wrapped.
    * @return a new {@link MarshallableUserCollection} instance containing the passed object if the object is not null,
    * otherwise null.
    */
   public static <T> MarshallableUserCollection<T> create(Collection<T> collection) {
      return collection == null ? null : new MarshallableUserCollection<>(collection);
   }

   /**
    * @param wrapper the {@link MarshallableUserCollection} instance to unwrap.
    * @return the wrapped {@link Collection} or null if the provided wrapper does not exist.
    */
   // TODO change to get(..)
   public static <T> Collection<T> unwrap(MarshallableUserCollection<T> wrapper) {
      return wrapper == null ? null : wrapper.get();
   }

   /**
    * @param wrapper the {@link MarshallableUserCollection} instance to unwrap.
    * @return an array representation of the wrapped {@link Collection} or null if the provided wrapper does not exist.
    */
   // TODO change all to asArray, asSet etc
   @SuppressWarnings("unchecked")
   public static <T> T[] unwrapAsArray(MarshallableUserCollection<T> wrapper, Function<Integer, T[]> builder) {
      return wrapper == null ? null : wrapper.get().toArray(builder.apply(wrapper.get().size()));
   }

   /**
    * @param wrapper the {@link MarshallableUserCollection} instance to unwrap.
    * @return a {@link Set} representation of the wrapped {@link Collection} or null if the provided wrapper does not exist.
    */
   public static <T> Set<T> unwrapAsSet(MarshallableUserCollection<T> wrapper) {
      if (wrapper == null)
         return null;

      Collection<T> collection = wrapper.get();
      return collection instanceof Set ? (Set<T>) collection : new HashSet<>(collection);
   }

   @ProtoFactory
   MarshallableUserCollection(List<byte[]> bytes) {
      super(bytes);
   }

   private MarshallableUserCollection(Collection<T> collection) {
      super(collection);
   }

   public static class Marshaller extends AbstractMarshallableCollectionWrapper.Marshaller {

      public Marshaller(String typeName, org.infinispan.commons.marshall.Marshaller marshaller) {
         super(typeName, marshaller);
      }

      public Class getJavaClass() {
         return MarshallableUserCollection.class;
      }

      @Override
      AbstractMarshallableCollectionWrapper<Object> newWrapperInstance(Collection<Object> o) {
         return new MarshallableUserCollection<>(o);
      }
   }
}
