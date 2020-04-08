package org.infinispan.persistence.rest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.infinispan.client.rest.RestCacheClient;
import org.infinispan.client.rest.RestClient;
import org.infinispan.client.rest.RestEntity;
import org.infinispan.client.rest.RestResponse;
import org.infinispan.client.rest.configuration.Protocol;
import org.infinispan.client.rest.configuration.RestClientConfiguration;
import org.infinispan.client.rest.configuration.RestClientConfigurationBuilder;
import org.infinispan.commons.CacheException;
import org.infinispan.commons.configuration.ConfiguredBy;
import org.infinispan.commons.dataconversion.ByteArrayWrapper;
import org.infinispan.commons.dataconversion.IdentityEncoder;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.marshall.MarshallUtil;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.commons.persistence.Store;
import org.infinispan.commons.util.AbstractIterator;
import org.infinispan.commons.util.Util;
import org.infinispan.container.impl.InternalEntryFactory;
import org.infinispan.encoding.DataConversion;
import org.infinispan.factories.impl.BasicComponentRegistry;
import org.infinispan.marshall.core.EncoderRegistry;
import org.infinispan.metadata.Metadata;
import org.infinispan.persistence.keymappers.MarshallingTwoWayKey2StringMapper;
import org.infinispan.persistence.rest.configuration.ConnectionPoolConfiguration;
import org.infinispan.persistence.rest.configuration.RestStoreConfiguration;
import org.infinispan.persistence.rest.logging.Log;
import org.infinispan.persistence.rest.metadata.MetadataHelper;
import org.infinispan.persistence.spi.AdvancedLoadWriteStore;
import org.infinispan.persistence.spi.InitializationContext;
import org.infinispan.persistence.spi.MarshallableEntry;
import org.infinispan.persistence.spi.MarshallableEntryFactory;
import org.infinispan.persistence.spi.PersistenceException;
import org.infinispan.reactive.RxJavaInterop;
import org.infinispan.util.concurrent.CompletionStages;
import org.infinispan.util.logging.LogFactory;
import org.reactivestreams.Publisher;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import io.reactivex.Flowable;
import net.jcip.annotations.ThreadSafe;

/**
 * RestStore.
 *
 * @author Tristan Tarrant
 * @since 6.0
 * @deprecated This cache store will be changed to only implement {@link org.infinispan.persistence.spi.CacheLoader}
 */
@Store(shared = true)
@ThreadSafe
@ConfiguredBy(RestStoreConfiguration.class)
public class RestStore<K, V> implements AdvancedLoadWriteStore<K, V> {
   private static final String MAX_IDLE_TIME_SECONDS = "maxIdleTimeSeconds";
   private static final String TIME_TO_LIVE_SECONDS = "timeToLiveSeconds";
   private static final String CREATED = "created";
   private static final String LAST_USED = "lastUsed";
   private static final Log log = LogFactory.getLog(RestStore.class, Log.class);
   private volatile RestStoreConfiguration configuration;
   private InternalEntryFactory iceFactory;
   private MarshallingTwoWayKey2StringMapper key2StringMapper;
   private MetadataHelper metadataHelper;
   private InitializationContext ctx;
   private Marshaller marshaller;
   private MarshallableEntryFactory<K, V> entryFactory;

   private RestClient client;
   private RestCacheClient cacheClient;
   private String initialCtxCache;

   private DataConversion keyConversion;
   private DataConversion valueConversion;

   @Override
   public void init(InitializationContext initializationContext) {
      configuration = initializationContext.getConfiguration();
      ctx = initializationContext;
      marshaller = ctx.getPersistenceMarshaller();
      entryFactory = ctx.getMarshallableEntryFactory();
      initialCtxCache = initializationContext.getCache().getName();
   }

