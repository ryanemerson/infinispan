package org.infinispan.rest.resources;

import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.infinispan.rest.framework.Method.GET;
import static org.infinispan.rest.framework.Method.POST;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.rest.InvocationHelper;
import org.infinispan.rest.NettyRestResponse;
import org.infinispan.rest.framework.ResourceHandler;
import org.infinispan.rest.framework.RestRequest;
import org.infinispan.rest.framework.RestResponse;
import org.infinispan.rest.framework.impl.Invocations;
import org.infinispan.rest.logging.Log;
import org.infinispan.server.core.BackupManager;
import org.infinispan.util.logging.LogFactory;

/**
 * @since 10.0
 */
public class ClusterResource implements ResourceHandler {

   private final static Log LOG = LogFactory.getLog(ClusterResource.class, Log.class);

   private final InvocationHelper invocationHelper;
   private final BackupManager backupManager;

   public ClusterResource(InvocationHelper invocationHelper) {
      this.invocationHelper = invocationHelper;
      this.backupManager = invocationHelper.getServer().getBackupManager();
   }

   @Override
   public Invocations getInvocations() {
      return new Invocations.Builder()
            .invocation().methods(POST).path("/v2/cluster").withAction("stop").handleWith(this::stop)
            .invocation().methods(GET).path("/v2/cluster").withAction("backup").handleWith(this::backup)
            .invocation().methods(POST).path("/v2/cluster").withAction("restore").handleWith(this::restore)
            .create();
   }

   private CompletionStage<RestResponse> stop(RestRequest restRequest) {
      List<String> servers = restRequest.parameters().get("server");

      if (servers != null && !servers.isEmpty()) {
         invocationHelper.getServer().serverStop(servers);
      } else {
         invocationHelper.getServer().clusterStop();
      }
      return CompletableFuture.completedFuture(new NettyRestResponse.Builder().status(NO_CONTENT).build());
   }

   private CompletionStage<RestResponse> backup(RestRequest request) {
      NettyRestResponse.Builder responseBuilder = new NettyRestResponse.Builder();
      return backupManager.create().handle((path, t) -> {
         if (t != null) {
            return responseBuilder.status(INTERNAL_SERVER_ERROR).entity(t.getMessage()).build();
         } else {
            File zip = path.toFile();
            return responseBuilder
                  .contentType(MediaType.APPLICATION_ZIP)
                  .header("Content-Disposition", String.format("attachment; filename=\"%s\"", zip.getName()))
                  .entity(zip)
                  .removeEntity(true)
                  .contentLength(zip.length())
                  .build();
         }
      });
   }

   private CompletionStage<RestResponse> restore(RestRequest request) {
      NettyRestResponse.Builder responseBuilder = new NettyRestResponse.Builder();
      if (!MediaType.APPLICATION_ZIP.equals(request.contentType()))
         return completedFuture(responseBuilder.status(UNSUPPORTED_MEDIA_TYPE).build());

      byte[] bytes = request.contents().rawContent();
      try (InputStream is = new ByteArrayInputStream(bytes)) {
         return backupManager.restore(is).handle((Void, t) -> t != null ?
               responseBuilder.status(INTERNAL_SERVER_ERROR).entity(t.getMessage()).build() :
               responseBuilder.status(NO_CONTENT).build()
         );
      } catch (IOException e) {
         LOG.error(e);
         return completedFuture(responseBuilder.status(INTERNAL_SERVER_ERROR).entity(e.getMessage()).build());
      }
   }
}
