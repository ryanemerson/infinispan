package org.infinispan.configuration.global;

import static org.infinispan.configuration.global.WhiteListConfiguration.CLASSES;
import static org.infinispan.configuration.global.WhiteListConfiguration.REGEXPS;

import java.util.Arrays;

import org.infinispan.commons.configuration.Builder;
import org.infinispan.commons.configuration.ClassWhiteList;
import org.infinispan.commons.configuration.attributes.AttributeSet;

/**
 * Configures the {@link org.infinispan.manager.EmbeddedCacheManager} {@link org.infinispan.commons.configuration.ClassWhiteList}.
 *
 * @author Ryan Emerson
 * @since 10.0
 */
public class WhiteListConfigurationBuilder implements Builder<WhiteListConfiguration> {

   private final AttributeSet attributes;

   WhiteListConfigurationBuilder() {
      attributes = WhiteListConfiguration.attributeDefinitionSet();
   }

   /**
    * Helper method that allows for registration of a class to the {@link ClassWhiteList}.
    */
   public <T> WhiteListConfigurationBuilder addClass(String clazz) {
      attributes.attribute(CLASSES).get().add(clazz);
      return this;
   }

   /**
    * Helper method that allows for registration of classes to the {@link ClassWhiteList}.
    */
   public <T> WhiteListConfigurationBuilder addClasses(String... classes) {
      attributes.attribute(CLASSES).get().addAll(Arrays.asList(classes));
      return this;
   }


   /**
    * Helper method that allows for registration of a regexp to the {@link ClassWhiteList}.
    */
   public <T> WhiteListConfigurationBuilder addRegexps(String regex) {
      attributes.attribute(REGEXPS).get().add(regex);
      return this;
   }

   /**
    * Helper method that allows for registration of regexps to the {@link ClassWhiteList}.
    */
   public <T> WhiteListConfigurationBuilder addRegexp(String... regexps) {
      attributes.attribute(REGEXPS).get().addAll(Arrays.asList(regexps));
      return this;
   }

   @Override
   public void validate() {
      // No-op, no validation required
   }

   @Override
   public WhiteListConfiguration create() {
      return new WhiteListConfiguration(attributes.protect());
   }

   @Override
   public Builder<?> read(WhiteListConfiguration template) {
      this.attributes.read(template.attributes());
      return this;
   }
}