   @Override
   public void start() {
      if (iceFactory == null) {
         iceFactory = ctx.getCache().getAdvancedCache().getComponentRegistry().getComponent(InternalEntryFactory.class);
      }

      ConnectionPoolConfiguration pool = configuration.connectionPool();
      RestClientConfiguration clientConfig = new RestClientConfigurationBuilder()
            .addServer().host(configuration.host())
            .port(configuration.port())
            .connectionTimeout(pool.connectionTimeout())
            .tcpNoDelay(pool.tcpNoDelay())
            .socketTimeout(pool.socketTimeout())
            .tcpKeepAlive(true)
            .build();
      client = RestClient.forConfiguration(clientConfig);
      String cacheName = configuration.cacheName();

      if (cacheName == null) cacheName = initialCtxCache;

      cacheClient = client.cache(cacheName);

      this.key2StringMapper = Util.getInstance(configuration.key2StringMapper(), ctx.getCache().getAdvancedCache().getClassLoader());
      this.key2StringMapper.setMarshaller(marshaller);
      this.metadataHelper = Util.getInstance(configuration.metadataHelper(), ctx.getCache().getAdvancedCache().getClassLoader());

      BasicComponentRegistry bcr = ctx.getCache().getCacheManager().getGlobalComponentRegistry().getComponent(BasicComponentRegistry.class);
      EncoderRegistry encoderRegistry = bcr.getComponent(EncoderRegistry.class).wired();
      MediaType keyType = MediaType.APPLICATION_OBJECT.withClassType(String.class);
      keyConversion = DataConversion.newKeyDataConversion(IdentityEncoder.class, ByteArrayWrapper.class, keyType).withRequestMediaType(keyType);
      keyConversion.injectDependencies(ctx.getGlobalConfiguration(), encoderRegistry, ctx.getCache().getCacheConfiguration());

      valueConversion = DataConversion.newValueDataConversion(IdentityEncoder.class, ByteArrayWrapper.class, MediaType.APPLICATION_JSON).withRequestMediaType(MediaType.APPLICATION_JSON);
      valueConversion.injectDependencies(ctx.getGlobalConfiguration(), encoderRegistry, ctx.getCache().getCacheConfiguration());
   }

   @Override
   public void stop() {
      try {
         client.close();
      } catch (Exception e) {
         log.cannotCloseClient(e);
      }
   }

   @Override
   public boolean isAvailable() {
      try {
         CompletionStage<RestResponse> exists = cacheClient.exists();
         RestResponse response = CompletionStages.join(exists);
         return response != null && response.getStatus() == 200;
      } catch (Exception e) {
         return false;
      }
   }

   private String encodeKey(Object key) {
      return key2StringMapper.getStringMapping(key);
   }

   private byte[] marshall(String contentType, MarshallableEntry<?, ?> entry) {
      if (configuration.rawValues()) {
         return (byte[]) entry.getValue();
      } else {
         if (isTextContentType(contentType)) {
            return (byte[]) entry.getValue();
         }
         return MarshallUtil.toByteArray(entry.getValueBytes());
      }
   }

   private Object unmarshall(String contentType, byte[] b) throws IOException, ClassNotFoundException {
      if (configuration.rawValues()) {
         return b;
      } else {
         if (isTextContentType(contentType)) {
            return new String(b); // TODO: use response header Content Encoding
         } else {
            return marshaller.objectFromByteBuffer(b);
         }
      }
   }

   private boolean isTextContentType(String contentType) {
      return contentType != null && (contentType.startsWith("text/") || "application/xml".equals(contentType) || "application/json".equals(contentType));
   }

   @Override
   public void write(MarshallableEntry<? extends K, ? extends V> entry) {
      try {
         RestResponse response = CompletionStages.join(writeAsync(entry));
         if (!isSuccessful(response.getStatus())) {
            throw new PersistenceException("Error writing entry");
         }
      } catch (Exception e) {
         throw new PersistenceException(e);
      }
   }

