package org.infinispan.cli.io;

import java.io.IOException;
import java.util.List;

import org.aesh.command.shell.Shell;
import org.aesh.readline.Prompt;
import org.fusesource.jansi.Ansi;
import org.infinispan.cli.commands.ProcessedCommand;

public class ConsoleIOAdapter implements IOAdapter {
   private final Shell shell;

   public ConsoleIOAdapter(final Shell shell) {
      this.shell = shell;
   }

   @Override
   public boolean isInteractive() {
      return true;
   }

   @Override
   public String readln(String prompt) {
      return read(prompt, null);
   }

   @Override
   public String secureReadln(String prompt) {
      return read(prompt, (char)0);
   }

   @Override
   public void println(String s) {
      shell.writeln(s);
   }

   @Override
   public void error(String s) {
      Ansi ansi = new Ansi();
      ansi.fg(Ansi.Color.RED);
      println(ansi.render(s).reset().toString());
   }

   @Override
   public void result(List<ProcessedCommand> commands, String result, boolean isError) throws IOException {
      if (isError)
         error(result);
      else
         println(result);
   }

   @Override
   public int getWidth() {
      return shell.getTerminalSize().getWidth();
   }

   private String read(String prompt, Character mask) {
      Prompt origPrompt = null;
      if (!shell.getPrompt().getPromptAsString().equals(prompt)) {
         origPrompt = shell.getPrompt();
         shell.setPrompt(new Prompt(prompt, mask));
      }
      try {
         return shell.getInputLine();
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
      } finally {
         if (origPrompt != null) {
            shell.setPrompt(origPrompt);
         }
      }
      return null;
   }


}
