package org.infinispan.configuration.cache;

import static org.infinispan.configuration.cache.IndexReaderConfiguration.REFRESH_INTERVAL;

import java.util.HashMap;
import java.util.Map;

import org.infinispan.commons.configuration.Builder;
import org.infinispan.commons.configuration.ConfigurationBuilderInfo;
import org.infinispan.commons.configuration.attributes.Attribute;
import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.infinispan.commons.configuration.elements.ElementDefinition;
import org.infinispan.configuration.global.GlobalConfiguration;

/**
 * @since 12.0
 */
public class IndexReaderConfigurationBuilder extends AbstractIndexingConfigurationChildBuilder
      implements Builder<IndexReaderConfiguration>, ConfigurationBuilderInfo {

   private static final String REFRESH_INTERVAL_KEY = "hibernate.search.backend.io.refresh_interval";

   private final AttributeSet attributes;
   private final Attribute<Long> refreshInterval;

   IndexReaderConfigurationBuilder(IndexingConfigurationBuilder builder) {
      super(builder);
      this.attributes = IndexReaderConfiguration.attributeDefinitionSet();
      this.refreshInterval = attributes.attribute(REFRESH_INTERVAL);
   }

   Map<String, Object> asInternalProperties() {
      Map<String, Object> props = new HashMap<>();
      if (refreshInterval.isModified() && refreshInterval.get() != 0) {
         props.put(REFRESH_INTERVAL_KEY, refreshInterval());
      }
      return props;
   }

   public IndexReaderConfigurationBuilder refreshInterval(long valueMillis) {
      refreshInterval.set(valueMillis);
      return this;
   }

   long refreshInterval() {
      return refreshInterval.get();
   }

   @Override
   public ElementDefinition<IndexReaderConfiguration> getElementDefinition() {
      return IndexReaderConfiguration.ELEMENT_DEFINITION;
   }

   @Override
   public AttributeSet attributes() {
      return attributes;
   }

   @Override
   public IndexReaderConfiguration create() {
      return new IndexReaderConfiguration(attributes.protect());
   }

   @Override
   public IndexReaderConfigurationBuilder read(IndexReaderConfiguration template) {
      this.attributes.read(template.attributes());
      return this;
   }

   @Override
   public String toString() {
      return "IndexReaderConfigurationBuilder{" +
            "attributes=" + attributes +
            '}';
   }

   @Override
   public void validate() {
   }

   @Override
   public void validate(GlobalConfiguration globalConfig) {
   }


}
