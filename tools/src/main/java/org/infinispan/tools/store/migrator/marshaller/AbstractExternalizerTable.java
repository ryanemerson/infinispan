package org.infinispan.tools.store.migrator.marshaller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.infinispan.commons.CacheConfigurationException;
import org.infinispan.commons.io.UnsignedNumeric;
import org.infinispan.commons.marshall.AdvancedExternalizer;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;
import org.jboss.marshalling.ObjectTable;
import org.jboss.marshalling.Unmarshaller;

/**
 * @author Ryan Emerson
 * @since 10.0
 */
public abstract class AbstractExternalizerTable implements ObjectTable {

   private static final Log log = LogFactory.getLog(AbstractExternalizerTable.class);
   private static final int MAX_ID = 255;

   private final Map<Integer, ExternalizerAdapter> readers = new HashMap<>();

   protected AbstractExternalizerTable(Map<Integer, ? extends AdvancedExternalizer<?>> userExternalizerMap) {
      initForeignMarshallables(userExternalizerMap);
   }

   public abstract void initInternalExternalizers(Marshaller marshaller);

   @Override
   public Writer getObjectWriter(Object o) {
      return null;
   }

   @Override
   public Object readObject(Unmarshaller input) throws IOException, ClassNotFoundException {
      int readerIndex = input.readUnsignedByte();

      int foreignId = -1;
      if (readerIndex == MAX_ID) {
         // User defined externalizer
         foreignId = UnsignedNumeric.readUnsignedInt(input);
         readerIndex = generateForeignReaderIndex(foreignId);
      } else {
         readerIndex = transformId(readerIndex);
      }

      ExternalizerAdapter adapter = readers.get(readerIndex);
      if (adapter == null) {
         if (foreignId > 0)
            throw log.missingForeignExternalizer(foreignId);

         throw log.unknownExternalizerReaderIndex(readerIndex);
      }
      return adapter.externalizer.readObject(input);
   }

   protected int transformId(int id) {
      return id;
   }

   protected void addInternalExternalizer(AdvancedExternalizer<?> ext) {
      int id = checkInternalIdLimit(ext.getId(), ext);
      updateExtReadersWithTypes(new ExternalizerAdapter(id, ext));
   }

   private void updateExtReadersWithTypes(ExternalizerAdapter adapter) {
      updateExtReadersWithTypes(adapter, adapter.id);
   }

   private void updateExtReadersWithTypes(ExternalizerAdapter adapter, int readerIndex) {
      Set<Class<?>> typeClasses = adapter.externalizer.getTypeClasses();
      if (typeClasses.size() > 0) {
         for (Class<?> typeClass : typeClasses)
            updateExtReaders(adapter, typeClass, readerIndex);
      } else {
         throw log.advanceExternalizerTypeClassesUndefined(adapter.externalizer.getClass().getName());
      }
   }

   private void initForeignMarshallables(Map<Integer, ? extends AdvancedExternalizer<?>> externalizerMap) {
      for (Map.Entry<Integer, ? extends AdvancedExternalizer<?>> entry : externalizerMap.entrySet()) {
         AdvancedExternalizer<?> ext = entry.getValue();
         Integer id = ext.getId();
         if (entry.getKey() == null && id == null)
            throw new CacheConfigurationException(String.format(
                  "No advanced externalizer identifier set for externalizer %s",
                  ext.getClass().getName()));
         else if (entry.getKey() != null)
            id = entry.getKey();

         id = checkForeignIdLimit(id, ext);
         updateExtReadersWithTypes(new ExternalizerAdapter(id, ext), generateForeignReaderIndex(id));
      }
   }

   private void updateExtReaders(ExternalizerAdapter adapter, Class<?> typeClass, int readerIndex) {
      ExternalizerAdapter prevReader = readers.put(readerIndex, adapter);
      // Several externalizers might share same id (i.e. HashMap and TreeMap use MapExternalizer)
      // but a duplicate is only considered when that particular index has already been entered
      // in the readers map and the externalizers are different (they're from different classes)
      if (prevReader != null && !prevReader.equals(adapter))
         throw log.duplicateExternalizerIdFound(adapter.id, typeClass, prevReader.externalizer.getClass().getName(), readerIndex);
   }

   private int checkInternalIdLimit(int id, AdvancedExternalizer<?> ext) {
      if (id >= MAX_ID)
         throw log.internalExternalizerIdLimitExceeded(ext, id, MAX_ID);
      return id;
   }

   private int checkForeignIdLimit(int id, AdvancedExternalizer<?> ext) {
      if (id < 0)
         throw log.foreignExternalizerUsingNegativeId(ext, id);

      return id;
   }

   private int generateForeignReaderIndex(int foreignId) {
      return 0x80000000 | foreignId;
   }

   private static class ExternalizerAdapter {
      final int id;
      final AdvancedExternalizer<Object> externalizer;

      ExternalizerAdapter(int id, AdvancedExternalizer<?> externalizer) {
         this.id = id;
         this.externalizer = (AdvancedExternalizer<Object>) externalizer;
      }

      @Override
      public String toString() {
         // Each adapter is represented by the externalizer it delegates to, so just return the class name
         return externalizer.getClass().getName();
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) return true;
         if (o == null || getClass() != o.getClass()) return false;
         ExternalizerAdapter that = (ExternalizerAdapter) o;
         if (id != that.id) return false;
         if (externalizer != null ? !externalizer.getClass().equals(that.externalizer.getClass()) : that.externalizer != null)
            return false;
         return true;
      }

      @Override
      public int hashCode() {
         int result = id;
         result = 31 * result + (externalizer.getClass() != null ? externalizer.getClass().hashCode() : 0);
         return result;
      }
   }
}
