package org.infinispan.persistence.rest.configuration;

import org.infinispan.commons.configuration.BuiltBy;
import org.infinispan.commons.configuration.ConfigurationFor;
import org.infinispan.commons.configuration.attributes.Attribute;
import org.infinispan.commons.configuration.attributes.AttributeDefinition;
import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.infinispan.configuration.serializing.SerializedWith;

/**
 * ConnectionPoolConfiguration.
 *
 * @author Tristan Tarrant
 * @since 6.0
 */
@BuiltBy(ConnectionPoolConfigurationBuilder.class)
public class ConnectionPoolConfiguration {
   static final AttributeDefinition<Integer> CONNECTION_TIMEOUT = AttributeDefinition.builder("connectionTimeout", 60000).immutable().build();
   static final AttributeDefinition<Integer> MAX_CONNECTIONS_PER_HOST = AttributeDefinition.builder("maxConnectionsPerHostTimeout", 4).immutable().build();
   static final AttributeDefinition<Integer> MAX_TOTAL_CONNECTIONS = AttributeDefinition.builder("maxTotalConnections", 20).immutable().build();
   static final AttributeDefinition<Integer> BUFFER_SIZE = AttributeDefinition.builder("bufferSize", 8192).immutable().build();
   static final AttributeDefinition<Integer> SOCKET_TIMEOUT = AttributeDefinition.builder("socketTimeout", 60000).immutable().build();
   static final AttributeDefinition<Boolean> TCP_NO_DELAY = AttributeDefinition.builder("tcpNoDelay", true).immutable().build();

   static AttributeSet attributeDefinitionSet() {
      return new AttributeSet(ConnectionPoolConfiguration.class, CONNECTION_TIMEOUT, MAX_CONNECTIONS_PER_HOST,
            MAX_TOTAL_CONNECTIONS, BUFFER_SIZE, SOCKET_TIMEOUT, TCP_NO_DELAY);
   }

   private final AttributeSet attributes;
   private final Attribute<Integer> connectionTimeout;
   private final Attribute<Integer> maxConnectionsPerHost;
   private final Attribute<Integer> maxTotalConnections;
   private final Attribute<Integer> bufferSize;
   private final Attribute<Integer> socketTimeout;
   private final Attribute<Boolean> tcpNoDelay;

   public ConnectionPoolConfiguration(AttributeSet attributes) {
      this.attributes = attributes;
      this.connectionTimeout = attributes.attribute(CONNECTION_TIMEOUT);
      this.maxConnectionsPerHost = attributes.attribute(MAX_CONNECTIONS_PER_HOST);
      this.maxTotalConnections = attributes.attribute(MAX_TOTAL_CONNECTIONS);
      this.bufferSize = attributes.attribute(BUFFER_SIZE);
      this.socketTimeout = attributes.attribute(SOCKET_TIMEOUT);
      this.tcpNoDelay = attributes.attribute(TCP_NO_DELAY);
   }

   public int connectionTimeout() {
      return connectionTimeout.get();
   }

   public int maxConnectionsPerHost() {
      return maxConnectionsPerHost.get();
   }

   public int maxTotalConnections() {
      return maxTotalConnections.get();
   }

   public int bufferSize() {
      return bufferSize.get();
   }

   public int socketTimeout() {
      return socketTimeout.get();
   }

   public boolean tcpNoDelay() {
      return tcpNoDelay.get();
   }

   @Override
   public String toString() {
      return "ConnectionPoolConfiguration [connectionTimeout=" + connectionTimeout + ", maxConnectionsPerHost=" + maxConnectionsPerHost + ", maxTotalConnections="
            + maxTotalConnections + ", bufferSize=" + bufferSize + ", socketTimeout=" + socketTimeout + ", tcpNoDelay="
            + tcpNoDelay + "]";
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      ConnectionPoolConfiguration that = (ConnectionPoolConfiguration) o;

      if (connectionTimeout() != that.connectionTimeout()) return false;
      if (maxConnectionsPerHost() != that.maxConnectionsPerHost()) return false;
      if (maxTotalConnections() != that.maxTotalConnections()) return false;
      if (bufferSize() != that.bufferSize()) return false;
      if (socketTimeout() != that.socketTimeout()) return false;
      return tcpNoDelay() == that.tcpNoDelay();

   }

   @Override
   public int hashCode() {
      int result = connectionTimeout();
      result = 31 * result + maxConnectionsPerHost();
      result = 31 * result + maxTotalConnections();
      result = 31 * result + bufferSize();
      result = 31 * result + socketTimeout();
      result = 31 * result + (tcpNoDelay() ? 1 : 0);
      return result;
   }
}
