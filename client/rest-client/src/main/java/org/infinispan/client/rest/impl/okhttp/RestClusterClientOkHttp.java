package org.infinispan.client.rest.impl.okhttp;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;

import org.infinispan.client.rest.RestClusterClient;
import org.infinispan.client.rest.RestResponse;
import org.infinispan.commons.dataconversion.MediaType;

import okhttp3.Request;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 10.0
 **/
public class RestClusterClientOkHttp implements RestClusterClient {
   private final RestClientOkHttp client;
   private final String baseClusterURL;

   RestClusterClientOkHttp(RestClientOkHttp restClient) {
      this.client = restClient;
      this.baseClusterURL = String.format("%s%s/v2/cluster", restClient.getBaseURL(), restClient.getConfiguration().contextPath()).replaceAll("//", "/");
   }

   @Override
   public CompletionStage<RestResponse> stop() {
      return stop(Collections.emptyList());
   }

   @Override
   public CompletionStage<RestResponse> stop(List<String> servers) {
      Request.Builder builder = new Request.Builder();
      StringBuilder sb = new StringBuilder(baseClusterURL);
      sb.append("?action=stop");
      for (String server : servers) {
         sb.append("&server=");
         sb.append(server);
      }
      builder.url(sb.toString());
      return client.execute(builder);
   }

   @Override
   public CompletionStage<RestResponse> backup() {
      Request.Builder builder = new Request.Builder().url(baseClusterURL + "?action=backup");
      return client.execute(builder);
   }

   @Override
   public CompletionStage<RestResponse> restore(File backup) {
      Request.Builder builder = new Request.Builder()
            .url(baseClusterURL + "?action=restore")
            .post(new FileRestEntityOkHttp(MediaType.APPLICATION_ZIP, backup).toRequestBody());
      return client.execute(builder);
   }
}
