package org.infinispan.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivilegedActionException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.naming.InitialContext;
import javax.naming.spi.NamingManager;
import javax.net.ssl.SSLContext;
import javax.sql.DataSource;

import org.apache.logging.log4j.LogManager;
import org.infinispan.commons.CacheConfigurationException;
import org.infinispan.commons.CacheException;
import org.infinispan.commons.configuration.ConfigurationFor;
import org.infinispan.commons.configuration.io.ConfigurationWriter;
import org.infinispan.commons.io.StringBuilderWriter;
import org.infinispan.commons.jdkspecific.ProcessInfo;
import org.infinispan.commons.marshall.SerializeWith;
import org.infinispan.commons.time.DefaultTimeService;
import org.infinispan.commons.time.TimeService;
import org.infinispan.commons.util.OS;
import org.infinispan.commons.util.Util;
import org.infinispan.commons.util.Version;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ParserRegistry;
import org.infinispan.factories.KnownComponentNames;
import org.infinispan.factories.impl.BasicComponentRegistry;
import org.infinispan.globalstate.GlobalConfigurationManager;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.manager.ClusterExecutor;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.registry.InternalCacheRegistry;
import org.infinispan.remoting.transport.Address;
import org.infinispan.remoting.transport.jgroups.JGroupsTransport;
import org.infinispan.remoting.transport.jgroups.NamedSocketFactory;
import org.infinispan.rest.RestServer;
import org.infinispan.rest.authentication.Authenticator;
import org.infinispan.rest.configuration.RestServerConfiguration;
import org.infinispan.security.AuthorizationPermission;
import org.infinispan.security.Security;
import org.infinispan.security.audit.LoggingAuditLogger;
import org.infinispan.server.configuration.DataSourceConfiguration;
import org.infinispan.server.configuration.ServerConfiguration;
import org.infinispan.server.configuration.ServerConfigurationBuilder;
import org.infinispan.server.configuration.ServerConfigurationSerializer;
import org.infinispan.server.configuration.endpoint.EndpointConfiguration;
import org.infinispan.server.configuration.endpoint.EndpointConfigurationBuilder;
import org.infinispan.server.configuration.security.RealmConfiguration;
import org.infinispan.server.configuration.security.TokenRealmConfiguration;
import org.infinispan.server.configuration.security.TransportSecurityConfiguration;
import org.infinispan.server.context.ServerInitialContextFactoryBuilder;
import org.infinispan.server.core.BackupManager;
import org.infinispan.server.core.ProtocolServer;
import org.infinispan.server.core.RequestTracer;
import org.infinispan.server.core.ServerManagement;
import org.infinispan.server.core.ServerStateManager;
import org.infinispan.server.core.backup.BackupManagerImpl;
import org.infinispan.server.core.configuration.ProtocolServerConfiguration;
import org.infinispan.server.core.configuration.ProtocolServerConfigurationBuilder;
import org.infinispan.server.datasource.DataSourceFactory;
import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.server.hotrod.configuration.HotRodServerConfiguration;
import org.infinispan.server.logging.Log;
import org.infinispan.server.router.RoutingTable;
import org.infinispan.server.router.configuration.SinglePortRouterConfiguration;
import org.infinispan.server.router.router.impl.singleport.SinglePortEndpointRouter;
import org.infinispan.server.router.routes.Route;
import org.infinispan.server.router.routes.RouteDestination;
import org.infinispan.server.router.routes.RouteSource;
import org.infinispan.server.router.routes.hotrod.HotRodServerRouteDestination;
import org.infinispan.server.router.routes.rest.RestServerRouteDestination;
import org.infinispan.server.router.routes.singleport.SinglePortRouteSource;
import org.infinispan.server.security.ElytronHTTPAuthenticator;
import org.infinispan.server.security.ElytronSASLAuthenticationProvider;
import org.infinispan.server.state.ServerStateManagerImpl;
import org.infinispan.server.tasks.admin.ServerAdminOperationsHandler;
import org.infinispan.tasks.TaskManager;
import org.infinispan.util.concurrent.BlockingManager;
import org.infinispan.util.concurrent.CompletableFutures;
import org.infinispan.util.function.SerializableFunction;
import org.infinispan.util.logging.LogFactory;
import org.wildfly.security.http.basic.WildFlyElytronHttpBasicProvider;
import org.wildfly.security.http.bearer.WildFlyElytronHttpBearerProvider;
import org.wildfly.security.http.cert.WildFlyElytronHttpClientCertProvider;
import org.wildfly.security.http.digest.WildFlyElytronHttpDigestProvider;
import org.wildfly.security.http.spnego.WildFlyElytronHttpSpnegoProvider;
import org.wildfly.security.sasl.digest.WildFlyElytronSaslDigestProvider;
import org.wildfly.security.sasl.external.WildFlyElytronSaslExternalProvider;
import org.wildfly.security.sasl.gs2.WildFlyElytronSaslGs2Provider;
import org.wildfly.security.sasl.gssapi.WildFlyElytronSaslGssapiProvider;
import org.wildfly.security.sasl.localuser.WildFlyElytronSaslLocalUserProvider;
import org.wildfly.security.sasl.oauth2.WildFlyElytronSaslOAuth2Provider;
import org.wildfly.security.sasl.plain.WildFlyElytronSaslPlainProvider;
import org.wildfly.security.sasl.scram.WildFlyElytronSaslScramProvider;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 10.0
 */
