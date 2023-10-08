package org.infinispan.marshall.core.proto;

import java.io.IOException;

import org.infinispan.commands.RemoteCommandsFactory;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.io.ByteBuffer;
import org.infinispan.commons.marshall.BufferSizePredictor;
import org.infinispan.commons.marshall.Externalizer;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.factories.KnownComponentNames;
import org.infinispan.factories.annotations.ComponentName;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.factories.annotations.Start;
import org.infinispan.factories.annotations.Stop;
import org.infinispan.factories.scopes.Scope;
import org.infinispan.factories.scopes.Scopes;
import org.infinispan.marshall.core.GlobalMarshaller;
import org.infinispan.marshall.persistence.PersistenceMarshaller;

@Scope(Scopes.GLOBAL)
public class DelegatingGlobalMarshaller implements Marshaller {

   // Set to false in order to determine what can't be serialized with the new marshaller
   private static final boolean DELEGATE = true;

   final Marshaller newMarshaller;
   final GlobalMarshaller oldMarshaller;

   final MediaType mediaType;

   @Inject
   GlobalComponentRegistry gcr;
   @Inject
   RemoteCommandsFactory cmdFactory;
   @Inject @ComponentName(KnownComponentNames.PERSISTENCE_MARSHALLER)
   PersistenceMarshaller persistenceMarshaller;

   public DelegatingGlobalMarshaller(Marshaller newMarshaller, GlobalMarshaller oldMarshaller, MediaType mediaType) {
      this.newMarshaller = newMarshaller;
      this.oldMarshaller = oldMarshaller;
      this.mediaType = mediaType;
   }

   @Override
   @Start(priority = 8) // Should start after the externalizer table and before transport
   public void start() {
      oldMarshaller.init(gcr, cmdFactory, persistenceMarshaller);
      oldMarshaller.start();
   }

   @Override
   @Stop(priority = 130) // Stop after transport to avoid send/receive and marshaller not being ready
   public void stop() {
      persistenceMarshaller.stop();
   }

   @Override
   public byte[] objectToByteBuffer(Object obj, int estimatedSize) throws IOException, InterruptedException {
      try {
         return newMarshaller.objectToByteBuffer(obj, estimatedSize);
      } catch (Throwable t) {
         if (DELEGATE)
            return oldMarshaller.objectToByteBuffer(obj, estimatedSize);
         throw t;
      }
   }

   @Override
   public byte[] objectToByteBuffer(Object obj) throws IOException, InterruptedException {
      try {
         return newMarshaller.objectToByteBuffer(obj);
      } catch (Throwable t) {
         if (DELEGATE)
            return oldMarshaller.objectToByteBuffer(obj);
         throw t;
      }
   }

   @Override
   public Object objectFromByteBuffer(byte[] buf) throws IOException, ClassNotFoundException {
      try {
         return newMarshaller.objectFromByteBuffer(buf);
      } catch (Throwable t) {
         if (DELEGATE)
            return oldMarshaller.objectFromByteBuffer(buf);
         throw t;
      }
   }

   @Override
   public Object objectFromByteBuffer(byte[] buf, int offset, int length) throws IOException, ClassNotFoundException {
      try {
         return newMarshaller.objectFromByteBuffer(buf, offset, length);
      } catch (Throwable t) {
         if (DELEGATE)
            return oldMarshaller.objectFromByteBuffer(buf, offset, length);
         throw t;
      }
   }

   @Override
   public ByteBuffer objectToBuffer(Object o) throws IOException, InterruptedException {
      try {
         return newMarshaller.objectToBuffer(o);
      } catch (Throwable t) {
         if (DELEGATE)
            return oldMarshaller.objectToBuffer(o);
         throw t;
      }
   }

   @Override
   public boolean isMarshallable(Object o) throws Exception {
      if (DELEGATE)
         return newMarshaller.isMarshallable(o) || oldMarshaller.isMarshallable(o);
      else
         return newMarshaller.isMarshallable(o);
   }

   @Override
   public BufferSizePredictor getBufferSizePredictor(Object o) {
      try {
         if (newMarshaller.isMarshallable(o))
            return newMarshaller.getBufferSizePredictor(o);
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
      if (!DELEGATE)
         throw new IllegalStateException("getBufferSizePredictor");
      return oldMarshaller.getBufferSizePredictor(o);
   }

   @Override
   public MediaType mediaType() {
      return mediaType;
   }

   public Externalizer findExternalizerFor(Object o) {
      return oldMarshaller.findExternalizerFor(o);
   }

   public GlobalMarshaller getOldMarshaller() {
      return oldMarshaller;
   }
}
