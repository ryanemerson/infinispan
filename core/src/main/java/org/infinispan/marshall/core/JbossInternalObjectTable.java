package org.infinispan.marshall.core;

import static org.infinispan.marshall.core.GlobalMarshaller.ID_EXTERNAL;
import static org.infinispan.marshall.core.GlobalMarshaller.ID_INTERNAL;
import static org.infinispan.marshall.core.GlobalMarshaller.ID_UNKNOWN;

import java.io.IOException;

import org.infinispan.commons.marshall.AdvancedExternalizer;
import org.infinispan.marshall.persistence.PersistenceMarshaller;
import org.jboss.marshalling.ObjectTable;
import org.jboss.marshalling.Unmarshaller;

/**
 * The object table used for the GlobalMarshaller. Allows internal and external externalizers to be used for marshalling
 *
 * @author Ryan Emerson
 * @since 10.0
 */
class JbossInternalObjectTable implements ObjectTable {

   private final GlobalMarshaller gm;
   private final PersistenceMarshaller pm;

   JbossInternalObjectTable(GlobalMarshaller gm) {
      this.gm = gm;
      this.pm = gm.persistenceMarshaller;
   }

   @Override
   public ObjectTable.Writer getObjectWriter(Object obj) {
      Class<?> clazz = obj.getClass();
      AdvancedExternalizer internalExt = gm.getExternalizer(gm.internalExts, clazz);
      if (internalExt != null)
         return (out, object) -> gm.writeInternalClean(object, internalExt, out);

      // We still try to write via any configured user externalizers, regardless of the user marshaller impl
      // because calls to writeObject may still fall through to the user marshaller impl.
      AdvancedExternalizer externalExt = gm.getExternalizer(gm.externalExts, clazz);
      if (externalExt != null)
         return (out, object) -> GlobalMarshaller.writeExternalClean(object, externalExt, out);

      try {
         if (pm.isMarshallable(obj))
            return (out, object) -> gm.writeUnknownClean(pm, obj, out);
      } catch (Exception ignore) {
      }
      return null;
   }

   @Override
   public Object readObject(Unmarshaller unmarshaller) throws IOException, ClassNotFoundException {
      int type = unmarshaller.readUnsignedByte();
      switch (type) {
         case ID_INTERNAL:
            return gm.getExternalizer(gm.reverseInternalExts, unmarshaller.readUnsignedByte()).readObject(unmarshaller);
         case ID_EXTERNAL:
            return gm.getExternalizer(gm.reverseExternalExts, unmarshaller.readInt()).readObject(unmarshaller);
         case ID_UNKNOWN:
            return gm.readUnknown(pm, unmarshaller);
         default:
            return null;
      }
   }
}