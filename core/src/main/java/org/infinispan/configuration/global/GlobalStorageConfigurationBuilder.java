package org.infinispan.configuration.global;

import static org.infinispan.configuration.global.GlobalStorageConfiguration.CONFIGURATION_STORAGE_SUPPLIER;

import java.lang.invoke.MethodHandles;
import java.util.function.Supplier;

import org.infinispan.commons.configuration.Builder;
import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.infinispan.globalstate.ConfigurationStorage;
import org.infinispan.globalstate.LocalConfigurationStorage;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

public class GlobalStorageConfigurationBuilder extends AbstractGlobalConfigurationBuilder implements Builder<GlobalStorageConfiguration> {
   private static final Log log = LogFactory.getLog(MethodHandles.lookup().lookupClass());

   private final AttributeSet attributes;
   private ConfigurationStorage storage = ConfigurationStorage.VOLATILE;

   GlobalStorageConfigurationBuilder(GlobalConfigurationBuilder globalConfig) {
      super(globalConfig);
      attributes = GlobalStorageConfiguration.attributeDefinitionSet();
   }

   public GlobalStorageConfigurationBuilder supplier(Supplier<? extends LocalConfigurationStorage> configurationStorageSupplier) {
      attributes.attribute(CONFIGURATION_STORAGE_SUPPLIER).set(configurationStorageSupplier);
      return this;
   }

   public GlobalStorageConfigurationBuilder configurationStorage(ConfigurationStorage configurationStorage) {
      storage = configurationStorage;
      return this;
   }

   @Override
   public void validate() {
      if (storage.equals(ConfigurationStorage.CUSTOM) && attributes.attribute(CONFIGURATION_STORAGE_SUPPLIER).isNull()) {
         throw log.customStorageStrategyNotSet();
      }
   }

   @Override
   public GlobalStorageConfiguration create() {
      return new GlobalStorageConfiguration(attributes.protect(), storage);
   }

   @Override
   public GlobalStorageConfigurationBuilder read(GlobalStorageConfiguration template) {
      attributes.read(template.attributes());
      return this;
   }
}
