package org.infinispan.persistence.jdbc.connectionfactory;

import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.mchange.v2.c3p0.DataSources;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

import org.infinispan.commons.util.FileLookup;
import org.infinispan.commons.util.FileLookupFactory;
import org.infinispan.persistence.jdbc.JdbcUtil;
import org.infinispan.persistence.jdbc.configuration.ConnectionFactoryConfiguration;
import org.infinispan.persistence.jdbc.configuration.PooledConnectionFactoryConfiguration;
import org.infinispan.persistence.jdbc.logging.Log;
import org.infinispan.persistence.spi.PersistenceException;
import org.infinispan.util.logging.LogFactory;

/**
 * Pooled connection factory that uses HikariCP by default. If no Hikari system properties or property files exist on the
 * classpath and a C3P0 properties file is found then we utilise the C3P0 connection pool.  Note, that support for the
 * C3PO pool has been deprecated and will be removed in future versions.
 *
 * HikariCP property files can be specified by either passing the <tt>hikaricp.configurationFile</tt> system property with
 * the path to a properties file, or by ensuring that a <tt>hikari.properties</tt> file is on the classpath. Note, that
 * the <tt>hikaricp.configurationFile</tt> takes precedence over <tt>hikari.properties</tt>.
 *
 * For a complete configuration reference for C3P0 look <a href="http://www.mchange.com/projects/c3p0/index.html#configuration">here</a>.
 * The connection pool can be configured n various ways, as described
 * <a href="http://www.mchange.com/projects/c3p0/index.html#configuration_files">here</a>. The simplest way is by having
 * an <tt>c3p0.properties</tt> file in the classpath.
 *
 * If no properties files are found for either HikariCP or C3PO then the default values of these connection pools are
 * utilised.
 *
 * @author Mircea.Markus@jboss.com
 * @author Tristan Tarrant
 * @author Ryan Emerson
 */
public class PooledConnectionFactory extends ConnectionFactory {

   private static final Log LOG = LogFactory.getLog(PooledConnectionFactory.class, Log.class);
   private static final boolean TRACE = LOG.isTraceEnabled();
   private static final String FORCE_C3P0 = "c3p0.force";
   private static final String HIKARI_CONFIG = "hikaricp.configurationFile";
   private static final String HIKARI_PROPERTIES = "hikari.properties";
   private static final String C3P0_PROPERTIES = "c3p0.properties";
   private static final String C3P0_CONFIG = "c3p0-config.xml";

   private HikariDataSource hikari;
   private ComboPooledDataSource c3p0;
   private HikariPoolMXBean hikariMxBean;
   private boolean hikariPoolEnabled = true;

   @Override
   public void start(ConnectionFactoryConfiguration config, ClassLoader classLoader) throws PersistenceException {
      PooledConnectionFactoryConfiguration pooledConfiguration;
      if (config instanceof PooledConnectionFactoryConfiguration) {
         pooledConfiguration = (PooledConnectionFactoryConfiguration) config;
      } else {
         throw new PersistenceException("ConnectionFactoryConfiguration passed in must be an instance of " +
                                              "PooledConnectionFactoryConfiguration");
      }

      initPool(classLoader, pooledConfiguration);
      if (!hikariPoolEnabled) LOG.warn("The c3p0 connection factory has been deprecated and will be removed in future releases");
      if (TRACE) LOG.tracef("Started connection factory with config: %s", config);
   }

   private void initPool(ClassLoader classLoader, PooledConnectionFactoryConfiguration poolConfig) {
      FileLookup fileLookup = FileLookupFactory.newInstance();

      String hikariPropPath = null;
      boolean forceC3P0 = Boolean.parseBoolean(System.getProperty(FORCE_C3P0));
      if (!forceC3P0) {
         hikariPropPath = System.getProperty(HIKARI_CONFIG);
         if (hikariPropPath == null) {
            URL hikariUrl = fileLookup.lookupFileLocation(HIKARI_PROPERTIES, classLoader);
            hikariPropPath = hikariUrl != null ? hikariUrl.getPath() : null;
         }
      }

      if (forceC3P0 || hikariPropPath == null) {
         URL c3p0Props = fileLookup.lookupFileLocation(C3P0_PROPERTIES, classLoader);
         URL c3p0Xml = fileLookup.lookupFileLocation(C3P0_CONFIG, classLoader);
         if (LOG.isDebugEnabled()) {
            if (c3p0Props != null)
               LOG.debugf("Found '%s' in classpath: %s", C3P0_PROPERTIES, c3p0Props);
            if (c3p0Xml != null)
               LOG.debugf("Found '%s' in classpath: %s", C3P0_CONFIG, c3p0Xml);
         }

         // If C3PO is not forced and no C3P0 properties are found, then do nothing so HikariCP is initialised
         if (forceC3P0 || c3p0Props != null || c3p0Xml != null) {
            hikariPoolEnabled = false;
            initC3P0DataSource(poolConfig);
            return;
         }
      }
      initHikariDataSource(hikariPropPath, poolConfig);
   }

