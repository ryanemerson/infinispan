package org.infinispan.tools.jdbc.migrator;

import static org.infinispan.tools.jdbc.migrator.Element.BINARY;
import static org.infinispan.tools.jdbc.migrator.Element.CACHE_NAME;
import static org.infinispan.tools.jdbc.migrator.Element.CONNECTION_POOL;
import static org.infinispan.tools.jdbc.migrator.Element.CONNECTION_URL;
import static org.infinispan.tools.jdbc.migrator.Element.DATA;
import static org.infinispan.tools.jdbc.migrator.Element.DB;
import static org.infinispan.tools.jdbc.migrator.Element.DIALECT;
import static org.infinispan.tools.jdbc.migrator.Element.DISABLE_INDEXING;
import static org.infinispan.tools.jdbc.migrator.Element.DISABLE_UPSERT;
import static org.infinispan.tools.jdbc.migrator.Element.DRIVER_CLASS;
import static org.infinispan.tools.jdbc.migrator.Element.ID;
import static org.infinispan.tools.jdbc.migrator.Element.MAJOR_VERSION;
import static org.infinispan.tools.jdbc.migrator.Element.MINOR_VERSION;
import static org.infinispan.tools.jdbc.migrator.Element.NAME;
import static org.infinispan.tools.jdbc.migrator.Element.PASSWORD;
import static org.infinispan.tools.jdbc.migrator.Element.SOURCE;
import static org.infinispan.tools.jdbc.migrator.Element.STRING;
import static org.infinispan.tools.jdbc.migrator.Element.TABLE;
import static org.infinispan.tools.jdbc.migrator.Element.TABLE_NAME_PREFIX;
import static org.infinispan.tools.jdbc.migrator.Element.TARGET;
import static org.infinispan.tools.jdbc.migrator.Element.TIMESTAMP;
import static org.infinispan.tools.jdbc.migrator.Element.TYPE;
import static org.infinispan.tools.jdbc.migrator.Element.USERNAME;

import java.util.Properties;

import org.infinispan.commons.CacheConfigurationException;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.persistence.jdbc.DatabaseType;
import org.infinispan.persistence.jdbc.configuration.ConnectionFactoryConfiguration;
import org.infinispan.persistence.jdbc.configuration.JdbcStringBasedStoreConfigurationBuilder;
import org.infinispan.persistence.jdbc.configuration.PooledConnectionFactoryConfiguration;
import org.infinispan.persistence.jdbc.configuration.TableManipulationConfiguration;
import org.infinispan.persistence.jdbc.table.management.DbMetaData;
import org.infinispan.transaction.TransactionMode;
import org.infinispan.transaction.lookup.DummyTransactionManagerLookup;

/**
 * @author Ryan Emerson
 * @since 9.0
 */
class MigratorConfiguration {
   final String cacheName;
   final StoreType storeType;
   private final Properties props;
   private final boolean sourceStore;
   private final Element orientation;
   private DbMetaData dbMetaData;
   private TableManipulationConfiguration stringTable = null;
   private TableManipulationConfiguration binaryTable = null;
   private ConnectionFactoryConfiguration connectionConfig;
   private JdbcStringBasedStoreConfigurationBuilder jdbcConfigBuilder;

   MigratorConfiguration(boolean sourceStore, Properties props) {
      this.props = props;
      this.sourceStore = sourceStore;
      this.orientation = sourceStore ? SOURCE : TARGET;
      this.cacheName = property(orientation, CACHE_NAME);
      this.storeType = StoreType.valueOf(property(orientation, TYPE).toUpperCase());
      initStoreConfig();
   }

   private void initStoreConfig() {
      if (cacheName == null) {
         String msg = String.format("The cache name property must be specified for the %1$s store. e.g. '%1$s.%2$s=some_cache'",
               orientation, CACHE_NAME.toString());
         throw new CacheConfigurationException(msg);
      }

      JdbcStringBasedStoreConfigurationBuilder builder = new ConfigurationBuilder().persistence()
            .addStore(JdbcStringBasedStoreConfigurationBuilder.class);

      dbMetaData = createDbMeta();
      connectionConfig = createConnectionConfig(builder);
      if (sourceStore) {
         if (storeType == StoreType.MIXED || storeType == StoreType.STRING)
            stringTable = createTableConfig(STRING, builder);

         if (storeType == StoreType.MIXED || storeType == StoreType.BINARY)
            binaryTable = createTableConfig(BINARY, builder);
      } else {
         stringTable = createTableConfig(STRING, builder);
         builder.transaction()
               .transactionMode(TransactionMode.TRANSACTIONAL)
               .transactionManagerLookup(new DummyTransactionManagerLookup());
      }
      builder.validate();
      jdbcConfigBuilder = builder;
   }

   private DbMetaData createDbMeta() {
      String prop;
      DatabaseType type = DatabaseType.valueOf(property(orientation, DIALECT).toUpperCase());
      int major = (prop = property(DB, MAJOR_VERSION)) != null ? new Integer(prop) : -1;
      int minor = (prop = property(DB, MINOR_VERSION)) != null ? new Integer(prop) : -1;
      boolean upsert = Boolean.parseBoolean(property(DB, DISABLE_UPSERT));
      boolean indexing = Boolean.parseBoolean(property(DB, DISABLE_INDEXING));
      return new DbMetaData(type, major, minor, upsert, indexing);
   }

   private TableManipulationConfiguration createTableConfig(Element tableType, JdbcStringBasedStoreConfigurationBuilder storeBuilder) {
      boolean createOnStart = orientation == SOURCE;
      return storeBuilder.table()
            .createOnStart(createOnStart)
            .tableNamePrefix(property(orientation, TABLE, tableType, TABLE_NAME_PREFIX))
            .idColumnName(property(orientation, TABLE, tableType, ID, NAME))
            .idColumnType(property(orientation, TABLE, tableType, ID, TYPE))
            .dataColumnName(property(orientation, TABLE, tableType, DATA, NAME))
            .dataColumnType(property(orientation, TABLE, tableType, DATA, TYPE))
            .timestampColumnName(property(orientation, TABLE, tableType, TIMESTAMP, NAME))
            .timestampColumnType(property(orientation, TABLE, tableType, TIMESTAMP, TYPE))
            .create();
   }

   private PooledConnectionFactoryConfiguration createConnectionConfig(JdbcStringBasedStoreConfigurationBuilder storeBuilder) {
      return storeBuilder.connectionPool()
            .connectionUrl(property(orientation, CONNECTION_POOL, CONNECTION_URL))
            .driverClass(property(orientation, CONNECTION_POOL, DRIVER_CLASS))
            .username(property(orientation, CONNECTION_POOL, USERNAME))
            .password(property(orientation, CONNECTION_POOL, PASSWORD))
            .create();
   }

   ConnectionFactoryConfiguration getConnectionConfig() {
      return connectionConfig;
   }

   DbMetaData getDbMeta() {
      return dbMetaData;
   }

   TableManipulationConfiguration getStringTable() {
      return stringTable;
   }

   TableManipulationConfiguration getBinaryTable() {
      return binaryTable;
   }

   JdbcStringBasedStoreConfigurationBuilder getJdbcConfigBuilder() {
      return jdbcConfigBuilder;
   }

   private String property(Element... elements) {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < elements.length; i++) {
         sb.append(elements[i].toString());
         if (i != elements.length - 1) sb.append(".");
      }
      return props.getProperty(sb.toString());
   }
}
