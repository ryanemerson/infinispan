package org.infinispan.server.test.core.persistence;

import static org.infinispan.server.test.core.Containers.DOCKER_CLIENT;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.infinispan.commons.logging.Log;
import org.infinispan.commons.logging.LogFactory;
import org.infinispan.commons.util.StringPropertyReplacer;
import org.infinispan.commons.util.Util;
import org.infinispan.server.test.core.Containers;
import org.infinispan.server.test.core.TestSystemPropertyNames;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Mount;
import com.github.dockerjava.api.model.MountType;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 10.0
 **/
public class ContainerDatabase extends Database {
   private final static Log log = LogFactory.getLog(ContainerDatabase.class);
   private final static String ENV_PREFIX = "database.container.env.";
   private final int port;
   private final String volumeName;
   private volatile GenericContainer<?> container;

   public ContainerDatabase(String type, Properties properties) {
      super(type, properties);
      this.port = Integer.parseInt(properties.getProperty("database.container.port"));
      this.volumeName = Util.threadLocalRandomUUID().toString();
      this.container = createContainer(true);
   }

   private GenericContainer<?> createContainer(boolean createVolume) {
      Map<String, String> env = properties.entrySet().stream().filter(e -> e.getKey().toString().startsWith(ENV_PREFIX))
            .collect(Collectors.toMap(e -> e.getKey().toString().substring(ENV_PREFIX.length()), e -> e.getValue().toString()));
      ImageFromDockerfile image = new ImageFromDockerfile()
            .withDockerfileFromBuilder(builder -> {
               builder
                     .from(properties.getProperty("database.container.name") + ":" + properties.getProperty("database.container.tag"))
                     .expose(port)
                     .env(env)
                     .build();
            });
      var container = new GenericContainer<>(image)
            .withExposedPorts(port)
            .withPrivilegedMode(true)
            .waitingFor(Wait.forListeningPort());

      String logMessageWaitStrategy = properties.getProperty(TestSystemPropertyNames.INFINISPAN_TEST_CONTAINER_DATABASE_LOG_MESSAGE);
      if (logMessageWaitStrategy != null) {
         container.waitingFor(new LogMessageWaitStrategy()
               .withRegEx(logMessageWaitStrategy)
               .withStartupTimeout(Duration.of(10, ChronoUnit.MINUTES)));
      }

      // TODO add property to disable volume creation by default
      var volumeRequired = true;
      if (volumeRequired) {
         if (createVolume) DOCKER_CLIENT.createVolumeCmd().withName(volumeName).exec();
         // TODO set target directory dynamically
         container.withCreateContainerCmdModifier(cmd ->
               cmd.getHostConfig().withMounts(
                     List.of(new Mount().withSource(volumeName).withTarget("/var/lib/mysql").withType(MountType.VOLUME))
               )
         );
      }
      return container;
   }

   @Override
   public void start() {
      log.infof("Starting database %s", getType());
      container.start();
   }

   @Override
   public void stop() {
      log.infof("Stopping database %s", getType());
      container.stop();
      log.infof("Stopped database %s", getType());
   }

   public void pause() {
      dockerClient().pauseContainerCmd(container.getContainerId()).exec();
   }

   public void resume() {
      dockerClient().unpauseContainerCmd(container.getContainerId()).exec();
   }

   public void restart() {
      if (container.isRunning()) stop();
      container = createContainer(false);
      container.start();
   }

   public int getPort() {
      return container.getMappedPort(port);
   }

   @Override
   public String jdbcUrl() {
      String address = Containers.ipAddress(container);
      Properties props = new Properties();
      props.setProperty("container.address", address);
      return StringPropertyReplacer.replaceProperties(super.jdbcUrl(), props);
   }

   @Override
   public String username() {
      Properties props = new Properties();
      return StringPropertyReplacer.replaceProperties(super.username(), props);
   }

   @Override
   public String password() {
      Properties props = new Properties();
      return StringPropertyReplacer.replaceProperties(super.password(), props);
   }

   private DockerClient dockerClient() {
      return DockerClientFactory.instance().client();
   }
}
