package org.infinispan.server.datasources.services.datasource;

import static org.infinispan.server.datasources.DatasourcesLogger.ROOT_LOGGER;

import javax.sql.DataSource;

import org.infinispan.server.datasources.services.driver.registry.DriverRegistry;
import org.infinispan.server.datasources.subsystem.ModelKeys;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import com.zaxxer.hikari.HikariDataSource;

/**
 * @author Ryan Emerson
 * @since 9.2
 */
public class DataSourceService implements Service<DataSource> {

   private static final ServiceName SERVICE_NAME_BASE = ServiceName.JBOSS.append(ModelKeys.DATA_SOURCE);

   public static ServiceName getServiceName(ContextNames.BindInfo bindInfo) {
      return SERVICE_NAME_BASE.append(bindInfo.getBinderServiceName().getCanonicalName());
   }

   private final InjectedValue<DriverRegistry> driverRegistry = new InjectedValue<>();

   private final DataSourceMeta dataSourceMetaData;
   private volatile HikariDataSource dataSource;

   public DataSourceService(DataSourceMeta dataSourceMetaData) {
      assert dataSourceMetaData != null : ROOT_LOGGER.nullVar("dataSourceMetaData");
      this.dataSourceMetaData = dataSourceMetaData;
   }

   @Override
   public void start(StartContext context) throws StartException {
      // TODO create HikariDataSource using metadata
      dataSource = new HikariDataSource();
      ROOT_LOGGER.startedDataSourceService(dataSourceMetaData.getPoolName());
   }

   @Override
   public void stop(StopContext context) {
      if (dataSource != null) {
         dataSource.close();
         ROOT_LOGGER.stoppedDataSourceService(dataSourceMetaData.getPoolName());
      }
   }

   @Override
   public DataSource getValue() throws IllegalStateException, IllegalArgumentException {
      return dataSource;
   }

   public Injector<DriverRegistry> getDriverRegistryInjector() {
      return driverRegistry;
   }
}
