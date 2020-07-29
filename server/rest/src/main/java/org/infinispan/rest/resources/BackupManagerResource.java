package org.infinispan.rest.resources;

import static io.netty.handler.codec.http.HttpResponseStatus.ACCEPTED;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
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
import java.util.function.BiFunction;

import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.dataconversion.internal.Json;
import org.infinispan.rest.MultiPartContentSource;
import org.infinispan.rest.NettyRestResponse;
import org.infinispan.rest.framework.Method;
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
      Method method = request.method();
      switch (method) {
         case DELETE:
            return handleDeleteBackup(name, backupManager);
         case GET:
         case HEAD:
            return handleGetBackup(name, backupManager, method);
         case POST:
            return handleCreateBackup(name, request, backupManager, creationConsumer);
         default:
            throw new IllegalStateException("Unsupported request method " + method);
      }
   }

   private static CompletionStage<RestResponse> handleCreateBackup(String name, RestRequest request, BackupManager backupManager,
                                                                   BiConsumer<String, Json> creationConsumer) {
      BackupManager.Status existingStatus = backupManager.getBackupStatus(name);
      if (existingStatus != BackupManager.Status.NOT_FOUND) {
         RestResponse response = new NettyRestResponse.Builder().status(CONFLICT).build();
         return CompletableFuture.completedFuture(response);
      }
      Json json = Json.read(request.contents().asString());
      creationConsumer.accept(name, json);
      RestResponse responseBuilder = new NettyRestResponse.Builder().status(ACCEPTED).build();
      return CompletableFuture.completedFuture(responseBuilder);
   }

   private static CompletionStage<RestResponse> handleDeleteBackup(String name, BackupManager backupManager) {
      NettyRestResponse.Builder responseBuilder = new NettyRestResponse.Builder();
      return backupManager.removeBackup(name).handle((s, t) -> {
         if (t != null)
            return responseBuilder.status(INTERNAL_SERVER_ERROR).entity(t.getMessage()).build();

         switch (s) {
            case NOT_FOUND:
               return responseBuilder.status(NOT_FOUND).build();
            case IN_PROGRESS:
               return responseBuilder.status(ACCEPTED).build();
            case COMPLETE:
               return responseBuilder.status(NO_CONTENT).build();
            default:
               return responseBuilder.status(INTERNAL_SERVER_ERROR).build();
         }
      });
   }

   private static CompletionStage<RestResponse> handleGetBackup(String name, BackupManager backupManager, Method method) {
      NettyRestResponse.Builder responseBuilder = new NettyRestResponse.Builder();
      BackupManager.Status status = backupManager.getBackupStatus(name);

      if (status == BackupManager.Status.FAILED) {
         responseBuilder.status(INTERNAL_SERVER_ERROR);
      } else if (status == BackupManager.Status.NOT_FOUND) {
         responseBuilder.status(NOT_FOUND);
      } else if (status == BackupManager.Status.IN_PROGRESS) {
         responseBuilder.status(ACCEPTED);
      } else {
         File zip = backupManager.getBackupLocation(name).toFile();
         responseBuilder
               .contentType(MediaType.APPLICATION_ZIP)
               .header("Content-Disposition", String.format("attachment; filename=%s", zip.getName()))
               .contentLength(zip.length());

         if (method == Method.GET)
            responseBuilder.entity(zip);
      }
      return CompletableFuture.completedFuture(responseBuilder.build());
   }

   static CompletionStage<RestResponse> handleRestoreRequest(RestRequest request, BiFunction<InputStream, Json, CompletionStage<Void>> function) {
      NettyRestResponse.Builder responseBuilder = new NettyRestResponse.Builder();

      byte[] bytes;
      Json json = null;
      MediaType contentType = request.contentType();
      if (MediaType.APPLICATION_ZIP.match(contentType)) {
         bytes = request.contents().rawContent();
      } else if (contentType.match(MediaType.MULTIPART_FORM_DATA)) {
         MultiPartContentSource source = (MultiPartContentSource) request.contents();
         json = Json.read(source.getPart("resources").asString());
         bytes = source.getPart("backup").rawContent();
      } else {
         return completedFuture(responseBuilder.status(UNSUPPORTED_MEDIA_TYPE).build());
      }

      try (InputStream is = new ByteArrayInputStream(bytes)) {
         return function.apply(is, json).handle((Void, t) -> t != null ?
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
      Map<String, Object> jsonMap = json.asMap();
      if (jsonMap.isEmpty())
         return builder.includeAll().build();

      for (Map.Entry<String, Object> e : jsonMap.entrySet()) {
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
}
