package org.infinispan.server.test.api;

import java.util.EnumSet;

import org.infinispan.commons.api.CacheContainerAdmin;
import org.infinispan.commons.configuration.BasicConfiguration;
import org.infinispan.commons.configuration.Self;
import org.infinispan.commons.configuration.StringConfiguration;
import org.infinispan.configuration.cache.ConfigurationBuilder;

/**
 * Base class for the driver API
 *
 * @since 10
 * @param <S>
 * @author Tristan Tarrant
 */
abstract class BaseTestClientDriver<S extends BaseTestClientDriver<S>> implements Self<S> {
   protected BasicConfiguration serverConfiguration = null;
   protected EnumSet<CacheContainerAdmin.AdminFlag> flags = EnumSet.noneOf(CacheContainerAdmin.AdminFlag.class);
   protected String mode = null;
   protected Object[] qualifiers;

   public S withServerConfiguration(BasicConfiguration serverConfiguration) {
      if (mode != null) {
         throw new IllegalStateException("Cannot set server configuration and cache mode");
      }
      this.serverConfiguration = serverConfiguration;
      return self();
   }

   public S withServerConfiguration(ConfigurationBuilder serverConfiguration) {
      return withServerConfiguration(serverConfiguration.build());
   }

   public S withServerConfiguration(StringConfiguration configuration) {
      if (mode != null) {
         throw new IllegalStateException("Cannot set server configuration and cache mode");
      }
      this.serverConfiguration = configuration;
      return self();
   }

   public S withCacheMode(Enum<?> mode) {
      if (serverConfiguration != null) {
         throw new IllegalStateException("Cannot set server configuration and cache mode");
      }
      this.mode = mode.name();
      return self();
   }

   public S withCacheMode(String mode) {
      if (serverConfiguration != null) {
         throw new IllegalStateException("Cannot set server configuration and cache mode");
      }
      this.mode = mode;
      return self();
   }

   public S withQualifier(String qualifier) {
      return withQualifiers(qualifier);
   }

   public S withQualifiers(Object... qualifiers) {
      this.qualifiers = qualifiers;
      return self();
   }

   public S makeVolatile() {
      this.flags = EnumSet.of(CacheContainerAdmin.AdminFlag.VOLATILE);
      return self();
   }
}
