package org.infinispan.server.datasources.services.datasource;

import java.util.HashMap;

public class DataSourceMeta {

   private final HashMap<String, String> connectionProperties;
   private final String connectionUrl;
   private final String driverName;
   private final String dataSourceClass;
   private final String driverClass;
   private final String jndiName;
   private final String poolName;
   private final String username;
   private final String password;

   public DataSourceMeta(String connectionUrl, String driverName, String dataSourceClass, String driverClass,
                         String jndiName, String poolName, String username, String password) {
      this.connectionProperties = new HashMap<>();
      this.connectionUrl = connectionUrl;
      this.driverName = driverName;
      this.dataSourceClass = dataSourceClass;
      this.driverClass = driverClass;
      this.jndiName = jndiName;
      this.poolName = poolName;
      this.username = username;
      this.password = password;
   }

   public String getConnectionUrl() {
      return connectionUrl;
   }

   public HashMap<String, String> getConnectionProperties() {
      return connectionProperties;
   }

   public String getDriverName() {
      return driverName;
   }

   public String getDataSourceClass() {
      return dataSourceClass;
   }

   public String getDriverClass() {
      return driverClass;
   }

   public String getJndiName() {
      return jndiName;
   }

   public String getPoolName() {
      return poolName;
   }

   public String getUsername() {
      return username;
   }

   public String getPassword() {
      return password;
   }
}