   private CompletionStage<RestResponse> writeAsync(MarshallableEntry<? extends K, ? extends V> entry) {
      String key = (String) keyConversion.fromStorage(entry.getKey());
      byte[] payload = (byte[]) valueConversion.fromStorage(entry.getValue());
      RestEntity restEntity = RestEntity.create(MediaType.APPLICATION_JSON, payload);
      Metadata metadata = entry.getMetadata();
      CompletionStage<RestResponse> req;
      if (metadata != null && entry.expiryTime() > -1) {
         long ttl = timeoutToSeconds(metadata.lifespan());
         long maxIdle = timeoutToSeconds(metadata.maxIdle());
         return cacheClient.put(key, restEntity, ttl, maxIdle);
      } else {
         return cacheClient.put(key, restEntity);
      }
   }

   private class EmptyRestResponse implements RestResponse {

      @Override
      public int getStatus() {
         return 0;
      }

      @Override
      public Map<String, List<String>> headers() {
         return null;
      }

      @Override
      public InputStream getBodyAsStream() {
         return null;
      }

      @Override
      public byte[] getBodyAsByteArray() {
         return new byte[0];
      }

      @Override
      public Protocol getProtocol() {
         return null;
      }

      @Override
      public void close() {

      }

      @Override
      public String getBody() {
         return null;
      }

      @Override
      public MediaType contentType() {
         return null;
      }
   }

   @Override
   public CompletionStage<Void> bulkUpdate(Publisher<MarshallableEntry<? extends K, ? extends V>> publisher) {
      return Flowable.fromPublisher(publisher)
            .concatMapSingle(me -> {
               CompletionStage<RestResponse> restResponseStage = writeAsync(me);
               return RxJavaInterop.completionStageToMaybe(restResponseStage)
                     .doAfterSuccess(rr -> {
                        rr.close();
                        if (!isSuccessful(rr.getStatus())) {
                           throw new PersistenceException("Failed to clear remote store");
                        }
                     })
                     // Just here in case if we didn't get a rest response back from the client (don't know the contract)
                     .toSingle(new EmptyRestResponse());
            })
            .rebatchRequests(configuration.maxBatchSize())
            .to(RxJavaInterop.flowableToCompletionStage());
   }

   @Override
   public void clear() {
      try {
         CompletionStage<RestResponse> clear = cacheClient.clear();
         RestResponse response = CompletionStages.join(clear);
         response.close();
         if (!isSuccessful(response.getStatus())) throw new PersistenceException("Failed to clear remote store");
      } catch (Exception e) {
         throw new PersistenceException(e);
      }
   }

   @Override
   public boolean delete(Object key) {
      try {
         CompletionStage<RestResponse> remove = cacheClient.remove(encodeKey(key));
         RestResponse response = CompletionStages.join(remove);
         return isSuccessful(response.getStatus());
      } catch (Exception e) {
         throw new PersistenceException(e);
      }
   }

   @Override
   public MarshallableEntry<K, V> loadEntry(Object key) {
      return load(key, true, true);
   }

   private String getHeader(String name, RestResponse restResponse) {
      List<String> values = restResponse.headers().get(name);
      if (values == null || values.isEmpty()) return null;
      return values.iterator().next();
   }

   private MarshallableEntry<K, V> load(Object key, boolean fetchValue, boolean fetchMetadata) {
      try (RestResponse response = CompletionStages.join(cacheClient.get(encodeKey(key)))) {
         if (isSuccessful(response.getStatus())) {
            String contentType = getHeader("Content-Type", response);
            Metadata metadata;
            long created, lastUsed;
            if (fetchMetadata) {
               long ttl = timeHeaderToLong(getHeader(TIME_TO_LIVE_SECONDS, response));
               long maxidle = timeHeaderToLong(getHeader(MAX_IDLE_TIME_SECONDS, response));
               metadata = metadataHelper.buildMetadata(contentType, ttl, TimeUnit.SECONDS, maxidle, TimeUnit.SECONDS);
               created = timeHeaderToLong(getHeader(CREATED, response));
               lastUsed = timeHeaderToLong(getHeader(LAST_USED, response));
            } else {
               metadata = null;
               created = -1;
               lastUsed = -1;
            }
            Object value;
            if (fetchValue) {
               byte[] bytes = response.getBodyAsByteArray();
               value = unmarshall(contentType, bytes);
            } else {
               value = null;
            }

            return entryFactory.create(key, value, metadata, created, lastUsed);

         } else if (response.getStatus() == 404) {
            return null;
         } else {
            throw log.httpError(String.valueOf(response.getStatus()));
         }
      } catch (IOException e) {
         throw log.httpError(e);
      } catch (Exception e) {
         throw new PersistenceException(e);
      }
   }

