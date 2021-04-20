package org.infinispan.server.core.backup.resources;

import java.lang.invoke.MethodHandles;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import org.infinispan.commons.api.CacheContainerAdmin;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ParserRegistry;
import org.infinispan.globalstate.GlobalConfigurationManager;
import org.infinispan.globalstate.impl.CacheState;
import org.infinispan.globalstate.impl.GlobalConfigurationManagerImpl;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.tasks.ServerTask;
import org.infinispan.tasks.TaskContext;
import org.infinispan.tasks.TaskExecutionMode;
import org.infinispan.util.concurrent.CompletionStages;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * Admin operation to create a cache on non-zero capacity pods Parameters:
 * <ul>
 *    <li><strong>name</strong> the name of the cache to create</li>
 *    <li><strong>configuration</strong> the XML configuration to use</li>
 *    <li><strong>flags</strong> any flags, e.g. PERMANENT</li>
 * </ul>
 *
 * @author Ryan Emerson
 * @since 12.1
 */
public class BackupCreateCacheTask implements ServerTask<Void> {

   private TaskContext taskContext;

   @Override
   public TaskExecutionMode getExecutionMode() {
      return TaskExecutionMode.ALL_NODES;
   }

   @Override
   public void setTaskContext(TaskContext taskContext) {
      this.taskContext = taskContext;
   }

   @Override
   public Void call() throws Exception {
      EmbeddedCacheManager cacheManager = taskContext.getCacheManager();
      // TODO handle optional
      Map<String, ?> parameters = taskContext.getParameters().get();

      Log log = LogFactory.getLog(MethodHandles.lookup().lookupClass());
      log.fatalf("In task");
      if (cacheManager.getCacheManagerConfiguration().isZeroCapacityNode()) {
         return null;
      }
      String name = (String) parameters.get("name");
      String configuration = (String) parameters.get("configuration");
      if (configuration == null)
         throw new IllegalArgumentException("Configuration cannot be null");

      log.fatalf("Task create cache=%s, config=%s", name, configuration);

      ConfigurationBuilderHolder builderHolder = new ParserRegistry().parse(configuration);
      Configuration config = builderHolder.getNamedConfigurationBuilders().get(name).build(cacheManager.getCacheManagerConfiguration());
      cacheManager.createCache(name, config);

      CacheState state = new CacheState(null, configuration, EnumSet.noneOf(CacheContainerAdmin.AdminFlag.class));
      GlobalConfigurationManagerImpl gcm = (GlobalConfigurationManagerImpl) cacheManager.getGlobalComponentRegistry().getComponent(GlobalConfigurationManager.class);
      CompletionStages.join(gcm.createCacheLocally(name, state));

//      ConfigurationBuilderHolder builderHolder = new ParserRegistry().parse(configuration);
//      Configuration config = builderHolder.getNamedConfigurationBuilders().get(name).build(cacheManager.getCacheManagerConfiguration());
//      cacheManager.administration().getOrCreateCache(name, config);
//      CacheState state = new CacheState(template, configuration, EnumSet.noneOf(CacheContainerAdmin.AdminFlag.class));
//      cacheManager.getGlobalComponentRegistry().getComponent(GlobalConfigurationManager.class).getStateCache().getAdvancedCache()
//            .putIfAbsent(new ScopedState(CACHE_SCOPE, name), state);
      return null;
   }

   @Override
   public String getName() {
      return "@@backup@CreateCache";
   }
}
