package org.infinispan.tools.store.migrator.marshaller.common;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.infinispan.commons.util.Util;
import org.infinispan.container.versioning.EntryVersion;
import org.infinispan.metadata.EmbeddedMetadata;

public class EmbeddedMetadataExternalizer extends AbstractMigratorExternalizer<EmbeddedMetadata> {

   private static final int IMMORTAL = 0;
   private static final int EXPIRABLE = 1;
   private static final int LIFESPAN_EXPIRABLE = 2;
   private static final int MAXIDLE_EXPIRABLE = 3;
   private final Map<Class<?>, Integer> numbers = new HashMap<>(4);

   public EmbeddedMetadataExternalizer() {
      numbers.put(EmbeddedMetadata.class, IMMORTAL);
      numbers.put(EmbeddedMetadata.EmbeddedExpirableMetadata.class, EXPIRABLE);
      numbers.put(EmbeddedMetadata.EmbeddedLifespanExpirableMetadata.class, LIFESPAN_EXPIRABLE);
      numbers.put(EmbeddedMetadata.EmbeddedMaxIdleExpirableMetadata.class, MAXIDLE_EXPIRABLE);
   }

   @Override
   public Set<Class<? extends EmbeddedMetadata>> getTypeClasses() {
      return Util.asSet(EmbeddedMetadata.class, EmbeddedMetadata.EmbeddedExpirableMetadata.class,
            EmbeddedMetadata.EmbeddedLifespanExpirableMetadata.class, EmbeddedMetadata.EmbeddedMaxIdleExpirableMetadata.class);
   }

   @Override
   public Integer getId() {
      return Ids.EMBEDDED_METADATA;
   }

   @Override
   public void writeObject(ObjectOutput output, EmbeddedMetadata object) throws IOException {
      int number = numbers.getOrDefault(object.getClass(), -1);
      output.write(number);
      switch (number) {
         case EXPIRABLE:
            output.writeLong(object.lifespan());
            output.writeLong(object.maxIdle());
            break;
         case LIFESPAN_EXPIRABLE:
            output.writeLong(object.lifespan());
            break;
         case MAXIDLE_EXPIRABLE:
            output.writeLong(object.maxIdle());
            break;
      }
      output.writeObject(object.version());
   }

   @Override
   public EmbeddedMetadata readObject(ObjectInput input) throws IOException, ClassNotFoundException {
      int number = input.readByte();
      EmbeddedMetadata.Builder builder = new EmbeddedMetadata.Builder();
      switch (number) {
         case IMMORTAL:
            builder.version((EntryVersion) input.readObject());
            break;
         case EXPIRABLE:
            builder.lifespan(toMillis(input.readLong(), TimeUnit.MILLISECONDS))
                  .maxIdle(toMillis(input.readLong(), TimeUnit.MILLISECONDS))
                  .version((EntryVersion) input.readObject());
            break;
         case LIFESPAN_EXPIRABLE:
            builder.lifespan(toMillis(input.readLong(), TimeUnit.MILLISECONDS))
                  .version((EntryVersion) input.readObject());
            break;
         case MAXIDLE_EXPIRABLE:
            builder.maxIdle(toMillis(input.readLong(), TimeUnit.MILLISECONDS))
                  .version((EntryVersion) input.readObject());
         default:
            throw new IllegalStateException("Unknown metadata type " + number);
      }
      return (EmbeddedMetadata) builder.build();
   }

   private static long toMillis(long duration, TimeUnit timeUnit) {
      return duration < 0 ? -1 : timeUnit.toMillis(duration);
   }
}
