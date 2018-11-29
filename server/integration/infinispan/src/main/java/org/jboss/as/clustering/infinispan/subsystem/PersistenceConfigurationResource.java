package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.configuration.cache.PersistenceConfiguration;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Resource description for the addressable resource /subsystem=infinispan/cache-container=X/cache=Y/persistence=PERSISTENCE
 *
 * @author Ryan Emerson
 */
public class PersistenceConfigurationResource extends CacheConfigurationChildResource {

   static final PathElement PATH = PathElement.pathElement(ModelKeys.PERSISTENCE, ModelKeys.PERSISTENCE_NAME);

   // attributes
   static final SimpleAttributeDefinition AVAILABILITY_INTERVAL =
         new SimpleAttributeDefinitionBuilder(ModelKeys.AVAILABILITY_INTERVAL, ModelType.INT, true)
               .setXmlName(Attribute.AVAILABILITY_INTERVAL.getLocalName())
               .setAllowExpression(false)
               .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
               .setDefaultValue(new ModelNode().set(PersistenceConfiguration.AVAILABILITY_INTERVAL.getDefaultValue()))
               .build();

   static final SimpleAttributeDefinition CONNECTION_ATTEMPTS =
         new SimpleAttributeDefinitionBuilder(ModelKeys.CONNECTION_ATTEMPTS, ModelType.INT, true)
               .setXmlName(Attribute.CONNECTION_ATTEMPTS.getLocalName())
               .setAllowExpression(false)
               .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
               .setDefaultValue(new ModelNode().set(PersistenceConfiguration.CONNECTION_ATTEMPTS.getDefaultValue()))
               .build();

   static final SimpleAttributeDefinition CONNECTION_INTERVAL =
         new SimpleAttributeDefinitionBuilder(ModelKeys.CONNECTION_INTERVAL, ModelType.INT, true)
               .setXmlName(Attribute.CONNECTION_INTERVAL.getLocalName())
               .setAllowExpression(false)
               .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
               .setDefaultValue(new ModelNode().set(PersistenceConfiguration.CONNECTION_INTERVAL.getDefaultValue()))
               .build();

   static final SimpleAttributeDefinition PASSIVATION =
         new SimpleAttributeDefinitionBuilder(ModelKeys.PASSIVATION, ModelType.BOOLEAN, true)
               .setXmlName(Attribute.PASSIVATION.getLocalName())
               .setAllowExpression(true)
               .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
               .setDefaultValue(new ModelNode().set(PersistenceConfiguration.PASSIVATION.getDefaultValue()))
               .build();

   static final AttributeDefinition[] ATTRIBUTES = {AVAILABILITY_INTERVAL, CONNECTION_ATTEMPTS, CONNECTION_INTERVAL, PASSIVATION};

   private CacheConfigurationResource cacheConfigResource;

   PersistenceConfigurationResource(CacheConfigurationResource parent) {
      super(PATH, ModelKeys.PERSISTENCE, parent, ATTRIBUTES);
      this.cacheConfigResource = parent;
   }

   @Override
   public void registerChildren(ManagementResourceRegistration resourceRegistration) {
      super.registerChildren(resourceRegistration);

      resourceRegistration.registerSubModel(new FileStoreResource(cacheConfigResource));
      resourceRegistration.registerSubModel(new LoaderConfigurationResource(cacheConfigResource));
      resourceRegistration.registerSubModel(new ClusterLoaderConfigurationResource(cacheConfigResource));
      resourceRegistration.registerSubModel(new RocksDBStoreConfigurationResource(cacheConfigResource));
      resourceRegistration.registerSubModel(new RemoteStoreConfigurationResource(cacheConfigResource));
      resourceRegistration.registerSubModel(new RestStoreConfigurationResource(cacheConfigResource));
      resourceRegistration.registerSubModel(new StoreConfigurationResource(cacheConfigResource));
      resourceRegistration.registerSubModel(new StringKeyedJDBCStoreResource(cacheConfigResource));
   }
}
