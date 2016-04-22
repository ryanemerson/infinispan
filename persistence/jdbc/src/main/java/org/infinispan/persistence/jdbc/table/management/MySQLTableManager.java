package org.infinispan.persistence.jdbc.table.management;

import org.infinispan.persistence.jdbc.configuration.TableManagerConfiguration;
import org.infinispan.persistence.jdbc.connectionfactory.ConnectionFactory;
import org.infinispan.persistence.jdbc.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * @author Ryan Emerson
 */
public class MySQLTableManager extends AbstractTableManager {
   private static final Log LOG = LogFactory.getLog(MySQLTableManager.class, Log.class);

   public MySQLTableManager(ConnectionFactory connectionFactory, TableManagerConfiguration config, DbMetaData metaData) {
      super(connectionFactory, config, metaData, LOG);
      identifierQuoteString = "`";
   }

   @Override
   public int getFetchSize() {
      return Integer.MIN_VALUE;
   }

   @Override
   public boolean isUpsertSupported() {
      return true;
   }

   @Override
   public String getUpsertRowSql() {
      if (upsertRowSql == null) {
         // Assumes that config.idColumnName is the primary key
         upsertRowSql = String.format("%s ON DUPLICATE KEY UPDATE %s = ?, %s = ?", getInsertRowSql(),
                                      config.dataColumnName(), config.timestampColumnName());
      }
      return upsertRowSql;
   }
}
