package org.infinispan.cli.commands;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.completer.FileOptionCompleter;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.aesh.io.Resource;
import org.infinispan.cli.impl.ContextAwareCommandInvocation;
import org.kohsuke.MetaInfServices;

/**
 * @author Ryan Emerson
 * @since 11.0
 **/
@MetaInfServices(Command.class)
@CommandDefinition(name = Restore.CMD, description = "Restores cluster content from a local backup file")
public class Restore extends CliCommand {

   public static final String CMD = "restore";

   @Argument(completer = FileOptionCompleter.class, description = "The path to a local backup archive")
   Resource archive;

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
      CommandInputLine cmd = new CommandInputLine(CMD)
            .arg(FILE, archive.getAbsolutePath());
      return invocation.execute(cmd);
   }

   @Override
   public int nesting() {
      return -1;
   }
}
