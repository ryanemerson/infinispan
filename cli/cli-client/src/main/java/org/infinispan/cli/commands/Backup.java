package org.infinispan.cli.commands;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.option.Option;
import org.infinispan.cli.impl.ContextAwareCommandInvocation;
import org.kohsuke.MetaInfServices;

/**
 * @author Ryan Emerson
 * @since 11.0
 **/
@MetaInfServices(Command.class)
@CommandDefinition(name = Backup.CMD, description = "Performs a cluster backup")
public class Backup extends CliCommand {

   public static final String CMD = "backup";

   @Option(shortName = 'h', hasValue = false, overrideRequired = true)
   protected boolean help;

   @Override
   public boolean isHelp() {
      return help;
   }

   @Override
   public CommandResult exec(ContextAwareCommandInvocation invocation) {
      if (help) {
         invocation.println(invocation.getHelpInfo());
         return CommandResult.SUCCESS;
      }
      return invocation.execute(new CommandInputLine(CMD));
   }

   @Override
   public int nesting() {
      return -1;
   }
}
