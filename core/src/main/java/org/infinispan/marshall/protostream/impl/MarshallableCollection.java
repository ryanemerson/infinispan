package org.infinispan.marshall.protostream.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * A wrapper for collections of objects whose type is unknown until runtime. This is equivalent to utilising a
 * <code>Collection<MarshallableObject></code> without the overhead of creating a {@link MarshallableObject} per
 * entry.
 *
 * @author Ryan Emerson
 * @since 11.0
 */
// TODO implement Collection directly?
@ProtoTypeId(ProtoStreamTypeIds.MARSHALLABLE_COLLECTION)
public class MarshallableCollection<T> extends AbstractMarshallableCollectionWrapper<T> {

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
    * @return an array representation of the wrapped {@link Collection} or null if the provided wrapper does not exist.
    */
   @SuppressWarnings("unchecked")
   public static <T> T[] unwrapAsArray(MarshallableCollection<T> wrapper, Function<Integer, T[]> builder) {
      return wrapper == null ? null : wrapper.get().toArray(builder.apply(wrapper.get().size()));
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

   @ProtoFactory
   MarshallableCollection(List<byte[]> bytes) {
      super(bytes);
   }

   private MarshallableCollection(Collection<T> collection) {
      super(collection);
   }

   public static class Marshaller extends AbstractMarshallableCollectionWrapper.Marshaller {

      public Marshaller(String typeName, org.infinispan.commons.marshall.Marshaller marshaller) {
         super(typeName, marshaller);
      }

      public Class getJavaClass() {
         return MarshallableCollection.class;
      }

      @Override
      AbstractMarshallableCollectionWrapper<Object> newWrapperInstance(Collection<Object> o) {
         return new MarshallableCollection<>(o);
      }
   }
}
