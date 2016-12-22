package org.infinispan.tools.jdbc.migrator;

/**
 * @author Ryan Emerson
 * @since 9.0
 */
enum Element {

   CACHE_NAME("cache_name"),
   CONNECTION_URL("connection_url"),
   CONNECTION_POOL("connection_pool"),
   DATA("data"),
   DIALECT("dialect"),
   DRIVER_CLASS("driver_class"),
   ID("id"),
   MARSHALLER("marshaller"),
   NAME("name"),
   PASSWORD("password"),
   SOURCE("source"),
   TARGET("target"),
   TABLE("table"),
   TABLE_NAME_PREFIX("table_name_prefix"),
   TIMESTAMP("timestamp"),
   TYPE("type"),
   USERNAME("username");

   private final String name;

   Element(final String name) {
      this.name = name;
   }

   @Override
   public String toString() {
      return name;
   }
}
