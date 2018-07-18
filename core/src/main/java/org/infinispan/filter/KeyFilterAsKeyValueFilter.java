package org.infinispan.filter;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import org.infinispan.commons.marshall.AbstractExternalizer;
import org.infinispan.commons.marshall.UserObjectInput;
import org.infinispan.commons.marshall.UserObjectOutput;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.marshall.core.Ids;
import org.infinispan.metadata.Metadata;

/**
 * KeyValueFilter that implements it's filtering solely on the use of the provided KeyFilter
 *
 * @author wburns
 * @since 7.0
 */
public class KeyFilterAsKeyValueFilter<K, V> implements KeyValueFilter<K, V> {
   private final KeyFilter<? super K> filter;

   public KeyFilterAsKeyValueFilter(KeyFilter<? super K> filter) {
      if (filter == null) {
         throw new NullPointerException();
      }
      this.filter = filter;
   }

   @Override
   public boolean accept(K key, V value, Metadata metadata) {
      return filter.accept(key);
   }

   @Inject
   protected void injectDependencies(ComponentRegistry cr) {
      cr.wireDependencies(filter);
   }

   public static class Externalizer extends AbstractExternalizer<KeyFilterAsKeyValueFilter> {
      @Override
      public Set<Class<? extends KeyFilterAsKeyValueFilter>> getTypeClasses() {
         return Collections.singleton(KeyFilterAsKeyValueFilter.class);
      }

      @Override
      public void writeObject(UserObjectOutput output, KeyFilterAsKeyValueFilter object) throws IOException {
         output.writeObject(object.filter);
      }

      @Override
      public KeyFilterAsKeyValueFilter readObject(UserObjectInput input) throws IOException, ClassNotFoundException {
         return new KeyFilterAsKeyValueFilter((KeyFilter)input.readObject());
      }

      @Override
      public Integer getId() {
         return Ids.KEY_FILTER_AS_KEY_VALUE_FILTER;
      }
   }
}
