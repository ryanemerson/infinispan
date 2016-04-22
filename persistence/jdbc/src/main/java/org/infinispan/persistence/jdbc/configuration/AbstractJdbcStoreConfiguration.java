package org.infinispan.persistence.jdbc.configuration;

import org.infinispan.commons.configuration.attributes.Attribute;
import org.infinispan.commons.configuration.attributes.AttributeDefinition;
import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.infinispan.configuration.cache.AbstractStoreConfiguration;
import org.infinispan.configuration.cache.AsyncStoreConfiguration;
import org.infinispan.configuration.cache.SingletonStoreConfiguration;
import org.infinispan.persistence.jdbc.DatabaseType;

public abstract class AbstractJdbcStoreConfiguration extends AbstractStoreConfiguration {
   static final AttributeDefinition<Boolean> MANAGE_CONNECTION_FACTORY = AttributeDefinition.builder("manageConnectionFactory", true).immutable().build();
   static final AttributeDefinition<DatabaseType> DIALECT = AttributeDefinition.builder("databaseType", null, DatabaseType.class).immutable().build();
   static final AttributeDefinition<Boolean> DB_DISABLE_UPSERT = AttributeDefinition.builder("databaseDisableUpsert", false).immutable().build();
   static final AttributeDefinition<Integer> DB_MAJOR_VERSION = AttributeDefinition.builder("databaseMajorVersion", null, Integer.class).immutable().build();
   static final AttributeDefinition<Integer> DB_MINOR_VERSION = AttributeDefinition.builder("databaseMinorVersion", null, Integer.class).immutable().build();

   public static AttributeSet attributeDefinitionSet() {
      return new AttributeSet(AbstractJdbcStoreConfiguration.class, AbstractStoreConfiguration.attributeDefinitionSet(),
                              MANAGE_CONNECTION_FACTORY, DIALECT, DB_DISABLE_UPSERT, DB_MAJOR_VERSION, DB_MINOR_VERSION);
   }


   private final Attribute<Boolean> manageConnectionFactory;
   private final Attribute<DatabaseType> dialect;
   private final Attribute<Boolean> dbDisableUpsert;
   private final Attribute<Integer> dbMajorVersion;
   private final Attribute<Integer> dbMinorVersion;
   private final ConnectionFactoryConfiguration connectionFactory;

   protected AbstractJdbcStoreConfiguration(AttributeSet attributes, AsyncStoreConfiguration async, SingletonStoreConfiguration singletonStore,
                                            ConnectionFactoryConfiguration connectionFactory) {
      super(attributes, async, singletonStore);
      this.connectionFactory = connectionFactory;
      manageConnectionFactory = attributes.attribute(MANAGE_CONNECTION_FACTORY);
      dialect = attributes.attribute(DIALECT);
      dbDisableUpsert = attributes.attribute(DB_DISABLE_UPSERT);
      dbMajorVersion = attributes.attribute(DB_MAJOR_VERSION);
      dbMinorVersion = attributes.attribute(DB_MINOR_VERSION);
   }

   public ConnectionFactoryConfiguration connectionFactory() {
      return connectionFactory;
   }

   public boolean manageConnectionFactory() {
      return manageConnectionFactory.get();
   }

   public DatabaseType dialect() {
      return dialect.get();
   }

   public boolean dbDisableUpsert() {
      return dbDisableUpsert.get();
   }

   public Integer dbMajorVersion() {
      return dbMajorVersion.get();
   }

   public Integer dbMinorVersion() {
      return dbMinorVersion.get();
   }

   @Override
   public String toString() {
      return "AbstractJdbcStoreConfiguration [connectionFactory=" + connectionFactory + ", attributes=" + attributes + "]";
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + ((connectionFactory == null) ? 0 : connectionFactory.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (!super.equals(obj))
         return false;
      if (getClass() != obj.getClass())
         return false;
      AbstractJdbcStoreConfiguration other = (AbstractJdbcStoreConfiguration) obj;
      if (connectionFactory == null) {
         if (other.connectionFactory != null)
            return false;
      } else if (!connectionFactory.equals(other.connectionFactory))
         return false;
      return true;
   }

}
