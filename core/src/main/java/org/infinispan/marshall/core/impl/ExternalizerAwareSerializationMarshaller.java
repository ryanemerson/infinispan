package org.infinispan.marshall.core.impl;

import java.io.IOException;
import java.io.ObjectInput;

import org.infinispan.commons.CacheException;
import org.infinispan.commons.configuration.ClassWhiteList;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.io.ByteBuffer;
import org.infinispan.commons.marshall.AbstractMarshaller;
import org.infinispan.commons.marshall.AdvancedExternalizer;
import org.infinispan.commons.marshall.Externalizer;
import org.infinispan.commons.marshall.JavaSerializationMarshaller;
import org.infinispan.commons.marshall.SerializeWith;
import org.infinispan.commons.util.Util;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.persistence.spi.InitializationContext;

/**
 * {@link org.infinispan.commons.marshall.Marshaller} implementation that utilises configured {@link
 * AdvancedExternalizer} instances if available, otherwise it delegates to the {@link JavaSerializationMarshaller}. This
 * marshaller is required in core as a replacement for the {@link org.infinispan.jboss.marshalling.core.JBossUserMarshaller},
 * which is no longer available without the `infinispan-jboss-marshalling` artifact.
 * <p>
 * In the context of clustered communication and the {@link org.infinispan.marshall.core.GlobalMarshaller}, it is
 * sufficient for the {@link org.infinispan.marshall.persistence.impl.PersistenceMarshallerImpl} to use the {@link
 * JavaSerializationMarshaller} directly as the {@link org.infinispan.marshall.core.GlobalMarshaller} attempts to
 * utilise user externalizers before delegating to the PersistenceMarshaller. However, as the PersistenceMarshaller is
 * utilised directly by {@link org.infinispan.persistence.spi.CacheLoader} and {@link
 * org.infinispan.persistence.spi.CacheWriter} implementations via {@link InitializationContext#getPersistenceMarshaller()},
 * it's necessary that the default user marshaller also attempts to utilise the configured externalizers.
 *
 * @author Ryan Emerson
 * @since 10.0
 */
public class ExternalizerAwareSerializationMarshaller extends AbstractMarshaller {

   private static final int ID_NULL = 0x00;
   private static final int ID_EXTERNAL = 0x01;
   private static final int ID_ANNOTATED = 0x02;
   private static final int ID_ARRAY = 0x03;
   private static final int ID_SERIALIZATION = 0x04;

   private final ClassLoader classLoader;
   private final ClassToExternalizerMap externalExts;
   private final ClassToExternalizerMap.IdToExternalizerMap reverseExts;
   private final JavaSerializationMarshaller serializationMarshaller;

   public ExternalizerAwareSerializationMarshaller(GlobalConfiguration globalCfg, ClassWhiteList whiteList) {
      this.classLoader = globalCfg.classLoader();
      this.externalExts = ExternalExternalizers.load(globalCfg, AdvancedExternalizer.USER_EXT_ID_MIN, Integer.MAX_VALUE);
      this.reverseExts = externalExts.reverseMap();
      this.serializationMarshaller = new JavaSerializationMarshaller(whiteList);
   }

   @Override
   protected ByteBuffer objectToBuffer(Object o, int estimatedSize) throws IOException {
      try (ExternalizerObjectOutput out = new ExternalizerObjectOutput(estimatedSize)) {
         out.writeObject(o);
         return out.toByteBuffer().copy();
      }
   }

   @Override
   public Object objectFromByteBuffer(byte[] buf, int offset, int length) throws IOException, ClassNotFoundException {
      try (ObjectInput in = new ExternalizerObjectInput(buf, offset)) {
         return in.readObject();
      }
   }

   @Override
   public boolean isMarshallable(Object o) throws Exception {
      return externalExts.get(o.getClass()) != null || serializationMarshaller.isMarshallable(o);
   }

   @Override
   public MediaType mediaType() {
      return null;
   }

   /**
    * {@link java.io.ObjectOutput} implementation that has comparable semantics to the
    * {@link org.infinispan.jboss.marshalling.core.JBossUserMarshaller.UserExternalizerObjectTable}.
    */
   class ExternalizerObjectOutput extends AbstractBytesObjectOutput {

      ExternalizerObjectOutput(int size) {
         super(size);
      }

      @Override
      public void writeObject(Object obj) throws IOException {
         if (obj == null) {
            writeByte(ID_NULL);
            return;
         }

         Class<?> clazz = obj.getClass();
         if (clazz.isArray()) {
            Object[] array = (Object[]) obj;
            writeByte(ID_ARRAY);
            writeInt(array.length);
            for (Object o : array)
               writeObject(o);
            return;
         }

         AdvancedExternalizer ext = externalExts.get(clazz);
         if (ext != null) {
            writeByte(ID_EXTERNAL);
            writeInt(ext.getId());
            ext.writeObject(this, obj);
            return;
         }

         SerializeWith serialAnn = clazz.getAnnotation(SerializeWith.class);
         if (serialAnn != null) {
            try {
               Externalizer annotExt = serialAnn.value().newInstance();
               writeByte(ID_ANNOTATED);
               writeUTF(annotExt.getClass().getName());
               annotExt.writeObject(this, obj);
               return;
            } catch (Exception e) {
               throw new IllegalArgumentException(String.format(
                     "Cannot instantiate externalizer for %s", clazz), e);
            }
         }

         // Attempt to write with Serialization
         // In the context of the GlobalMarshaller->PersistenceMarshaller->ExternalizerAwareSerializationMarshaller
         // This should always be the case
         writeByte(ID_SERIALIZATION);
         try {
            byte[] bytes = serializationMarshaller.objectToByteBuffer(obj);
            writeInt(bytes.length);
            write(bytes);
         } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
         }
      }
   }

   class ExternalizerObjectInput extends AbstractBytesObjectInput {

      ExternalizerObjectInput(byte[] bytes, int offset) {
         super(bytes, offset);
      }

      @Override
      public Object readObject() throws ClassNotFoundException, IOException {
         int type = readUnsignedByte();
         int length;
         Externalizer<?> ext;
         switch (type) {
            case ID_NULL:
               return null;
            case ID_EXTERNAL:
               int id = readInt();
               ext = reverseExts.get(id);
               return ext.readObject(this);
            case ID_ANNOTATED:
               String impl = readUTF();
               try {
                  ext = Util.getInstance(impl, classLoader);
                  return ext.readObject(this);
               } catch (Exception e) {
                  throw new CacheException("Error instantiating class: " + impl, e);
               }
            case ID_ARRAY:
               length = readInt();
               Object[] array = new Object[length];
               for (int i = 0; i < length; i++)
                  array[i] = readObject();
               return array;
            case ID_SERIALIZATION:
               length = readInt();
               byte[] bytes = new byte[length];
               readFully(bytes);
               return serializationMarshaller.objectFromByteBuffer(bytes);
            default:
               throw new IOException("Unknown type: " + type);
         }
      }
   }
}
