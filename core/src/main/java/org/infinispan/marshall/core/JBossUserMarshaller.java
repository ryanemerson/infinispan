package org.infinispan.marshall.core;

import java.io.IOException;
import java.io.ObjectInput;

import org.infinispan.commons.CacheException;
import org.infinispan.commons.marshall.AdvancedExternalizer;
import org.infinispan.commons.marshall.Externalizer;
import org.infinispan.commons.marshall.SerializeFunctionWith;
import org.infinispan.commons.marshall.SerializeWith;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;
import org.jboss.marshalling.ObjectTable;
import org.jboss.marshalling.Unmarshaller;

/**
 * An extension of the {@link JBossMarshaller} that creates a {@link ObjectTable} using any user configured {@link
 * Externalizer}s.
 *
 * @author Ryan Emerson
 * @since 10.0
 */
public class JBossUserMarshaller extends JBossMarshaller {

   private static final Log log = LogFactory.getLog(JBossUserMarshaller.class);

   static final int NOT_FOUND = -1;

   static final int ID_NULL = 0x00;
   static final int ID_PRIMITIVE = 0x01;
   static final int ID_INTERNAL = 0x02;
   static final int ID_EXTERNAL = 0x03;
   static final int ID_ANNOTATED = 0x04;

   private final ClassToExternalizerMap externalExts;

   public JBossUserMarshaller(GlobalComponentRegistry gcr) {
      this.globalCfg = gcr.getGlobalConfiguration();
      this.externalExts = ExternalExternalizers.load(globalCfg);
      this.objectTable = new UserExternalizerObjectTable();
   }

   public <T> Externalizer<T> findExternalizerFor(Object obj) {
      Class<?> clazz = obj.getClass();
      Externalizer ext = getExternalizer(externalExts, clazz);
      return ext != null ? ext : findAnnotatedExternalizer(clazz);
   }

   private <T> Externalizer<T> findAnnotatedExternalizer(Class<?> clazz) {
      try {
         SerializeWith serialAnn = clazz.getAnnotation(SerializeWith.class);
         if (serialAnn != null) {
            return (Externalizer<T>) serialAnn.value().newInstance();
         } else {
            SerializeFunctionWith funcSerialAnn = clazz.getAnnotation(SerializeFunctionWith.class);
            if (funcSerialAnn != null)
               return (Externalizer<T>) funcSerialAnn.value().newInstance();
         }

         return null;
      } catch (Exception e) {
         throw new IllegalArgumentException(String.format(
               "Cannot instantiate externalizer for %s", clazz), e);
      }
   }

   private AdvancedExternalizer getExternalizer(ClassToExternalizerMap class2ExternalizerMap, Class<?> clazz) {
      if (class2ExternalizerMap == null) {
         throw log.cacheManagerIsStopping();
      }
      return class2ExternalizerMap.get(clazz);
   }

   private Object readAnnotated(ObjectInput in) throws IOException, ClassNotFoundException {
      Class<? extends Externalizer> clazz = (Class<? extends Externalizer>) in.readObject();
      try {
         Externalizer ext = clazz.newInstance();
         return ext.readObject(in);
      } catch (Exception e) {
         throw new CacheException("Error instantiating class: " + clazz, e);
      }
   }

   /**
    * A {@link org.jboss.marshalling.ObjectTable} implementation that creates {@link
    * org.jboss.marshalling.ObjectTable.Writer} based upon a users configured {@link
    * org.infinispan.commons.marshall.Externalizer} implementations.
    */
   class UserExternalizerObjectTable implements ObjectTable {

      final ClassToExternalizerMap.IdToExternalizerMap reverseExts = externalExts.reverseMap();

      @Override
      public ObjectTable.Writer getObjectWriter(Object object) {
         Class clazz = object.getClass();
         AdvancedExternalizer ext = externalExts.get(clazz);
         if (ext != null)
            return (out, obj) -> {
               out.writeByte(ID_EXTERNAL);
               out.writeInt(ext.getId());
               ext.writeObject(out, obj);
            };

         Externalizer annotExt = findAnnotatedExternalizer(clazz);
         return annotExt == null ? null : (out, obj) -> {
            out.writeByte(ID_ANNOTATED);
            out.writeObject(ext.getClass());
            ext.writeObject(out, obj);
         };
      }

      @Override
      public Object readObject(Unmarshaller unmarshaller) throws IOException, ClassNotFoundException {
         int type = unmarshaller.readUnsignedByte();

         switch (type) {
            case ID_EXTERNAL:
               int externalizerId = unmarshaller.readInt();
               return reverseExts.get(externalizerId).readObject(unmarshaller);
            case ID_ANNOTATED:
               return readAnnotated(unmarshaller);
            default:
               throw new IllegalStateException(String.format("Unexpected type %s", type));
         }
      }
   }
}
