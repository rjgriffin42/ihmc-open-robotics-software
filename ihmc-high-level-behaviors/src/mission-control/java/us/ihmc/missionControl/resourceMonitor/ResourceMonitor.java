package us.ihmc.missionControl.resourceMonitor;

import org.apache.logging.log4j.util.Strings;
import us.ihmc.commons.thread.ThreadTools;
import us.ihmc.tools.thread.PausablePeriodicThread;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs a command and reads the process output from stdout.
 * Provides a generic parse(String[] lines) method for specific implementations.
 */
public abstract class ResourceMonitor
{
   private static final double DEFAULT_READ_PERIOD_SECONDS = 1.0;

   private final double parsePeriodSeconds;
   private final String[] command;
   private final PausablePeriodicThread thread;
   private volatile boolean running;

   public ResourceMonitor(String... command)
   {
      this(DEFAULT_READ_PERIOD_SECONDS, command);
   }

   public ResourceMonitor(double parsePeriodSeconds, String... command)
   {
      this.parsePeriodSeconds = parsePeriodSeconds;
      this.command = command;
      thread = new PausablePeriodicThread(getClass().getName() + "-Reader-Parser", parsePeriodSeconds, true, () ->
      {
         try
         {
            String[] lines = read();
            parse(lines);
         }
         catch (Exception e)
         {
            e.printStackTrace();
         }
      });
   }

   public void start()
   {
      thread.start();
      running = true;
   }

   public void stop()
   {
      thread.stop();
      running = false;
   }

   public String[] read() throws IOException, InterruptedException
   {
      ProcessBuilder processBuilder = new ProcessBuilder(command);
      processBuilder.redirectErrorStream(true);
      Process process = processBuilder.start();

      // Destroy the process after the parse period has expired
      new Thread(() ->
      {
         ThreadTools.sleep((long) (parsePeriodSeconds * 1000));
         if (process.isAlive())
         {
            process.destroy();
         }
      }).start();

      List<String> lines = new ArrayList<>();

      try (InputStream inputStream = process.getInputStream(); BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream)))
      {
         String line;
         while ((line = reader.readLine()) != null && process.isAlive() && running)
         {
            lines.add(line);
         }
      }

      String[] lineArray = new String[lines.size()];
      lines.toArray(lineArray);
      return lineArray;
   }

   public abstract void parse(String[] lines);
}
