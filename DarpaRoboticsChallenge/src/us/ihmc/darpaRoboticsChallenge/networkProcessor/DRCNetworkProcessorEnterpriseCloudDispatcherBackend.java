package us.ihmc.darpaRoboticsChallenge.networkProcessor;

import com.martiansoftware.jsap.*;
import us.ihmc.darpaRoboticsChallenge.DRCConfigParameters;
import us.ihmc.utilities.fixedPointRepresentation.UnsignedByteTools;
import us.ihmc.utilities.net.tcpServer.DisconnectedException;
import us.ihmc.utilities.net.tcpServer.ReconnectingTCPServer;
import us.ihmc.utilities.processManagement.JavaProcessSpawner;

import java.io.IOException;

public class DRCNetworkProcessorEnterpriseCloudDispatcherBackend implements Runnable
{
   private JavaProcessSpawner networkProcessorSpawner = new JavaProcessSpawner(true);
   private ReconnectingTCPServer server;

   private final byte[] buffer;

   private static String scsMachineIPAddress = DRCConfigParameters.SCS_MACHINE_IP_ADDRESS;
   private static String rosMasterURI = DRCConfigParameters.ROS_MASTER_URI;

   private static String[] javaArgs = new String[] {"-Xms2048m", "-Xmx2048m"};

   public DRCNetworkProcessorEnterpriseCloudDispatcherBackend()
   {
      try
      {
         server = new ReconnectingTCPServer(DRCConfigParameters.NETWORK_PROCESSOR_CLOUD_DISPATCHER_BACKEND_TCP_PORT);
      }
      catch (IOException e)
      {
         e.printStackTrace();
      }

      buffer = server.getBuffer();
   }

   public void run()
   {
      while (true)
      {
         try
         {
            server.read(1);

            switch (buffer[0])
            {
               case 0x00 :
                  spawnNetworkProcessor();

                  break;

               case 0x01 :
               case 0x10 :
                  killNetworkProcessor();

                  break;

               case 0x11 :
                  restartNetworkProcessor();

                  break;

               default :
                  System.err.println("Invalid request: " + Integer.toHexString(UnsignedByteTools.toInt(buffer[0])));

                  break;
            }
         }
         catch (DisconnectedException e)
         {
            server.reset();
         }

         if (networkProcessorSpawner.hasRunningProcesses())
         {
            try
            {
               server.write(new byte[] {UnsignedByteTools.fromInt(0x00)});
            }
            catch (DisconnectedException e)
            {
               server.reset();
            }
         }
         else
         {
            try
            {
               server.write(new byte[] {UnsignedByteTools.fromInt(0x11)});
            }
            catch (DisconnectedException e)
            {
               server.reset();
            }
         }

         server.reset();
      }
   }

   private void spawnNetworkProcessor()
   {
      if (!networkProcessorSpawner.hasRunningProcesses())
         networkProcessorSpawner.spawn(DRCNetworkProcessor.class, javaArgs, new String[] {"--ros-uri", rosMasterURI, "--scs-ip", scsMachineIPAddress});
      else
         System.err.println("Network processor is already running. Try restarting.");
   }

   private void killNetworkProcessor()
   {
      networkProcessorSpawner.killAll();
   }

   private void restartNetworkProcessor()
   {
      killNetworkProcessor();

      spawnNetworkProcessor();
   }

   public static void main(String[] args) throws JSAPException
   {
      JSAP jsap = new JSAP();

      FlaggedOption scsIPFlag =
         new FlaggedOption("scs-ip").setLongFlag("scs-ip").setShortFlag(JSAP.NO_SHORTFLAG).setRequired(false).setStringParser(JSAP.STRING_PARSER);
      FlaggedOption rosURIFlag =
         new FlaggedOption("ros-uri").setLongFlag("ros-uri").setShortFlag(JSAP.NO_SHORTFLAG).setRequired(false).setStringParser(JSAP.STRING_PARSER);

      Switch largeHeapForProcessor = new Switch("large-heap").setLongFlag("large-heap").setShortFlag('h');

      jsap.registerParameter(scsIPFlag);
      jsap.registerParameter(rosURIFlag);
      jsap.registerParameter(largeHeapForProcessor);

      JSAPResult config = jsap.parse(args);

      if (config.success())
      {
         if (config.getString(scsIPFlag.getID()) != null)
         {
            scsMachineIPAddress = config.getString(scsIPFlag.getID());
         }

         if (config.getString(rosURIFlag.getID()) != null)
         {
            rosMasterURI = config.getString(rosURIFlag.getID());
         }

         if (config.getBoolean(largeHeapForProcessor.getID()))
         {
            javaArgs = new String[] {"-Xms10240m", "-Xmx10240m"};
         }
      }

      Thread t = new Thread(new DRCNetworkProcessorEnterpriseCloudDispatcherBackend());

      t.start();
   }
}