public class Server implements ServerManagement, AutoCloseable {
   public static final Log log = LogFactory.getLog("SERVER", Log.class);

   // Properties
   public static final String INFINISPAN_BIND_ADDRESS = "infinispan.bind.address";
   public static final String INFINISPAN_BIND_PORT = "infinispan.bind.port";
   public static final String INFINISPAN_CLUSTER_NAME = "infinispan.cluster.name";
   public static final String INFINISPAN_CLUSTER_STACK = "infinispan.cluster.stack";
   public static final String INFINISPAN_NODE_NAME = "infinispan.node.name";
   public static final String INFINISPAN_PORT_OFFSET = "infinispan.socket.binding.port-offset";
   public static final String JGROUPS_BIND_ADDRESS = "jgroups.bind.address";
   public static final String JGROUPS_BIND_PORT = "jgroups.bind.port";

   /**
    * Property name indicating the path to the server installation. If unspecified, the current working directory will
    * be used
    */
   public static final String INFINISPAN_SERVER_HOME_PATH = "infinispan.server.home.path";
   /**
    * Property name indicating the path to the root of a server instance. If unspecified, defaults to the <i>server</i>
    * directory under the server home.
    */
   public static final String INFINISPAN_SERVER_ROOT_PATH = "infinispan.server.root.path";
   /**
    * Property name indicating the path to the configuration directory of a server instance. If unspecified, defaults to
    * the <i>conf</i> directory under the server root.
    */
   public static final String INFINISPAN_SERVER_CONFIG_PATH = "infinispan.server.config.path";
   /**
    * Property name indicating the path to the data directory of a server instance. If unspecified, defaults to the
    * <i>data</i> directory under the server root.
    */
   public static final String INFINISPAN_SERVER_DATA_PATH = "infinispan.server.data.path";
   /**
    * Property name indicating the path to the log directory of a server instance. If unspecified, defaults to the
    * <i>log</i> directory under the server root.
    */
   public static final String INFINISPAN_SERVER_LOG_PATH = "infinispan.server.log.path";

   // Defaults
   private static final String SERVER_DEFAULTS = "infinispan-defaults.xml";

   public static final String DEFAULT_SERVER_CONFIG = "conf";
   public static final String DEFAULT_SERVER_DATA = "data";
   public static final String DEFAULT_SERVER_LIB = "lib";
   public static final String DEFAULT_SERVER_LOG = "log";
   public static final String DEFAULT_SERVER_ROOT_DIR = "server";
   public static final String DEFAULT_SERVER_STATIC_DIR = "static";
   public static final String DEFAULT_CONFIGURATION_FILE = "infinispan.xml";
   public static final String DEFAULT_LOGGING_FILE = "log4j2.xml";
   public static final String DEFAULT_CLUSTER_NAME = "cluster";
   public static final String DEFAULT_CLUSTER_STACK = "tcp";
   public static final int DEFAULT_BIND_PORT = 11222;
   public static final int DEFAULT_JGROUPS_BIND_PORT = 7800;

