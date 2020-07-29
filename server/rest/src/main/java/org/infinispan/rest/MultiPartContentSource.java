package org.infinispan.rest;

import java.util.HashMap;
import java.util.Map;

import org.infinispan.rest.framework.ContentSource;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.multipart.AbstractHttpData;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostMultipartRequestDecoder;

public class MultiPartContentSource implements ContentSource {

   private ContentSource source;
   private Map<String, ContentSource> parts;

   static MultiPartContentSource create(FullHttpRequest request) {
      HttpDataFactory factory = new DefaultHttpDataFactory(true);
      HttpPostMultipartRequestDecoder decoder = new HttpPostMultipartRequestDecoder(factory, request);
      Map<String, ContentSource> parts = new HashMap<>();
      while (decoder.hasNext()) {
         AbstractHttpData data = (AbstractHttpData) decoder.next();
         parts.put(data.getName(), new ByteBufContentSource(data.content()));
      }
      decoder.destroy();
      ContentSource source = new ByteBufContentSource(request.content());
      return new MultiPartContentSource(source, parts);
   }

   private MultiPartContentSource(ContentSource source, Map<String, ContentSource> parts) {
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

   public ContentSource getPart(String name) {
      return parts.get(name);
   }
}
