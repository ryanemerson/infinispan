package org.infinispan.server.core.security.actions;

import java.security.PrivilegedAction;
import java.util.concurrent.CompletionStage;

import org.infinispan.tasks.TaskContext;
import org.infinispan.tasks.TaskManager;

/**
 * ExecuteAdminTaskAction
 *
 * @author Ryan Emerson
 * @since 12.1
 */
public class ExecuteAdminTaskAction implements PrivilegedAction<CompletionStage<?>> {

   private final String taskName;
   private final TaskManager taskManager;
   private final TaskContext taskContext;

   public ExecuteAdminTaskAction(String taskName, TaskManager taskManager, TaskContext taskContext) {
      this.taskName = taskName;
      this.taskManager = taskManager;
      this.taskContext = taskContext;
   }

   @Override
   public CompletionStage<?> run() {
      return taskManager.runTask(taskName, taskContext);
   }
}
