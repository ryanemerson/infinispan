package org.infinispan.marshall.core;

import static org.infinispan.marshall.core.GlobalMarshaller.ID_EXTERNAL;
import static org.infinispan.marshall.core.GlobalMarshaller.writeExternalClean;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.function.BiConsumer;

import org.infinispan.commons.marshall.AdvancedExternalizer;
import org.jboss.marshalling.ObjectTable;
import org.jboss.marshalling.Unmarshaller;

/**
 * A {@link org.jboss.marshalling.ObjectTable} implementation that creates {@link org.jboss.marshalling.ObjectTable.Writer}
 * based upon a users configured {@link org.infinispan.marshall.core.MarshalledEntryImpl.Externalizer} implementation.
 */
class UserExternalizerObjectTable implements ObjectTable {
   final ClassToExternalizerMap exts;
   final ClassToExternalizerMap.IdToExternalizerMap reverseExts;

   UserExternalizerObjectTable(ClassToExternalizerMap exts) {
      this.exts = exts;
      this.reverseExts = exts.reverseMap();
   }

   @Override
   public ObjectTable.Writer getObjectWriter(Object object) {
      BiConsumer<ObjectOutput, Object> writer = findWriter(object);
      return writer != null ? writer::accept : null;
   }

   @Override
   public Object readObject(Unmarshaller unmarshaller) throws IOException, ClassNotFoundException {
      AdvancedExternalizer<Object> ext = findExternalizerIn(unmarshaller);
      return ext.readObject(unmarshaller);
   }

   BiConsumer<ObjectOutput, Object> findWriter(Object object) {
      if (exts != null) {
         Class clazz = object.getClass();
         AdvancedExternalizer ext = exts.get(clazz);
         if (ext != null)
            return (out, obj) -> writeExternalClean(obj, ext, out);
      }
      return null;
   }

   AdvancedExternalizer findExternalizerIn(ObjectInput in) throws IOException {
      int type = in.readUnsignedByte();
      if (type != ID_EXTERNAL)
         throw new IllegalStateException(String.format("Expected type %s but received %s", ID_EXTERNAL, type));

      return reverseExts.get(in.readInt());
   }
}
