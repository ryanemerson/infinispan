package org.infinispan.rest.resources;

import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE;
import static java.util.concurrent.CompletableFuture.completedFuture;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.rest.NettyRestResponse;
import org.infinispan.rest.framework.RestRequest;
import org.infinispan.rest.framework.RestResponse;
import org.infinispan.rest.logging.Log;
import org.infinispan.server.core.BackupManager;
import org.infinispan.server.core.backup.BackupManagerResources;
import org.infinispan.util.logging.LogFactory;

/**
 * A helper class for common functionality related to the {@link BackupManager}.
 *
 * @author Ryan Emerson
 * @since 12.0
 */
class BackupManagerResource {

   private final static Log LOG = LogFactory.getLog(BackupManagerResource.class, Log.class);

   static CompletionStage<RestResponse> handleBackupResponse(CompletionStage<Path> backupStage) {
      NettyRestResponse.Builder responseBuilder = new NettyRestResponse.Builder();
      return backupStage.handle((path, t) -> {
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

   static CompletionStage<RestResponse> handleRestoreRequest(RestRequest request, Function<InputStream, CompletionStage<Void>> function) {
      NettyRestResponse.Builder responseBuilder = new NettyRestResponse.Builder();
      if (!MediaType.APPLICATION_ZIP.equals(request.contentType()))
         return completedFuture(responseBuilder.status(UNSUPPORTED_MEDIA_TYPE).build());

      byte[] bytes = request.contents().rawContent();
      try (InputStream is = new ByteArrayInputStream(bytes)) {
         return function.apply(is).handle((Void, t) -> t != null ?
               responseBuilder.status(INTERNAL_SERVER_ERROR).entity(t.getMessage()).build() :
               responseBuilder.status(NO_CONTENT).build()
         );
      } catch (IOException e) {
         LOG.error(e);
         return completedFuture(responseBuilder.status(INTERNAL_SERVER_ERROR).entity(e.getMessage()).build());
      }
   }

   static BackupManager.Resources getResources(RestRequest request) {
      BackupManagerResources.Builder builder = new BackupManagerResources.Builder();
      Map<String, List<String>> params = request.parameters();
      // No resources have been explicitly defined, so we backup/restore all resources
      if (params == null || params.isEmpty() || (params.size() == 1 && params.containsKey("action")))
         return builder.includeAll().build();

      for (BackupManager.Resources.Type type : BackupManager.Resources.Type.values()) {
         String key = type.toString();
         List<String> resources = params.get(key);
         if (resources != null) {
            if (resources.size() == 1 && resources.get(0).equals("*")) {
               builder.includeAll(type);
            } else {
               builder.addResources(type, resources);
            }
         }
      }
      return builder.build();
   }
}
