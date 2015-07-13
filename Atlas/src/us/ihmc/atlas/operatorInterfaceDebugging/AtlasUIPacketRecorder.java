package us.ihmc.atlas.operatorInterfaceDebugging;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

import us.ihmc.communication.configuration.NetworkParameterKeys;
import us.ihmc.communication.configuration.NetworkParameters;
import us.ihmc.communication.kryo.IHMCCommunicationKryoNetClassList;
import us.ihmc.communication.net.KryoStreamSerializer;
import us.ihmc.communication.net.NetStateListener;
import us.ihmc.communication.packetCommunicator.PacketCommunicator;
import us.ihmc.communication.packetCommunicator.interfaces.GlobalPacketConsumer;
import us.ihmc.communication.packets.Packet;
import us.ihmc.communication.util.NetworkPorts;
import us.ihmc.utilities.io.files.FileTools;
import us.ihmc.utilities.io.printing.PrintTools;
import us.ihmc.utilities.math.UnitConversions;
import us.ihmc.utilities.time.DateTools;
import us.ihmc.utilities.time.Timer;

public class AtlasUIPacketRecorder
{
   private static final Path PACKET_RECORDINGS_PATH = Paths.get("./packetRecordings");
   private static final String PACKET_RECORDING_FILENAME = "PacketRecording_" + DateTools.getDateString() + "_real3";
   private Object streamConch = new Object();

   public AtlasUIPacketRecorder() throws IOException
   {
      Scanner scanner = new Scanner(System.in);
      PrintTools.info("Press Enter to record...");
      scanner.nextLine();
      
      final DataOutputStream fileDataOutputStream = FileTools.getFileDataOutputStream(getPacketRecordingFilePath());
      final PrintWriter timeWriter = FileTools.newPrintWriter(getPacketTimingPath());
      
      IHMCCommunicationKryoNetClassList netClassList = new IHMCCommunicationKryoNetClassList();
      
      final KryoStreamSerializer kryoStreamSerializer = new KryoStreamSerializer(UnitConversions.megabytesToBytes(10));
      kryoStreamSerializer.registerClasses(netClassList);
      
      PacketCommunicator packetClient = PacketCommunicator.createTCPPacketCommunicatorClient(NetworkParameters.getHost(NetworkParameterKeys.networkManagerForUI), NetworkPorts.NETWORK_PROCESSOR_TO_UI_TCP_PORT, netClassList);
      packetClient.attachStateListener(new NetStateListener()
      {
         @Override
         public void disconnected()
         {
            PrintTools.info("Disconnected");
            try
            {
               synchronized (streamConch)
               {
                  fileDataOutputStream.flush();
                  fileDataOutputStream.close();
                  
                  timeWriter.flush();
                  timeWriter.close();
               }
            }
            catch (IOException e)
            {
               e.printStackTrace();
            }
         }
         
         @Override
         public void connected()
         {
            PrintTools.info("Connected");
         }
      });
      packetClient.connect();
      packetClient.attachGlobalListener(new GlobalPacketConsumer()
      {
         Timer timer = new Timer();
         boolean firstPacketReceived = false;
         
         @Override
         public void receivedPacket(Packet<?> packet)
         {
            try
            {
               synchronized (streamConch)
               {
                  if (!firstPacketReceived)
                  {
                     firstPacketReceived = true;
                     timer.start();
                  }
                  else
                  {
                     timeWriter.println(timer.lap());
                  }

                  PrintTools.info("Receiving packet: " + packet);
                  kryoStreamSerializer.write(fileDataOutputStream, packet);
               }
            }
            catch (IOException e)
            {
               e.printStackTrace();
            }
         }
      });
      
      scanner.nextLine();
      scanner.close();
      
      packetClient.close();
   }
   
   public static Path getPacketRecordingFilePath()
   {
      FileTools.ensureDirectoryExists(PACKET_RECORDINGS_PATH);
      return PACKET_RECORDINGS_PATH.resolve(getPrefixFileName() + ".ibag");
   }
   
   public static Path getPacketTimingPath()
   {
      return getPacketRecordingFilePath().getParent().resolve(getPrefixFileName() + ".tbag");
   }
   
   public static String getPrefixFileName()
   {
      return "PacketRecording_" + DateTools.getDateString() + "_real5";
   }
   
   public static void main(String[] args) throws IOException
   {
      new AtlasUIPacketRecorder();
   }
}
