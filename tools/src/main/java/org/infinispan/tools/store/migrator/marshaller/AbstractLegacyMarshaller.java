package org.infinispan.tools.store.migrator.marshaller;

import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;

import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.io.ByteBuffer;
import org.infinispan.commons.marshall.AbstractMarshaller;
import org.infinispan.commons.marshall.StreamingMarshaller;
import org.infinispan.commons.marshall.jboss.AbstractJBossMarshaller;
import org.infinispan.commons.marshall.jboss.DefaultContextClassResolver;
import org.infinispan.commons.marshall.jboss.SerializeWithExtFactory;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.jboss.marshalling.ObjectTable;

abstract class AbstractLegacyMarshaller extends AbstractMarshaller implements StreamingMarshaller {

   protected final LegacyJBossMarshaller defaultMarshaller;

   public AbstractLegacyMarshaller(AbstractExternalizerTable externalizerTable) {
      this.defaultMarshaller = new LegacyJBossMarshaller(externalizerTable);
      externalizerTable.initInternalExternalizers(this);
   }

   @Override
   public void stop() {
   }

   @Override
   public void start() {
   }

   @Override
   protected ByteBuffer objectToBuffer(Object obj, int estimatedSize) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void finishObjectInput(ObjectInput oi) {
      defaultMarshaller.finishObjectInput(oi);
   }

   @Override
   public boolean isMarshallable(Object o) throws Exception {
      return defaultMarshaller.isMarshallable(o);
   }

   @Override
   public MediaType mediaType() {
      return defaultMarshaller.mediaType();
   }

   @Override
   public Object objectFromObjectStream(ObjectInput in) {
      throw new UnsupportedOperationException();
   }

   @Override
   public ObjectOutput startObjectOutput(OutputStream os, boolean isReentrant, final int estimatedSize) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void finishObjectOutput(ObjectOutput oo) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void objectToObjectStream(Object obj, ObjectOutput out) {
      throw new UnsupportedOperationException();
   }

   class LegacyJBossMarshaller extends AbstractJBossMarshaller implements StreamingMarshaller {
      LegacyJBossMarshaller(ObjectTable objectTable) {
         baseCfg.setClassExternalizerFactory(new SerializeWithExtFactory());
         baseCfg.setObjectTable(objectTable);
         baseCfg.setClassResolver(new DefaultContextClassResolver(GlobalConfigurationBuilder.class.getClassLoader()));
      }
   }
}
