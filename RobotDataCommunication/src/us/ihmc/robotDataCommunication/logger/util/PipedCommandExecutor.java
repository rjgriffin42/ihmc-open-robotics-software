package us.ihmc.robotDataCommunication.logger.util;

import java.io.IOException;
import java.io.PrintStream;

import us.ihmc.utilities.ThreadTools;
import us.ihmc.utilities.processManagement.ProcessStreamGobbler;

public class PipedCommandExecutor
{
   private final ProcessBuilder processBuilder;

   private final Object synchronizer = new Object();
   private Process process;
   private PrintStream processStdin;

   public PipedCommandExecutor(ExternalProgram... programs)
   {
      StringBuilder cmd = new StringBuilder();

      cmd.append(programs[0].getCommandLine());

      for (int i = 1; i < programs.length; i++)
      {
         cmd.append(" | ");
         cmd.append(programs[i].getCommandLine());
      }

      System.out.println("Running command " + cmd.toString());
      processBuilder = new ProcessBuilder("sh", "-c", cmd.toString());
   }

   public void execute() throws IOException
   {
      execute(System.out, System.err);
   }
   
   public void execute(PrintStream outputStream, PrintStream errorStream) throws IOException
   {

      synchronized (synchronizer)
      {
         if (process != null)
         {
            throw new RuntimeException("Process already started");
         }
         process = processBuilder.start();
         processStdin = new PrintStream(process.getOutputStream(), true);
      }

      new ProcessStreamGobbler("PipedCommandExecutor", process.getInputStream(), outputStream).start();
      new ProcessStreamGobbler("PipedCommandExecutor", process.getErrorStream(), errorStream).start();

   }

   public void writeln(String value)
   {
      synchronized (synchronizer)
      {
         if(processStdin != null)
         {
            processStdin.println(value);
         }
      }
   }

   public void write(String value)
   {
      synchronized (synchronizer)
      {
         if(processStdin != null)
         {
            processStdin.print(value);
         }
      }
   }
   
   public int waitFor()
   {
	   synchronized(synchronizer)
	   {
		   if(process == null)
		   {
			   return 0;
		   }
	   }
	   try {
		   return process.waitFor();
	   } catch (InterruptedException e) {
		return 0;
	   }
   }

   public void stop()
   {
      synchronized (synchronizer)
      {
         if(process != null)
         {
            UnixProcessKiller.killUnixProcess(process);
         }
      }
   }
   
   public static void main(String[] args) throws IOException
   {
		PipedCommandExecutor executor = new PipedCommandExecutor(
				new ExternalProgram() 
				{

					@Override
					public String getCommandLine() {
						return "cat";
					}
				});

		executor.execute();
		long i = 0;
		while (true) 
		{
			i++;
			executor.writeln(String.valueOf(i));
			ThreadTools.sleep(500);
		}
	   
   }
}
