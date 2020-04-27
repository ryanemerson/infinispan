package org.infinispan.marshall.core.impl;

import static org.infinispan.util.logging.Log.CONTAINER;

import java.io.IOException;

import org.infinispan.commons.configuration.ClassWhiteList;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.io.ByteBuffer;
import org.infinispan.commons.marshall.BufferSizePredictor;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.factories.annotations.Start;
import org.infinispan.factories.annotations.Stop;
import org.infinispan.factories.scopes.Scope;
import org.infinispan.factories.scopes.Scopes;

/**
 * A delegate {@link Marshaller} implementation for the user marshaller that ensures that the {@link Marshaller#start()} and
 * {@link Marshaller#stop()} of the configured marshaller are called and logged as required.
 *
 * @author Ryan Emerson
 * @since 11.0
 */
@Scope(Scopes.GLOBAL)
public class DelegatingUserMarshaller implements Marshaller {

   final Marshaller marshaller;
   final boolean defaultUserMarshaller;

   public DelegatingUserMarshaller(Marshaller marshaller, boolean defaultUserMarshaller) {
      this.marshaller = marshaller;
      this.defaultUserMarshaller = defaultUserMarshaller;
   }

   @Start
   @Override
   public void start() {
      CONTAINER.startingUserMarshaller(marshaller.getClass().getName());
      marshaller.start();
   }

   @Stop
   @Override
   public void stop() {
      marshaller.stop();
   }

   @Override
   public void initialize(ClassWhiteList classWhiteList) {
      marshaller.initialize(classWhiteList);
   }

   @Override
   public byte[] objectToByteBuffer(Object obj, int estimatedSize) throws IOException, InterruptedException {
      return marshaller.objectToByteBuffer(obj, estimatedSize);
   }

   @Override
   public byte[] objectToByteBuffer(Object obj) throws IOException, InterruptedException {
      return marshaller.objectToByteBuffer(obj);
   }

   @Override
   public Object objectFromByteBuffer(byte[] buf) throws IOException, ClassNotFoundException {
      return marshaller.objectFromByteBuffer(buf);
   }

   @Override
   public Object objectFromByteBuffer(byte[] buf, int offset, int length) throws IOException, ClassNotFoundException {
      return marshaller.objectFromByteBuffer(buf, offset, length);
   }

   @Override
   public ByteBuffer objectToBuffer(Object o) throws IOException, InterruptedException {
      return marshaller.objectToBuffer(o);
   }

   @Override
   public boolean isMarshallable(Object o) throws Exception {
      return marshaller.isMarshallable(o);
   }

   @Override
   public BufferSizePredictor getBufferSizePredictor(Object o) {
      return marshaller.getBufferSizePredictor(o);
   }

   @Override
   public MediaType mediaType() {
      return marshaller.mediaType();
   }

   public Marshaller getDelegate() {
      return marshaller;
   }

   public boolean isDefaultUserMarshaller() {
      return defaultUserMarshaller;
   }
}