   private static final int SHUTDOWN_DELAY_SECONDS = 3;

   private final ClassLoader classLoader;
   private final TimeService timeService;
   private final File serverHome;
   private final File serverRoot;
   private final File serverConf;
   private final long startTime;
   private final Properties properties;
   private ExitHandler exitHandler = new DefaultExitHandler();
   private ConfigurationBuilderHolder defaultsHolder;
   private ConfigurationBuilderHolder configurationBuilderHolder;
   private Map<String, DefaultCacheManager> cacheManagers;
   private Map<String, ProtocolServer> protocolServers;
   private volatile ComponentStatus status;
   private ServerConfiguration serverConfiguration;
   private Extensions extensions;
   private ServerStateManager serverStateManager;
   private ScheduledExecutorService scheduler;
   private TaskManager taskManager;
   private ServerInitialContextFactoryBuilder initialContextFactoryBuilder;
   private BlockingManager blockingManager;
   private BackupManager backupManager;
   private Map<String, DataSource> dataSources;

   /**
    * Initializes a server with the default server root, the default configuration file and system properties
    */
   public Server() {
      this(
            new File(DEFAULT_SERVER_ROOT_DIR),
            new File(DEFAULT_CONFIGURATION_FILE),
            SecurityActions.getSystemProperties()
      );
   }

   /**
    * Initializes a server with the supplied server root, configuration file and properties
    *
    * @param serverRoot
    * @param configurationFiles
    * @param properties
    */
   public Server(File serverRoot, List<Path> configurationFiles, Properties properties) {
      this(serverRoot, properties);
      parseConfiguration(configurationFiles);
   }

   public Server(File serverRoot, File configuration, Properties properties) {
      this(serverRoot, Collections.singletonList(configuration.toPath()), properties);
   }

   private Server(File serverRoot, Properties properties) {
      this.classLoader = Thread.currentThread().getContextClassLoader();
      this.timeService = DefaultTimeService.INSTANCE;
      this.startTime = timeService.time();
      this.serverHome = new File(properties.getProperty(INFINISPAN_SERVER_HOME_PATH, ""));
      this.serverRoot = serverRoot;
      this.properties = properties;
      this.status = ComponentStatus.INSTANTIATED;

      // Populate system properties unless they have already been set externally
      properties.putIfAbsent(INFINISPAN_SERVER_HOME_PATH, serverHome);
      properties.putIfAbsent(INFINISPAN_SERVER_ROOT_PATH, serverRoot);
      properties.putIfAbsent(INFINISPAN_SERVER_CONFIG_PATH, new File(serverRoot, DEFAULT_SERVER_CONFIG).getAbsolutePath());
      properties.putIfAbsent(INFINISPAN_SERVER_DATA_PATH, new File(serverRoot, DEFAULT_SERVER_DATA).getAbsolutePath());
      properties.putIfAbsent(INFINISPAN_SERVER_LOG_PATH, new File(serverRoot, DEFAULT_SERVER_LOG).getAbsolutePath());
      properties.putIfAbsent(INFINISPAN_BIND_PORT, DEFAULT_BIND_PORT);

      this.serverConf = new File(properties.getProperty(INFINISPAN_SERVER_CONFIG_PATH));

      // Register our simple naming context factory builder
      try {
         if (!NamingManager.hasInitialContextFactoryBuilder()) {
            initialContextFactoryBuilder = new ServerInitialContextFactoryBuilder();
            SecurityActions.setInitialContextFactoryBuilder(initialContextFactoryBuilder);
         } else {
            // This will only happen when running multiple server instances in the same JVM (i.e. embedded tests)
            log.warn("Could not register the ServerInitialContextFactoryBuilder. JNDI will not be available");
         }
      } catch (PrivilegedActionException e) {
         throw new RuntimeException(e.getCause());
      }

      // Register only the providers that matter to us
      SecurityActions.addSecurityProvider(WildFlyElytronHttpBasicProvider.getInstance());
      SecurityActions.addSecurityProvider(WildFlyElytronHttpBearerProvider.getInstance());
      SecurityActions.addSecurityProvider(WildFlyElytronHttpDigestProvider.getInstance());
      SecurityActions.addSecurityProvider(WildFlyElytronHttpClientCertProvider.getInstance());
      SecurityActions.addSecurityProvider(WildFlyElytronHttpSpnegoProvider.getInstance());
      SecurityActions.addSecurityProvider(WildFlyElytronSaslPlainProvider.getInstance());
      SecurityActions.addSecurityProvider(WildFlyElytronSaslDigestProvider.getInstance());
      SecurityActions.addSecurityProvider(WildFlyElytronSaslScramProvider.getInstance());
      SecurityActions.addSecurityProvider(WildFlyElytronSaslExternalProvider.getInstance());
      SecurityActions.addSecurityProvider(WildFlyElytronSaslLocalUserProvider.getInstance());
      SecurityActions.addSecurityProvider(WildFlyElytronSaslOAuth2Provider.getInstance());
      SecurityActions.addSecurityProvider(WildFlyElytronSaslGssapiProvider.getInstance());
      SecurityActions.addSecurityProvider(WildFlyElytronSaslGs2Provider.getInstance());
   }

