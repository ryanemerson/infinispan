package org.infinispan.rest.resources;

import static io.netty.handler.codec.http.HttpResponseStatus.ACCEPTED;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE;
import static java.util.concurrent.CompletableFuture.completedFuture;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.dataconversion.internal.Json;
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

   static CompletionStage<RestResponse> handleBackupRequest(RestRequest request, BackupManager backupManager,
                                                            BiConsumer<String, Json> creationConsumer) {
      String name = request.variables().get("backupName");
      switch (request.method()) {
         case DELETE:
            return handleDeleteBackup(name, backupManager);
         case GET:
            return handleGetBackup(name, backupManager);
         case POST:
            return handleCreateBackup(name, request, creationConsumer);
         default:
            // TODO update
            throw new IllegalArgumentException("TODO");
      }
   }

   private static CompletionStage<RestResponse> handleCreateBackup(String name, RestRequest request,
                                                                   BiConsumer<String, Json> creationConsumer) {
      Json json = Json.read(request.contents().asString());
      creationConsumer.accept(name, json);
      RestResponse responseBuilder = new NettyRestResponse.Builder().status(ACCEPTED).build();
      return CompletableFuture.completedFuture(responseBuilder);
   }

   private static CompletionStage<RestResponse> handleDeleteBackup(String name, BackupManager backupManager) {
      NettyRestResponse.Builder responseBuilder = new NettyRestResponse.Builder();
      BackupManager.Status status = backupManager.getBackupStatus(name);
      if (status == BackupManager.Status.NOT_FOUND)
         return CompletableFuture.completedFuture(responseBuilder.status(NOT_FOUND).build());

      return backupManager.removeBackup(name).handle((Void, t) -> {
         if (t != null) {
            responseBuilder.status(INTERNAL_SERVER_ERROR).entity(t.getMessage());
         } else {
            responseBuilder.status(NO_CONTENT);
         }
         return responseBuilder.build();
      });
   }

   private static CompletionStage<RestResponse> handleGetBackup(String name, BackupManager backupManager) {
      NettyRestResponse.Builder responseBuilder = new NettyRestResponse.Builder();
      BackupManager.Status status = backupManager.getBackupStatus(name);
      switch (status) {
         case COMPLETE:
            File zip = backupManager.getBackup(name).toFile();
            responseBuilder
                  .contentType(MediaType.APPLICATION_ZIP)
                  .header("Content-Disposition", String.format("attachment; filename=%s", zip.getName()))
                  .entity(zip)
                  .contentLength(zip.length())
                  .build();
            break;
         case IN_PROGRESS:
            responseBuilder.status(ACCEPTED);
            break;
         case NOT_FOUND:
            responseBuilder.status(NOT_FOUND);
            break;
         default:
            // TODO add message?
            responseBuilder.status(INTERNAL_SERVER_ERROR);
      }
      return CompletableFuture.completedFuture(responseBuilder.build());
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

   static BackupManager.Resources getResources(Json json) {
      BackupManagerResources.Builder builder = new BackupManagerResources.Builder();
      for (Map.Entry<String, Object> e : json.asMap().entrySet()) {
         @SuppressWarnings("unchecked")
         List<String> resources = (List<String>) e.getValue();
         BackupManager.Resources.Type type = BackupManager.Resources.Type.fromString(e.getKey());
         if (resources.size() == 1 && resources.get(0).equals("*")) {
            builder.includeAll(type);
         } else {
            builder.addResources(type, resources);
         }
      }
      return builder.build();
   }


   // TODO update to process JSON
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
