package org.infinispan.marshall.protostream;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;

import org.infinispan.commons.marshall.StreamingMarshaller;
import org.infinispan.commons.marshall.protostream.BaseProtoStreamMarshaller;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.config.Configuration;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * TODO make this a generic implementation that can be used by users. PeristenceMarshallerImpl should then just extend this
 * A marshaller that uses Protocol Buffers.
 *
 * @author remerson@redhat.com
 * @since 9.4
 */
public class ProtoStreamMarshaller extends BaseProtoStreamMarshaller implements StreamingMarshaller {

   private static final Log log = LogFactory.getLog(ProtoStreamMarshaller.class, Log.class);

   private final SerializationContext serializationContext = ProtobufUtil.newSerializationContext(Configuration.builder().build());

   @Override
   public SerializationContext getSerializationContext() {
      return serializationContext;
   }

   @Override
   public ObjectOutput startObjectOutput(OutputStream os, boolean isReentrant, int estimatedSize) throws IOException {
      log.errorf(stackTrace(Thread.currentThread().getStackTrace()));
      throw new IllegalStateException();
   }

   @Override
   public void finishObjectOutput(ObjectOutput oo) {
      log.errorf(stackTrace(Thread.currentThread().getStackTrace()));
      throw new IllegalStateException();
   }

   @Override
   public void objectToObjectStream(Object obj, ObjectOutput out) throws IOException {
      log.errorf(stackTrace(Thread.currentThread().getStackTrace()));
      throw new IllegalStateException();
   }

   @Override
   public ObjectInput startObjectInput(InputStream is, boolean isReentrant) throws IOException {
      log.errorf(stackTrace(Thread.currentThread().getStackTrace()));
      throw new IllegalStateException();
   }

   @Override
   public void finishObjectInput(ObjectInput oi) {
      log.errorf(stackTrace(Thread.currentThread().getStackTrace()));
   }

   @Override
   public Object objectFromObjectStream(ObjectInput in) throws IOException, ClassNotFoundException, InterruptedException {
      log.errorf(stackTrace(Thread.currentThread().getStackTrace()));
      throw new IllegalStateException();
   }

   @Override
   public void stop() {
      // TODO: Customise this generated block
   }

   @Override
   public void start() {
      // TODO: Customise this generated block
   }

   private String stackTrace(StackTraceElement[] elements) {
      StringBuilder sb = new StringBuilder();
      for (StackTraceElement element : elements) {
         sb.append(element).append("\n");
      }
      return sb.toString();
   }
}