   private void parseConfiguration(List<Path> configurationFiles) {
      ParserRegistry parser = new ParserRegistry(classLoader, false, properties);
      try {
         // load the defaults first
         URL defaults = this.getClass().getClassLoader().getResource(SERVER_DEFAULTS);
         defaultsHolder = parser.parse(defaults);

         // Set a default audit logger
         defaultsHolder.getGlobalConfigurationBuilder().security().authorization().auditLogger(new LoggingAuditLogger());

         // base the global configuration to the default
         configurationBuilderHolder = new ConfigurationBuilderHolder();
         GlobalConfigurationBuilder global = configurationBuilderHolder.getGlobalConfigurationBuilder();
         global
               .read(defaultsHolder.getGlobalConfigurationBuilder().build())
               .classLoader(classLoader);

         // Copy all default templates
         for (Map.Entry<String, ConfigurationBuilder> entry : defaultsHolder.getNamedConfigurationBuilders().entrySet()) {
            configurationBuilderHolder.newConfigurationBuilder(entry.getKey()).read(entry.getValue().build());
         }

         // then load the user configurations
         for (Path configurationFile : configurationFiles) {
            if (!configurationFile.isAbsolute()) {
               configurationFile = serverConf.toPath().resolve(configurationFile);
            }
            parser.parse(configurationFile.toUri().toURL(), configurationBuilderHolder);
         }
         if (log.isDebugEnabled()) {
            StringBuilderWriter sw = new StringBuilderWriter();
            try (ConfigurationWriter w = ConfigurationWriter.to(sw).build()) {
               Map<String, Configuration> configs = configurationBuilderHolder.getNamedConfigurationBuilders().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().build()));
               parser.serialize(w, global.build(), configs);
            }
            log.debugf("Actual configuration: %s", sw);
         }

         // Amend the named caches configurations with the defaults
         for (Map.Entry<String, ConfigurationBuilder> entry : configurationBuilderHolder.getNamedConfigurationBuilders().entrySet()) {
            Configuration cfg = entry.getValue().build();
            ConfigurationBuilder defaultCfg = defaultsHolder.getNamedConfigurationBuilders().get("org.infinispan." + cfg.clustering().cacheMode().name());
            ConfigurationBuilder rebased = new ConfigurationBuilder().read(defaultCfg.build());
            rebased.read(cfg);
            entry.setValue(rebased);
         }

         // Process the server configuration
         ServerConfigurationBuilder serverBuilder = global.module(ServerConfigurationBuilder.class);

