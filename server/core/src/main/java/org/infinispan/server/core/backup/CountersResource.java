package org.infinispan.server.core.backup;

import static org.infinispan.server.core.BackupManager.ResourceType.COUNTERS;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.infinispan.commons.CacheException;
import org.infinispan.counter.api.CounterConfiguration;
import org.infinispan.counter.api.CounterManager;
import org.infinispan.counter.api.CounterType;
import org.infinispan.counter.api.StrongCounter;
import org.infinispan.counter.api.WeakCounter;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.marshall.protostream.impl.SerializationContextRegistry;
import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.server.core.BackupManager;
import org.infinispan.util.concurrent.BlockingManager;
import org.infinispan.util.concurrent.CompletionStages;

import io.reactivex.rxjava3.core.Flowable;

/**
 * // TODO: Document this
 *
 * @author Ryan Emerson
 * @since 11.0
 */
public class CountersResource extends AbstractContainerResource {

   private static final String COUNTERS_FILE = "counters.dat";

   final CounterManager counterManager;
   final ImmutableSerializationContext serCtx;

   public CountersResource(BlockingManager blockingManager, EmbeddedCacheManager cm,
                           BackupManager.Parameters params, Path root) {
      super(COUNTERS, params, root, blockingManager, cm);
      GlobalComponentRegistry gcr = cm.getGlobalComponentRegistry();
      this.counterManager = gcr.getComponent(CounterManager.class);
      this.serCtx = gcr.getComponent(SerializationContextRegistry.class).getPersistenceCtx();
   }

   @Override
   public CompletionStage<Void> backup() {
      return blockingManager.runBlocking(() -> {
         Set<String> counterNames = qualifiedResources;
         if (wildcard)
            counterNames.addAll(counterManager.getCounterNames());

         root.toFile().mkdir();
         Flowable.using(
               () -> Files.newOutputStream(root.resolve(COUNTERS_FILE)),
               output ->
                     Flowable.fromIterable(counterNames)
                           .map(counter -> {
                              CounterConfiguration config = counterManager.getConfiguration(counter);
                              CounterBackupEntry e = new CounterBackupEntry();
                              e.name = counter;
                              e.configuration = config;
                              e.value = config.type() == CounterType.WEAK ?
                                    counterManager.getWeakCounter(counter).getValue() :
                                    CompletionStages.join(counterManager.getStrongCounter(counter).getValue());
                              return e;
                           })
                           .doOnNext(e -> writeMessageStream(e, serCtx, output))
                           .doOnError(t -> {
                              throw new CacheException("Unable to create counter backup", t);
                           }),
               OutputStream::close
         ).subscribe();
      }, "write-counters");
   }

   @Override
   public CompletionStage<Void> restore(Properties properties, ZipFile zip) {
      return blockingManager.runBlocking(() -> {
         Set<String> countersToRestore = resourcesToRestore(properties);
         String countersFile = root.resolve(COUNTERS_FILE).toString();
         ZipEntry zipEntry = zip.getEntry(countersFile);
         if (zipEntry == null) {
            if (!countersToRestore.isEmpty())
               throw log.unableToFindBackupResource(type.toString(), countersToRestore);
            return;
         }

         try (InputStream is = zip.getInputStream(zipEntry)) {
            while (is.available() > 0) {
               CounterBackupEntry entry = readMessageStream(serCtx, CounterBackupEntry.class, is);
               if (!countersToRestore.isEmpty() && !countersToRestore.contains(entry.name)) {
                  log.debugf("Ignoring '%s' counter", entry.name);
                  continue;
               }
               CounterConfiguration config = entry.configuration;
               counterManager.defineCounter(entry.name, config);
               if (config.type() == CounterType.WEAK) {
                  WeakCounter counter = counterManager.getWeakCounter(entry.name);
                  counter.add(entry.value - config.initialValue());
               } else {
                  StrongCounter counter = counterManager.getStrongCounter(entry.name);
                  counter.compareAndSet(config.initialValue(), entry.value);
               }
            }
         } catch (IOException e) {
            throw new CacheException(e);
         }
      }, "restore-counters");
   }
}
