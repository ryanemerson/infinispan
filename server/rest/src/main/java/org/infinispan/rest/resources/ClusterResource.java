package org.infinispan.rest.resources;

import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.infinispan.rest.framework.Method.GET;
import static org.infinispan.rest.framework.Method.POST;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletionStage;

import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.rest.InvocationHelper;
import org.infinispan.rest.NettyRestResponse;
import org.infinispan.rest.framework.ResourceHandler;
import org.infinispan.rest.framework.RestRequest;
import org.infinispan.rest.framework.RestResponse;
import org.infinispan.rest.framework.impl.Invocations;
import org.infinispan.server.core.BackupManager;

import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * @since 10.0
 */
public class ClusterResource implements ResourceHandler {
   private final InvocationHelper invocationHelper;
   private final BackupManager backupManager;

   public ClusterResource(InvocationHelper invocationHelper) {
      this.invocationHelper = invocationHelper;
      this.backupManager = invocationHelper.getServer().getBackupManager();
   }

   @Override
   public Invocations getInvocations() {
      return new Invocations.Builder()
            .invocation().methods(GET, POST).path("/v2/cluster").withAction("stop").handleWith(this::stop)
            // TODO create 'infinispan-<dd-mm-yy>.zip' optional archive name as server-resource
            .invocation().methods(GET, POST).path("/v2/cluster").withAction("backup").handleWith(this::backup)
            .invocation().methods(GET, POST).path("/v2/cluster").withAction("restore").handleWith(this::restore)
            .create();
   }

   private CompletionStage<RestResponse> stop(RestRequest restRequest) {
      List<String> servers = restRequest.parameters().get("server");

      HttpResponseStatus status = restRequest.method().equals(POST) ? NO_CONTENT: OK;

      if (servers != null && !servers.isEmpty()) {
         invocationHelper.getServer().serverStop(servers);
      } else {
         invocationHelper.getServer().clusterStop();
      }
      return completedFuture(new NettyRestResponse.Builder().status(status).build());
   }

   private CompletionStage<RestResponse> backup(RestRequest request) {
      NettyRestResponse.Builder responseBuilder = new NettyRestResponse.Builder();
      return backupManager.create().handle((path, t) -> {
         if (t != null) {
            t.printStackTrace(System.err);
            return responseBuilder.status(HttpResponseStatus.INTERNAL_SERVER_ERROR).entity(t.getMessage()).build();
         } else {
            File zip = path.toFile();
            return responseBuilder
                  .contentType(MediaType.APPLICATION_ZIP)
                  // TODO update filename
                  .header("Content-Disposition", "attachment; filename=backup.zip")
                  .entity(zip)
                  .contentLength(zip.length())
                  .build();
         }
      });
   }

   private CompletionStage<RestResponse> restore(RestRequest request) {
      NettyRestResponse.Builder responseBuilder = new NettyRestResponse.Builder();
      if (!MediaType.APPLICATION_ZIP.equals(request.contentType()))
         return completedFuture(responseBuilder.status(HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE).build());

      byte[] bytes = request.contents().rawContent();
      return backupManager.restore(bytes).handle((Void, t) -> t != null ?
            responseBuilder.status(HttpResponseStatus.INTERNAL_SERVER_ERROR).entity(t.getMessage()).build() :
            responseBuilder.status(HttpResponseStatus.CREATED).build()
      );
   }
}