         // Set up transport security
         TransportSecurityConfiguration transportSecurityConfiguration = serverBuilder.security().transport().create();
         if (transportSecurityConfiguration.securityRealm() != null) {
            String securityRealm = transportSecurityConfiguration.securityRealm();
            Supplier<SSLContext> serverSSLContextSupplier = serverBuilder.serverSSLContextSupplier(securityRealm);
            Supplier<SSLContext> clientSSLContextSupplier = serverBuilder.clientSSLContextSupplier(securityRealm);
            NamedSocketFactory namedSocketFactory = new NamedSocketFactory(() -> clientSSLContextSupplier.get().getSocketFactory(), () -> serverSSLContextSupplier.get().getServerSocketFactory());
            global.transport().addProperty(JGroupsTransport.SOCKET_FACTORY, namedSocketFactory);
            Server.log.sslTransport(securityRealm);
         }

         // Set the operation handler on all endpoints
         ServerAdminOperationsHandler adminOperationsHandler = new ServerAdminOperationsHandler(defaultsHolder);
         ServerConfigurationBuilder serverConfigurationBuilder = global.module(ServerConfigurationBuilder.class);
         for (EndpointConfigurationBuilder endpoint : serverConfigurationBuilder.endpoints().endpoints().values()) {
            for (ProtocolServerConfigurationBuilder<?, ?> connector : endpoint.connectors()) {
               connector.adminOperationsHandler(adminOperationsHandler);
            }
         }

