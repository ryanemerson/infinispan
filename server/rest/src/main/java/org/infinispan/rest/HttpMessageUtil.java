package org.infinispan.rest;

import java.util.Map;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.util.internal.StringUtil;

/**
 * @author Ryan Emerson
 * @since 14.0
 */
final class HttpMessageUtil {
   static String dumpRequest(HttpRequest req) {
      StringBuilder sb = new StringBuilder();
      appendCommon(sb, req);
      appendInitialLine(sb, req);
      appendHeaders(sb, req.headers());
      removeLastNewLine(sb);
      return sb.toString();
   }

   static String dumpResponse(HttpResponse res) {
      StringBuilder sb = new StringBuilder();
      appendCommon(sb, res);
      appendInitialLine(sb, res);
      appendHeaders(sb, res.headers());
      removeLastNewLine(sb);
      return sb.toString();
   }

   private static void appendCommon(StringBuilder buf, HttpMessage msg) {
      buf.append(StringUtil.simpleClassName(msg));
      buf.append("(decodeResult: ");
      buf.append(msg.decoderResult());
      buf.append(", version: ");
      buf.append(msg.protocolVersion());
      buf.append(')');
      buf.append(StringUtil.NEWLINE);
   }

   private static void appendInitialLine(StringBuilder buf, HttpRequest req) {
      buf.append(req.method());
      buf.append(' ');
      buf.append(req.uri());
      buf.append(' ');
      buf.append(req.protocolVersion());
      buf.append(StringUtil.NEWLINE);
   }

   private static void appendInitialLine(StringBuilder buf, HttpResponse res) {
      buf.append(res.protocolVersion());
      buf.append(' ');
      buf.append(res.status());
      buf.append(StringUtil.NEWLINE);
   }

   private static void appendHeaders(StringBuilder buf, HttpHeaders headers) {
      for (Map.Entry<String, String> header : headers) {
         buf.append(header.getKey());
         buf.append(": ");
         buf.append(header.getValue());
         buf.append(StringUtil.NEWLINE);
      }
   }

   private static void removeLastNewLine(StringBuilder buf) {
      buf.setLength(buf.length() - StringUtil.NEWLINE.length());
   }

   private HttpMessageUtil() {
   }
}