   private long timeoutToSeconds(long timeout) {
      if (timeout < 0)
         return -1;
      else if (timeout > 0 && timeout < 1000)
         return 1;
      else
         return TimeUnit.MILLISECONDS.toSeconds(timeout);
   }

   private long timeHeaderToLong(String header) {
      return header == null ? -1 : Long.parseLong(header);
   }

   @Override
   public Flowable<K> publishKeys(Predicate<? super K> filter) {
      return Flowable.using(() -> {
         RestResponse response = CompletionStages.join(cacheClient.keys());
         return new BufferedReader(new InputStreamReader(response.getBodyAsStream()));
      }, kvp -> Flowable.fromIterable(() -> new RestIterator(kvp, filter)), BufferedReader::close);
   }

   @Override
   public Flowable<MarshallableEntry<K, V>> entryPublisher(Predicate<? super K> filter, boolean fetchValue, boolean fetchMetadata) {
      Flowable<K> keyFlowable = publishKeys(filter);
      if (!fetchValue && !fetchMetadata) {
         return keyFlowable.map(k -> entryFactory.create(k));
      } else {
         return keyFlowable.map(k -> {
            // Technically this load will only be done synchronously but we are fine with that
            MarshallableEntry<K, V> entry = load(k, fetchValue, fetchMetadata);
            if (entry == null) {
               // Rxjava2 doesn't allow nulls
               entry = entryFactory.getEmpty();
            }
            return entry;
         }).filter(me -> me != entryFactory.getEmpty());
      }
   }

   @Override
   public void purge(Executor executor, PurgeListener<? super K> purgeListener) {
      // This should be handled by the remote server
   }

   @Override
   public int size() {
      try {
         RestResponse response = CompletionStages.join(cacheClient.size());
         String sizeContent = response.getBody();
         try {
            return Integer.parseInt(sizeContent);
         } catch (NumberFormatException e) {
            throw log.errorGettingCacheSize(e);
         }
      } catch (Exception e) {
         throw log.errorLoadingRemoteEntries(e);
      }
   }

   @Override
   public boolean contains(Object o) {
      return loadEntry(o) != null;
   }

   private boolean isSuccessful(int status) {
      return status >= 200 && status < 300;
   }

   private class RestIterator extends AbstractIterator<K> {
      private final Predicate<? super K> filter;
      private JsonParser jp;

      RestIterator(BufferedReader reader, Predicate<? super K> filter) {
         this.filter = filter;
         try {
            jp = new JsonFactory().createJsonParser(reader);
            JsonToken token = jp.nextToken();

            if (token == null) {
               throw new CacheException("empty response from keys");
            }

            if (!JsonToken.START_ARRAY.equals(token)) {
               throw new CacheException("empty response from keys");
            }

         } catch (IOException e) {
            throw new CacheException(e);
         }
      }

      @Override
      protected K getNext() {
         K key = null;
         try {
            while (key == null && !JsonToken.END_ARRAY.equals(jp.nextToken())) {
               K tmpKey = (K) key2StringMapper.getKeyMapping(jp.getText());
               if (filter == null || filter.test(tmpKey)) {
                  key = tmpKey;
               }
            }
         } catch (IOException e) {
            throw new CacheException(e);
         }
         return key;
      }
   }
}