         configurationBuilderHolder.validate();
      } catch (IOException e) {
         throw new CacheConfigurationException(e);
      }
   }

   public ExitHandler getExitHandler() {
      return exitHandler;
   }

   public void setExitHandler(ExitHandler exitHandler) {
      if (status == ComponentStatus.INSTANTIATED) {
         this.exitHandler = exitHandler;
      } else {
         throw new IllegalStateException("Cannot change exit handler on a running server");
      }
   }

   public synchronized CompletableFuture<ExitStatus> run() {
      CompletableFuture<ExitStatus> r = exitHandler.getExitFuture();
      if (status == ComponentStatus.RUNNING) {
         return r;
      }
      cacheManagers = new LinkedHashMap<>(2);
      protocolServers = new ConcurrentHashMap<>(4);
      try {
         // Load any server extensions
         extensions = new Extensions();
         extensions.load(classLoader);

         // Create the cache manager
         DefaultCacheManager cm = new DefaultCacheManager(configurationBuilderHolder, false);
         cacheManagers.put(cm.getName(), cm);

         // Retrieve the server configuration
         serverConfiguration = SecurityActions.getCacheManagerConfiguration(cm).module(ServerConfiguration.class);
         serverConfiguration.setServer(this);

         // Initialize the data sources
         dataSources = new HashMap<>();
         InitialContext initialContext = new InitialContext();
         for (DataSourceConfiguration dataSourceConfiguration : serverConfiguration.dataSources().values()) {
            DataSource dataSource = DataSourceFactory.create(dataSourceConfiguration);
            dataSources.put(dataSourceConfiguration.name(), dataSource);
            initialContext.bind(dataSourceConfiguration.jndiName(), dataSource);
         }

         // Start the cache manager
         SecurityActions.startCacheManager(cm);

         BasicComponentRegistry bcr = SecurityActions.getGlobalComponentRegistry(cm).getComponent(BasicComponentRegistry.class.getName());
         blockingManager = bcr.getComponent(BlockingManager.class).running();
         serverStateManager = new ServerStateManagerImpl(this, cm, bcr.getComponent(GlobalConfigurationManager.class).running());
         bcr.registerComponent(ServerStateManager.class, serverStateManager, false);
         ScheduledExecutorService timeoutExecutor = bcr.getComponent(KnownComponentNames.TIMEOUT_SCHEDULE_EXECUTOR, ScheduledExecutorService.class).running();

         // BlockingManager of single container used for writing the global manifest, but this will need to change
         // when multiple containers are supported by the server. Similarly, the default cache manager is used to create
         // the clustered locks.
         Path dataRoot = serverRoot.toPath().resolve(properties.getProperty(INFINISPAN_SERVER_DATA_PATH));
         backupManager = new BackupManagerImpl(blockingManager, cm, cacheManagers, dataRoot);
         backupManager.init();

         // Register the task manager
         taskManager = bcr.getComponent(TaskManager.class).running();
         taskManager.registerTaskEngine(extensions.getServerTaskEngine(cm));

         // Initialize the OpenTracing integration
         RequestTracer.start();

         for (EndpointConfiguration endpoint : serverConfiguration.endpoints().endpoints()) {
            // Start the protocol servers
            SinglePortRouteSource routeSource = new SinglePortRouteSource();
            Set<Route<? extends RouteSource, ? extends RouteDestination>> routes = ConcurrentHashMap.newKeySet();
            endpoint.connectors().parallelStream().forEach(configuration -> {
               try {
                  Class<? extends ProtocolServer> protocolServerClass = configuration.getClass().getAnnotation(ConfigurationFor.class).value().asSubclass(ProtocolServer.class);
                  ProtocolServer protocolServer = Util.getInstance(protocolServerClass);
                  if (endpoint.admin()) {
                     protocolServer.setServerManagement(this);
                  }
                  if (configuration instanceof HotRodServerConfiguration) {
                     ElytronSASLAuthenticationProvider.init((HotRodServerConfiguration) configuration, serverConfiguration, timeoutExecutor);
                  } else if (configuration instanceof RestServerConfiguration) {
                     ElytronHTTPAuthenticator.init((RestServerConfiguration)configuration, serverConfiguration);
                  }
                  protocolServers.put(protocolServer.getName() + "-" + configuration.name(), protocolServer);
                  SecurityActions.startProtocolServer(protocolServer, configuration, cm);
                  ProtocolServerConfiguration protocolConfig = protocolServer.getConfiguration();
                  if (protocolConfig.startTransport()) {
                     log.protocolStarted(protocolServer.getName(), configuration.socketBinding(), protocolConfig.host(), protocolConfig.port());
                  } else {
                     if (protocolServer instanceof HotRodServer) {
                        routes.add(new Route<>(routeSource, new HotRodServerRouteDestination(protocolServer.getName(), (HotRodServer) protocolServer)));
                        extensions.apply((HotRodServer) protocolServer);
                     } else if (protocolServer instanceof RestServer) {
                        routes.add(new Route<>(routeSource, new RestServerRouteDestination(protocolServer.getName(), (RestServer) protocolServer)));
                     }
                     log.protocolStarted(protocolServer.getName());
                  }
               } catch (Throwable t) {
                  throw t instanceof RuntimeException ? (RuntimeException) t : new RuntimeException(t);
               }
            });

            // Next we start the single-port endpoints
            SinglePortRouterConfiguration singlePortRouter = endpoint.singlePortRouter();
            SinglePortEndpointRouter endpointServer = new SinglePortEndpointRouter(singlePortRouter);
            endpointServer.start(new RoutingTable(routes), cm);
            protocolServers.put("endpoint-" + endpoint.socketBinding(), endpointServer);
            log.protocolStarted(endpointServer.getName(), singlePortRouter.socketBinding(), singlePortRouter.host(), singlePortRouter.port());
            log.endpointUrl(
                  Util.requireNonNullElse(cm.getAddress(), "local"),
                  singlePortRouter.ssl().enabled() ? "https" : "http", singlePortRouter.host(), singlePortRouter.port()
            );
         }
         serverStateManager.start();
         // Change status
         this.status = ComponentStatus.RUNNING;
         log.serverStarted(Version.getBrandName(), Version.getBrandVersion(), timeService.timeDuration(startTime, TimeUnit.MILLISECONDS));
      } catch (Exception e) {
         r.completeExceptionally(e);
      }
      r = r.whenComplete((status, t) -> localShutdown(status));
      return r;
   }

   @Override
   public void serializeConfiguration(ConfigurationWriter writer) {
      ServerConfigurationSerializer serializer = new ServerConfigurationSerializer();
      serializer.serialize(writer, this.serverConfiguration);
   }

   @Override
   public Map<String, String> getLoginConfiguration(ProtocolServer protocolServer) {
      Map<String, String> loginConfiguration = new HashMap<>();
      // Get the REST endpoint's authentication configuration
      RestServerConfiguration rest = (RestServerConfiguration) protocolServer.getConfiguration();
      if (rest.authentication().mechanisms().contains("BEARER_TOKEN")) {
         // Find the token realm
         RealmConfiguration realm = serverConfiguration.security().realms().getRealm(rest.authentication().securityRealm());
         TokenRealmConfiguration realmConfiguration = realm.realmProviders().stream().filter(r -> r instanceof TokenRealmConfiguration).map(r -> (TokenRealmConfiguration)r).findFirst().get();
         loginConfiguration.put("mode", "OIDC");
         loginConfiguration.put("url", realmConfiguration.authServerUrl());
         loginConfiguration.put("realm", realmConfiguration.name());
         loginConfiguration.put("clientId", realmConfiguration.clientId());
      } else {
         loginConfiguration.put("mode", "HTTP");
         for (String mechanism : rest.authentication().mechanisms()) {
            loginConfiguration.put(mechanism, "true");
         }
      }

      Authenticator authenticator = rest.authentication().authenticator();
      loginConfiguration.put("ready", Boolean.toString(authenticator == null || authenticator.isReadyForHttpChallenge()));

      return loginConfiguration;
   }

   @Override
   public void serverStop(List<String> servers) {
      // Stop the OpenTracing integration
      RequestTracer.stop();

      for (DefaultCacheManager cacheManager : cacheManagers.values()) {
         SecurityActions.checkPermission(cacheManager.withSubject(Security.getSubject()), AuthorizationPermission.LIFECYCLE);
         ClusterExecutor executor = SecurityActions.getClusterExecutor(cacheManager);
         if (servers != null && !servers.isEmpty()) {
            // Find the actual addresses of the servers
            List<Address> targets = cacheManager.getMembers().stream()
                  .filter(a -> servers.contains(a.toString()))
                  .collect(Collectors.toList());
            executor = executor.filterTargets(targets);
            // Tell all the target servers to exit
            sendExitStatusToServers(executor, ExitStatus.SERVER_SHUTDOWN);
         } else {
            serverStopHandler(ExitStatus.SERVER_SHUTDOWN);
         }
      }
   }

   @Override
   public void clusterStop() {
      cacheManagers.values().forEach(cm -> {
         SecurityActions.checkPermission(cm.withSubject(Security.getSubject()), AuthorizationPermission.LIFECYCLE);

         InternalCacheRegistry icr = cm.getGlobalComponentRegistry().getComponent(InternalCacheRegistry.class);
         Set<String> cacheNames = cm.getCacheNames();
         shutdownCaches(cm, cacheNames);

         Set<String> internalCaches = new HashSet<>(icr.getInternalCacheNames());
         /* The ___script_cache is included in both getCacheNames() and getInternalCacheNames so prevent repeated shutdown calls */
         internalCaches.removeAll(cacheNames);
         shutdownCaches(cm, internalCaches);

         sendExitStatusToServers(SecurityActions.getClusterExecutor(cm), ExitStatus.CLUSTER_SHUTDOWN);
      });
   }

   private void shutdownCaches(EmbeddedCacheManager cm, Collection<String> cacheNames) {
      for (String name : cacheNames) {
         try {
            SecurityActions.shutdownCache(cm, name);
         } catch (CacheException e) {
            log.exceptionOnCacheShutdown(name, e);
         }
      }
   }

   private void sendExitStatusToServers(ClusterExecutor clusterExecutor, ExitStatus exitStatus) {
      CompletableFuture<Void> job = clusterExecutor.submitConsumer(new ShutdownRunnable(exitStatus), (a, i, t) -> {
         if (t != null) {
            log.clusteredTaskError(t);
         }
      });
      job.join();
   }

   private void localShutdown(ExitStatus exitStatus) {
      this.status = ComponentStatus.STOPPING;
      if (exitStatus == ExitStatus.CLUSTER_SHUTDOWN) {
         log.clusterShutdown();
      }
      // Shutdown the protocol servers in parallel
      protocolServers.values().parallelStream().forEach(ProtocolServer::stop);
      cacheManagers.values().forEach(SecurityActions::stopCacheManager);
      // Shutdown the context and all associated resources
      if (initialContextFactoryBuilder != null) {
         initialContextFactoryBuilder.close();
      }
      // Set the status to TERMINATED
      this.status = ComponentStatus.TERMINATED;
      // Don't wait for the scheduler to finish
      if (scheduler != null) {
         scheduler.shutdown();
      }

      // Stop the OpenTracing integration
      RequestTracer.stop();

      // Shutdown Log4j's context manually as we set shutdownHook="disable"
      // Log4j's shutdownHook may run concurrently with our shutdownHook,
      // disabling logging before the server has finished stopping.
      LogManager.shutdown();
   }

   private void serverStopHandler(ExitStatus exitStatus) {
      scheduler = Executors.newSingleThreadScheduledExecutor();
      // This will complete the exit handler
      scheduler.schedule(() -> getExitHandler().exit(exitStatus), SHUTDOWN_DELAY_SECONDS, TimeUnit.SECONDS);
   }

   @SerializeWith(ShutdownRunnable.Externalizer.class)
   static final class ShutdownRunnable implements SerializableFunction<EmbeddedCacheManager, Void> {
      private final ExitStatus exitStatus;

      ShutdownRunnable(ExitStatus exitStatus) {
         this.exitStatus = exitStatus;
      }

      @Override
      public Void apply(EmbeddedCacheManager em) {
         Server server = SecurityActions.getCacheManagerConfiguration(em).module(ServerConfiguration.class).getServer();
         server.serverStopHandler(exitStatus);
         return null;
      }

      public static class Externalizer implements org.infinispan.commons.marshall.Externalizer<ShutdownRunnable> {
         @Override
         public void writeObject(ObjectOutput output, ShutdownRunnable object) throws IOException {
            output.writeObject(object.exitStatus);
         }

         @Override
         public ShutdownRunnable readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            ExitStatus exitStatus = (ExitStatus) input.readObject();
            return new ShutdownRunnable(exitStatus);
         }
      }
   }

   @Override
   public void close() {
      if (scheduler != null) {
         scheduler.shutdown();
      }
   }

   @Override
   public Set<String> cacheManagerNames() {
      return cacheManagers.keySet();
   }

   @Override
   public DefaultCacheManager getCacheManager(String name) {
      return cacheManagers.get(name);
   }

   @Override
   public ServerStateManager getServerStateManager() {
      return serverStateManager;
   }

   public ConfigurationBuilderHolder getConfigurationBuilderHolder() {
      return configurationBuilderHolder;
   }

   public File getServerRoot() {
      return serverRoot;
   }

   public Map<String, DefaultCacheManager> getCacheManagers() {
      return cacheManagers;
   }

   @Override
   public Map<String, ProtocolServer> getProtocolServers() {
      return protocolServers;
   }

   public ComponentStatus getStatus() {
      return status;
   }

   @Override
   public TaskManager getTaskManager() {
      return taskManager;
   }

   @Override
   public CompletionStage<Path> getServerReport() {
      SecurityActions.checkPermission(cacheManagers.values().iterator().next().withSubject(Security.getSubject()), AuthorizationPermission.ADMIN);
      OS os = OS.getCurrentOs();
      String reportFile = "bin/%s";
      switch (os) {
         case LINUX:
            reportFile = String.format(reportFile, "report.sh");
            break;
         case MAC_OS:
            reportFile = String.format(reportFile, "report-osx.sh");
            break;
         default:
            return CompletableFutures.completedExceptionFuture(log.serverReportUnavailable(os));
      }
      long pid = ProcessInfo.getInstance().getPid();
      Path home = serverHome.toPath();
      Path root = serverRoot.toPath();
      ProcessBuilder builder = new ProcessBuilder();
      builder.command("sh", "-c", home.resolve(reportFile).toString(), Long.toString(pid), root.toString());
      return blockingManager.supplyBlocking(() -> {
         try {
            Process process = builder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            Path path = Paths.get(reader.readLine());
            process.waitFor(1, TimeUnit.MINUTES);
            return path;
         } catch (IOException e) {
            throw new RuntimeException(e);
         } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
         }
      }, "report");
   }

   @Override
   public BackupManager getBackupManager() {
      return backupManager;
   }

   @Override
   public Map<String, DataSource> getDataSources() {
      return dataSources;
   }
}
