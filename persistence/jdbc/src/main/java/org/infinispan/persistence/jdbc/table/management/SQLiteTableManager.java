package org.infinispan.persistence.jdbc.table.management;

import org.infinispan.persistence.jdbc.configuration.TableManagerConfiguration;
import org.infinispan.persistence.jdbc.connectionfactory.ConnectionFactory;
import org.infinispan.persistence.jdbc.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * @author Ryan Emerson
 */
public class SQLiteTableManager extends AbstractTableManager {

   private static final Log LOG = LogFactory.getLog(SQLiteTableManager.class, Log.class);

   public SQLiteTableManager(ConnectionFactory connectionFactory, TableManagerConfiguration config, DbMetaData metaData) {
      super(connectionFactory, config, metaData, LOG);
   }

   @Override
   public boolean isUpsertSupported() {
      // OR/ON CONFLICT introduced in 3.8.11
      return super.isUpsertSupported() && metaData.getMajorVersion() > 2 && metaData.getMinorVersion() > 7;
   }

   @Override
   public String getUpsertRowSql() {
      if (upsertRowSql == null) {
         upsertRowSql = String.format("INSERT OR REPLACE INTO %s (%s, %s, %s) VALUES (?, ?, ?)",
                                      getTableName(), config.dataColumnName(), config.timestampColumnName(),
                                      config.idColumnName());
      }
      return upsertRowSql;
   }
}