   private void initHikariDataSource(String propertiesPath, PooledConnectionFactoryConfiguration poolConfig) {
      try {
         HikariConfig hikariConfig = propertiesPath != null ? new HikariConfig(propertiesPath) : new HikariConfig();
         hikariConfig.setRegisterMbeans(true);

         // Set Hikari properties from poolConfig only if there are no existing values specified in the properties file
         if (hikariConfig.getDataSourceClassName() == null && hikariConfig.getJdbcUrl() == null)
            hikariConfig.setJdbcUrl(poolConfig.connectionUrl());
         if (hikariConfig.getUsername() == null)
            hikariConfig.setUsername(poolConfig.username());
         if (hikariConfig.getPassword() == null)
            hikariConfig.setPassword(poolConfig.password());
         if (hikariConfig.getDriverClassName() == null)
            hikariConfig.setDriverClassName(poolConfig.driverClass());

         hikari = new HikariDataSource(hikariConfig);
      } catch (Exception e) {
         // Exception should only be thrown when trying to setDriverClassName either explicitly or as part of HikariConfig
         LOG.errorInstantiatingJdbcDriver(poolConfig.driverClass(), e);
         throw new PersistenceException(String.format(
               "Error while instantiating JDBC driver: '%s'", poolConfig.driverClass()), e);
      }

      try {
         MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
         ObjectName poolName = new ObjectName("com.zaxxer.hikari:type=Pool (" + hikari.getPoolName() + ")");
         hikariMxBean = JMX.newMXBeanProxy(mBeanServer, poolName, HikariPoolMXBean.class);
      } catch (MalformedObjectNameException ignore) {
         // Ignore as this should never happen
      }
   }

   private void initC3P0DataSource(PooledConnectionFactoryConfiguration pooledConfiguration) {
      c3p0 = new ComboPooledDataSource();
      c3p0.setProperties(new Properties());
      try {
         /* Since c3p0 does not throw an exception when it fails to load a driver we attempt to do so here
          * Also, c3p0 does not allow specifying a custom classloader, so use c3p0's
          */
         Class.forName(pooledConfiguration.driverClass(), true, ComboPooledDataSource.class.getClassLoader());
         c3p0.setDriverClass(pooledConfiguration.driverClass()); //loads the jdbc driver
      } catch (Exception e) {
         LOG.errorInstantiatingJdbcDriver(pooledConfiguration.driverClass(), e);
         throw new PersistenceException(String.format(
               "Error while instatianting JDBC driver: '%s'", pooledConfiguration.driverClass()), e);
      }
      c3p0.setJdbcUrl(pooledConfiguration.connectionUrl());
      c3p0.setUser(pooledConfiguration.username());
      c3p0.setPassword(pooledConfiguration.password());
   }

   @Override
   public void stop() {
      if (hikariPoolEnabled) {
         if (hikari != null)
            hikari.close();
      } else {
         try {
            DataSources.destroy(c3p0);
         } catch (SQLException e) {
            LOG.couldNotDestroyC3p0ConnectionPool(c3p0 != null ? c3p0.toString() : null, e);
            return;
         }
      }
      if (LOG.isDebugEnabled()) LOG.debug("Successfully stopped PooledConnectionFactory.");
   }

   @Override
   public Connection getConnection() throws PersistenceException {
      try {
         logBefore(true);
         Connection connection = hikariPoolEnabled ? hikari.getConnection() : c3p0.getConnection();
         logAfter(connection, true);
         return connection;
      } catch (SQLException e) {
         throw new PersistenceException("Failed obtaining connection from PooledDataSource", e);
      }
   }

   @Override
   public void releaseConnection(Connection conn) {
      logBefore(false);
      JdbcUtil.safeClose(conn);
      logAfter(conn, false);
   }

   public int getMaxPoolSize() {
      return hikariPoolEnabled ? hikari.getMaximumPoolSize() : c3p0.getMaxPoolSize();
   }

   public int getNumConnectionsAllUsers() throws SQLException {
      return hikariPoolEnabled ? hikariMxBean.getTotalConnections() : c3p0.getNumConnectionsAllUsers();
   }

   public int getNumBusyConnectionsAllUsers() throws SQLException {
      return hikariPoolEnabled ? hikariMxBean.getActiveConnections() : c3p0.getNumBusyConnectionsAllUsers();
   }

   private void logBefore(boolean checkout) {
      log(null, checkout, true);
   }

   private void logAfter(Connection connection, boolean checkout) {
      log(connection, checkout, false);
   }

   private void log(Connection connection, boolean checkout, boolean before)  {
      if (TRACE) {
         String stage = before ? "before" : "after";
         String operation = checkout ? "checkout" : "release";
         try {
            LOG.tracef("DataSource %s %s (NumBusyConnectionsAllUsers) : %d, (NumConnectionsAllUsers) : %d",
                      stage, operation, getNumBusyConnectionsAllUsers(), getNumConnectionsAllUsers());
         } catch (SQLException e) {
            LOG.sqlFailureUnexpected(e);
         }

         if (connection != null)
            LOG.tracef("Connection %s : %s", operation, connection);
      }
   }
}
