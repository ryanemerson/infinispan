package org.infinispan.rest;

import java.util.HashMap;
import java.util.Map;

import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.rest.framework.ContentSource;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.multipart.AbstractHttpData;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostMultipartRequestDecoder;

/**
 * // TODO: Document this
 *
 * @author Ryan Emerson
 * @since 12.0
 */
// TODO expose ByteBufInputStream from ContentSource? Throw IllegalState if called more than once?
public class MultiPartContentSource implements ContentSource {

   private ContentSource source;
   private Map<String, Part> parts = new HashMap<>();

   static MultiPartContentSource create(FullHttpRequest request) {
      HttpDataFactory factory = new DefaultHttpDataFactory(true);
      HttpPostMultipartRequestDecoder decoder = new HttpPostMultipartRequestDecoder(factory, request);
      Map<String, Part> parts = new HashMap<>();
      while (decoder.hasNext()) {
         AbstractHttpData data = (AbstractHttpData) decoder.next();
         String name = data.getName();
         // TODO how to retrieve?
         MediaType type = null;
         ByteBuf content = data.content();
         parts.put(name, new Part(name, type, content));
      }
      decoder.destroy();
      ContentSource source = new ByteBufContentSource(request.content());
      return new MultiPartContentSource(source, parts);
   }

   private MultiPartContentSource(ContentSource source, Map<String, Part> parts) {
      this.source = source;
      this.parts = parts;
   }

   @Override
   public String asString() {
      return source.asString();
   }

   @Override
   public byte[] rawContent() {
      return source.rawContent();
   }

   // TODO restrict to name param
   public Map<String, Part> getParts() {
      return parts;
   }

   public ContentSource getPart(String name) {
      return parts.get(name);
   }

   public static class Part implements ContentSource {

      final String name;
      final MediaType type;
      final ContentSource content;

      Part(String name, MediaType type, ByteBuf content) {
         this.name = name;
         this.type = type;
         this.content = new ByteBufContentSource(content);
      }

      @Override
      public String asString() {
         return content.asString();
      }

      @Override
      public byte[] rawContent() {
         return content.rawContent();
      }
   }
}
