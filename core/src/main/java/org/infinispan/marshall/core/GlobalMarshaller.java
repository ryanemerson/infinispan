package org.infinispan.marshall.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.infinispan.commands.RemoteCommandsFactory;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.io.ByteBuffer;
import org.infinispan.commons.marshall.BufferSizePredictor;
import org.infinispan.commons.marshall.MarshallableTypeHints;
import org.infinispan.commons.marshall.StreamAwareMarshaller;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.factories.KnownComponentNames;
import org.infinispan.factories.annotations.ComponentName;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.factories.annotations.Start;
import org.infinispan.factories.annotations.Stop;
import org.infinispan.factories.scopes.Scope;
import org.infinispan.factories.scopes.Scopes;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * A globally-scoped marshaller. This is needed so that the transport layer
 * can unmarshall requests even before it's known which cache's marshaller can
 * do the job.
 *
 * @author Galder Zamarreño
 * @since 5.0
 */
@Scope(Scopes.GLOBAL)
public class GlobalMarshaller implements StreamAwareMarshaller {

   private static final Log log = LogFactory.getLog(GlobalMarshaller.class);
   private static final boolean trace = log.isTraceEnabled();


   private final MarshallableTypeHints marshallableTypeHints = new MarshallableTypeHints();

   @Inject private GlobalComponentRegistry gcr;
   @Inject private RemoteCommandsFactory cmdFactory;
   @Inject @ComponentName(KnownComponentNames.PERSISTENCE_MARSHALLER) private StreamAwareMarshaller persistenceMarshaller;

   public GlobalMarshaller() {
   }

   @Override
   @Start(priority = 8) // Should start after the externalizer table and before transport
   public void start() {
      GlobalConfiguration globalCfg = gcr.getGlobalConfiguration();
}

   @Override
   @Stop(priority = 130) // Stop after transport to avoid send/receive and marshaller not being ready
   public void stop() {
   }

   @Override
   public void writeObject(Object o, OutputStream out) throws IOException {
      // TODO: Customise this generated block
   }

   @Override
   public Object readObject(InputStream in) throws ClassNotFoundException, IOException {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public byte[] objectToByteBuffer(Object obj, int estimatedSize) throws IOException, InterruptedException {
      return new byte[0];  // TODO: Customise this generated block
   }

   @Override
   public byte[] objectToByteBuffer(Object obj) throws IOException, InterruptedException {
      return new byte[0];  // TODO: Customise this generated block
   }

   @Override
   public Object objectFromByteBuffer(byte[] buf) throws IOException, ClassNotFoundException {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public Object objectFromByteBuffer(byte[] buf, int offset, int length) throws IOException, ClassNotFoundException {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public ByteBuffer objectToBuffer(Object o) throws IOException, InterruptedException {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public boolean isMarshallable(Object o) throws Exception {
      return false;  // TODO: Customise this generated block
   }

   @Override
   public BufferSizePredictor getBufferSizePredictor(Object o) {
      return null;  // TODO: Customise this generated block
   }

   @Override
   public MediaType mediaType() {
      return null;  // TODO: Customise this generated block
   }
}
