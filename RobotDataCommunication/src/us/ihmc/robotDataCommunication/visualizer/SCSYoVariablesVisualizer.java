package us.ihmc.robotDataCommunication.visualizer;

import us.ihmc.robotDataCommunication.YoVariableClient;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;

public class SCSYoVariablesVisualizer
{
   public static final String defaultHost = "localhost";
   public static final int defaultPort = 5555;
   
   public SCSYoVariablesVisualizer(String host, int port, int bufferSize)
   {
      System.out.println("Connecting to host " + host);
      
      SCSVisualizer scsYoVariablesUpdatedListener = new SCSVisualizer(bufferSize);

      boolean showOverheadView = false;
      YoVariableClient client = new YoVariableClient(host, port, scsYoVariablesUpdatedListener, "remote", showOverheadView);
      client.start();
   }

   public static void main(String[] args) throws JSAPException
   {
      int bufferSize=32768;
      JSAP jsap = new JSAP();
      
      FlaggedOption hostOption = new FlaggedOption("host").setStringParser(JSAP.STRING_PARSER).setRequired(false).setLongFlag("host").setShortFlag('L').setDefault(
            defaultHost);
      FlaggedOption portOption = new FlaggedOption("port").setStringParser(JSAP.INTEGER_PARSER).setRequired(false).setLongFlag("port").setShortFlag('p')
            .setDefault(String.valueOf(defaultPort));
      
      jsap.registerParameter(hostOption);
      jsap.registerParameter(portOption);
      
      JSAPResult config = jsap.parse(args);
      
      if (config.success())
      {
         String host = config.getString("host");
         int port = config.getInt("port");
         
         new SCSYoVariablesVisualizer(host, port, bufferSize);         
      }
      else
      {
         System.err.println();
         System.err.println("Usage: java " + SCSVisualizer.class.getName());
         System.err.println("                " + jsap.getUsage());
         System.err.println();
         System.exit(1);
      }
      
      
      
   }
}
