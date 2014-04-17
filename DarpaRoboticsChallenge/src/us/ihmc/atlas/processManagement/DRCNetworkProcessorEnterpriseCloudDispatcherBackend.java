package us.ihmc.atlas.processManagement;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

import us.ihmc.atlas.AtlasNetworkProcessor;
import us.ihmc.atlas.AtlasRobotModelFactory;
import us.ihmc.darpaRoboticsChallenge.DRCConfigParameters;
import us.ihmc.darpaRoboticsChallenge.DRCLocalConfigParameters;
import us.ihmc.utilities.ThreadTools;
import us.ihmc.utilities.fixedPointRepresentation.UnsignedByteTools;
import us.ihmc.utilities.net.tcpServer.DisconnectedException;
import us.ihmc.utilities.net.tcpServer.ReconnectingTCPServer;
import us.ihmc.utilities.processManagement.JavaProcessSpawner;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Switch;

public class DRCNetworkProcessorEnterpriseCloudDispatcherBackend implements Runnable
{
   private JavaProcessSpawner networkProcessorSpawner = new JavaProcessSpawner(true);
   private ReconnectingTCPServer commandServer;

   private final byte[] buffer;

   private static String scsMachineIPAddress = DRCLocalConfigParameters.ROBOT_CONTROLLER_IP_ADDRESS;
   private static String rosMasterURI = DRCConfigParameters.ROS_MASTER_URI;

   private static String[] javaArgs = new String[] {"-Xms2048m", "-Xmx2048m"};

   private static String robotModel;

   public DRCNetworkProcessorEnterpriseCloudDispatcherBackend()
   {
      try
      {
         commandServer = new ReconnectingTCPServer(DRCConfigParameters.NETWORK_PROCESSOR_CLOUD_DISPATCHER_BACKEND_TCP_PORT);
      }
      catch (IOException e)
      {
         e.printStackTrace();
      }

      buffer = commandServer.getBuffer();
   }

   public void run()
   {
      while (true)
      {
         try
         {
            commandServer.read(1);

            switch (UnsignedByteTools.toInt(buffer[0]))
            {
               case 0x00 :
                  commandServer.read(1);
                  robotModel = AtlasRobotModelFactory.getAvailableRobotModels()[UnsignedByteTools.toInt(buffer[1])];
                  spawnNetworkProcessor();

                  break;

               case 0x10 :
                  killNetworkProcessor();

                  break;

               case 0x11 :
                  restartNetworkProcessor();

                  break;

               case 0x22 :
                  startStreamingOutput();

                  break;

               default :
                  System.err.println("Invalid request: " + Integer.toHexString(UnsignedByteTools.toInt(buffer[0])));

                  break;
            }
         }
         catch (DisconnectedException e)
         {
            commandServer.reset();
         }

         if (networkProcessorSpawner.hasRunningProcesses())
         {
         }
         else
         {
         }

         commandServer.reset();
      }
   }

   private void startStreamingOutput()
   {
      new Thread(new Runnable()
      {
         @Override
         public void run()
         {
            ServerSocket server = null;
            try
            {
               server = new ServerSocket(DRCConfigParameters.CONTROLLER_CLOUD_DISPATCHER_BACKEND_TCP_PORT + 5);
               commandServer.write(new byte[] {UnsignedByteTools.fromInt(0x22)});
               Socket socket = server.accept();
               socket.setTcpNoDelay(true);
               OutputStream outputStream = socket.getOutputStream();
               System.setOut(new PrintStream(outputStream));
               System.setErr(new PrintStream(outputStream));
            }
            catch (IOException e)
            {
               e.printStackTrace();
            }
            catch (DisconnectedException e)
            {
               commandServer.reset();

               try
               {
                  server.close();
               }
               catch (IOException e1)
               {
                  e1.printStackTrace();
               }
            }
         }
      }).start();
   }

   private void spawnNetworkProcessor()
   {
      if (!networkProcessorSpawner.hasRunningProcesses())
      {
         networkProcessorSpawner.spawn(AtlasNetworkProcessor.class, javaArgs, new String[] {"--ros-uri", rosMasterURI, "--scs-ip", scsMachineIPAddress, "-m", robotModel});

         try
         {
            commandServer.write(new byte[] {UnsignedByteTools.fromInt(0x00)});
         }
         catch (DisconnectedException e)
         {
            commandServer.reset();
         }
      }
      else
         System.err.println("Network processor is already running. Try restarting.");
   }

   private void killNetworkProcessor()
   {
      networkProcessorSpawner.killAll();

      try
      {
         commandServer.write(new byte[] {UnsignedByteTools.fromInt(0x11)});
      }
      catch (DisconnectedException e)
      {
         commandServer.reset();
      }
   }

   private void restartNetworkProcessor()
   {
      killNetworkProcessor();
      ThreadTools.sleep(5000);
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
            javaArgs = new String[] {"-Xms4096m", "-Xmx4096m"};
         }
      }

      Thread t = new Thread(new DRCNetworkProcessorEnterpriseCloudDispatcherBackend());

      t.start();
   }
}
